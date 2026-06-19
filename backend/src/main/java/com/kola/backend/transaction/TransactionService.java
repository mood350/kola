package com.kola.backend.transaction;

import com.kola.backend.beneficiary.Beneficiary;
import com.kola.backend.beneficiary.BeneficiaryRepository;
import com.kola.backend.exception.InsufficientFundsException;
import com.kola.backend.exception.KycLimitExceededException;
import com.kola.backend.user.User;
import com.kola.backend.user.UserRepository;
import com.kola.backend.wallet.Wallet;
import com.kola.backend.wallet.WalletService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletService walletService;
    private final BeneficiaryRepository beneficiaryRepository;
    private final UserRepository userRepository;
    private final TransactionReferenceGenerator referenceGenerator;

    // ═══════════════════════════════════════════════════════════════
    //  DÉPÔT (DEPOSIT) — recharge depuis Mobile Money
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public TransactionResponse deposit(User currentUser, DepositRequest request) {
        String idempotencyKey = request.idempotencyKey();

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return TransactionResponse.fromEntity(existing.get());
            }
        }

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
                .idempotencyKey(idempotencyKey)
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

        // FEE : trace séparée du prélèvement de frais
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
    public Page<TransactionResponse> getWalletHistory(User currentUser, Long walletId, Pageable pageable) {
        Wallet wallet = walletService.findOwnedWalletOrThrow(currentUser, walletId);
        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId(), pageable)
                .map(TransactionResponse::fromEntity);
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

    private void checkDailyLimit(User currentUser, BigDecimal amount) {
        BigDecimal limit = TransactionPolicy.getDailyLimit(currentUser.getKycLevel());

        User lockedUser = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));

        LocalDateTime startOfDay = LocalDateTime.of(LocalDateTime.now().toLocalDate(), LocalTime.MIDNIGHT);

        BigDecimal alreadySpentToday = transactionRepository
                .sumSpentTodayBySender(lockedUser.getId(), startOfDay);

        if (alreadySpentToday.add(amount).compareTo(limit) > 0) {
            throw new KycLimitExceededException(
                    "Limite journalière dépassée pour votre niveau de vérification ("
                            + lockedUser.getKycLevel() + " : " + limit + " XOF/jour). "
                            + "Soumettez une pièce d'identité pour augmenter votre limite."
            );
        }
    }
}