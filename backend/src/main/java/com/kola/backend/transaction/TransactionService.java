package com.kola.backend.transaction;

import com.kola.backend.beneficiary.Beneficiary;
import com.kola.backend.beneficiary.BeneficiaryRepository;
import com.kola.backend.exception.InsufficientFundsException;
import com.kola.backend.exception.KycLimitExceededException;
import com.kola.backend.user.User;
import com.kola.backend.wallet.Wallet;
import com.kola.backend.wallet.WalletService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║                TransactionService.java                      ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * RÈGLE D'OR (rappel de Transaction.java) : on ne modifie JAMAIS une
 * transaction déjà créée. Toute correction passe par une nouvelle
 * transaction (REFUNDED, etc.). Ce service ne fait donc que des
 * INSERT, jamais d'UPDATE sur une transaction existante (sauf le
 * passage PENDING → SUCCESS/FAILED qui a lieu de façon synchrone ici
 * faute d'intégration réelle avec un opérateur Mobile Money).
 *
 * VERROUILLAGE : chaque opération qui modifie un solde de wallet passe
 * par WalletService.findOwnedWalletForUpdateOrThrow, qui prend un verrou
 * PESSIMISTIC_WRITE en base. Ça garantit qu'on ne peut pas avoir deux
 * débits concurrents qui passeraient tous les deux la vérification de
 * solde avant que l'un des deux ait écrit son résultat (race condition
 * classique sur les soldes).
 */
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletService walletService;
    private final BeneficiaryRepository beneficiaryRepository;
    private final TransactionReferenceGenerator referenceGenerator;

    // ═══════════════════════════════════════════════════════════════
    //  DÉPÔT (DEPOSIT) — recharge depuis Mobile Money
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public TransactionResponse deposit(User currentUser, DepositRequest request) {
        Wallet wallet = walletService.findOwnedWalletForUpdateOrThrow(currentUser, request.walletId());

        wallet.setBalance(wallet.getBalance().add(request.amount()));

        Transaction tx = Transaction.builder()
                .reference(referenceGenerator.generate())
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.SUCCESS)
                .amount(request.amount())
                .fee(BigDecimal.ZERO)
                .currency(wallet.getCurrency())
                .wallet(wallet)
                .sender(currentUser)
                .externalReference(request.externalReference())
                .description("Rechargement Mobile Money")
                .build();

        transactionRepository.save(tx);
        return TransactionResponse.fromEntity(tx);
    }

    // ═══════════════════════════════════════════════════════════════
    //  RETRAIT (WITHDRAWAL) — vers Mobile Money
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public TransactionResponse withdraw(User currentUser, WithdrawalRequest request) {
        Wallet wallet = walletService.findOwnedWalletForUpdateOrThrow(currentUser, request.walletId());

        BigDecimal fee = TransactionPolicy.computeWithdrawalFee(request.amount());
        BigDecimal totalDebit = request.amount().add(fee);

        checkSufficientFunds(wallet, totalDebit);
        checkDailyLimit(currentUser, request.amount());

        wallet.setBalance(wallet.getBalance().subtract(totalDebit));

        Transaction tx = Transaction.builder()
                .reference(referenceGenerator.generate())
                .type(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.SUCCESS)
                .amount(request.amount())
                .fee(fee)
                .currency(wallet.getCurrency())
                .wallet(wallet)
                .sender(currentUser)
                .description("Retrait vers Mobile Money")
                .build();

        transactionRepository.save(tx);
        return TransactionResponse.fromEntity(tx);
    }

    // ═══════════════════════════════════════════════════════════════
    //  TRANSFERT (TRANSFER_OUT / TRANSFER_IN) — vers un bénéficiaire
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public TransactionResponse transfer(User currentUser, TransferRequest request) {
        Wallet sourceWallet = walletService.findOwnedWalletForUpdateOrThrow(currentUser, request.sourceWalletId());

        Beneficiary beneficiary = beneficiaryRepository.findById(request.beneficiaryId())
                .orElseThrow(() -> new EntityNotFoundException("Bénéficiaire introuvable"));

        if (!beneficiary.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Ce bénéficiaire ne vous appartient pas");
        }

        BigDecimal fee = TransactionPolicy.computeTransferFee(request.amount());
        BigDecimal totalDebit = request.amount().add(fee);

        checkSufficientFunds(sourceWallet, totalDebit);
        checkDailyLimit(currentUser, request.amount());

        sourceWallet.setBalance(sourceWallet.getBalance().subtract(totalDebit));

        // TRANSFER_OUT : trace côté émetteur
        Transaction outTx = Transaction.builder()
                .reference(referenceGenerator.generate())
                .type(TransactionType.TRANSFER_OUT)
                .status(TransactionStatus.SUCCESS)
                .amount(request.amount())
                .fee(fee)
                .currency(sourceWallet.getCurrency())
                .wallet(sourceWallet)
                .sender(currentUser)
                .receiverPhoneNumber(beneficiary.getPhoneNumber())
                .receiverCountryCode(beneficiary.getCountryCode())
                .description(request.description() != null ? request.description() : "Transfert vers " + beneficiary.getAlias())
                .build();
        transactionRepository.save(outTx);

        // FEE : trace séparée du prélèvement de frais (cf. règle d'or dans Transaction.java)
        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            Transaction feeTx = Transaction.builder()
                    .reference(referenceGenerator.generate())
                    .type(TransactionType.FEE)
                    .status(TransactionStatus.SUCCESS)
                    .amount(fee)
                    .fee(BigDecimal.ZERO)
                    .currency(sourceWallet.getCurrency())
                    .wallet(sourceWallet)
                    .sender(currentUser)
                    .description("Frais sur transfert " + outTx.getReference())
                    .build();
            transactionRepository.save(feeTx);
        }

        return TransactionResponse.fromEntity(outTx);
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONSULTATION
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<TransactionResponse> getWalletHistory(User currentUser, Long walletId) {
        // findOwnedWalletOrThrow vérifie déjà l'appartenance → pas d'IDOR possible
        Wallet wallet = walletService.findOwnedWalletOrThrow(currentUser, walletId);
        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId())
                .stream()
                .map(TransactionResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse getByReference(User currentUser, String reference) {
        Transaction tx = transactionRepository.findByReference(reference)
                .orElseThrow(() -> new EntityNotFoundException("Transaction introuvable"));

        boolean isSender = tx.getSender() != null && tx.getSender().getId().equals(currentUser.getId());
        boolean isReceiver = tx.getReceiver() != null && tx.getReceiver().getId().equals(currentUser.getId());

        if (!isSender && !isReceiver) {
            throw new AccessDeniedException("Cette transaction ne vous appartient pas");
        }
        return TransactionResponse.fromEntity(tx);
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS INTERNES
    // ═══════════════════════════════════════════════════════════════

    private void checkSufficientFunds(Wallet wallet, BigDecimal totalDebit) {
        BigDecimal available = wallet.getBalance().subtract(wallet.getLockedBalance());
        if (available.compareTo(totalDebit) < 0) {
            throw new InsufficientFundsException(
                    "Solde disponible insuffisant : " + available + " " + wallet.getCurrency()
                            + " (requis : " + totalDebit + " " + wallet.getCurrency() + ")"
            );
        }
    }

    /**
     * Vérifie que le cumul des sorties du jour (transferts + retraits) ne
     * dépasse pas la limite KYC de l'utilisateur. Calculé à la volée sur
     * les transactions du jour plutôt que stocké, pour rester toujours
     * cohérent avec l'historique réel (source de vérité unique).
     */
    private void checkDailyLimit(User currentUser, BigDecimal amount) {
        BigDecimal limit = TransactionPolicy.getDailyLimit(currentUser.getKycLevel());

        LocalDateTime startOfDay = LocalDateTime.of(LocalDateTime.now().toLocalDate(), LocalTime.MIDNIGHT);

        BigDecimal alreadySpentToday = transactionRepository
                .findBySenderIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(startOfDay))
                .filter(t -> t.getType() == TransactionType.TRANSFER_OUT || t.getType() == TransactionType.WITHDRAWAL)
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (alreadySpentToday.add(amount).compareTo(limit) > 0) {
            throw new KycLimitExceededException(
                    "Limite journalière dépassée pour votre niveau de vérification ("
                            + currentUser.getKycLevel() + " : " + limit + " XOF/jour). "
                            + "Soumettez une pièce d'identité pour augmenter votre limite."
            );
        }
    }

    /**
     * Génération de référence déléguée à TransactionReferenceGenerator
     * (composant partagé avec VaultService, cf. ce fichier).
     */
}
