package com.kola.backend.transaction;

import com.kola.backend.aml.TransactionCompletedEvent;
import com.kola.backend.beneficiary.Beneficiary;
import com.kola.backend.beneficiary.BeneficiaryRepository;
import com.kola.backend.exception.InsufficientFundsException;
import com.kola.backend.exception.KycLimitExceededException;
import com.kola.backend.merchant.Merchant;
import com.kola.backend.merchant.MerchantRepository;
import com.kola.backend.notification.NotificationService;
import com.kola.backend.notification.NotificationType;
import com.kola.backend.user.User;
import com.kola.backend.user.UserRepository;
import com.kola.backend.wallet.Wallet;
import com.kola.backend.wallet.WalletService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletService walletService;
    private final BeneficiaryRepository beneficiaryRepository;
    private final UserRepository userRepository;
    private final TransactionReferenceGenerator referenceGenerator;
    private final MerchantRepository merchantRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    // ═══════════════════════════════════════════════════════════════
    //  DÉPÔT (DEPOSIT) — recharge depuis Mobile Money
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public TransactionResponse deposit(User currentUser, DepositRequest request) {
        String idempotencyKey = normalizeKey(request.idempotencyKey());

        TransactionResponse replay = findReplay(idempotencyKey);
        if (replay != null) {
            return replay;
        }

        Wallet wallet = walletService.findOwnedWalletForUpdateOrThrow(currentUser, request.walletId());

        checkDailyDepositLimit(currentUser, request.amount());

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

        notificationService.notify(
                currentUser,
                "Dépôt effectué",
                "Votre wallet a été crédité de " + request.amount() + " " + wallet.getCurrency() + ".",
                NotificationType.TRANSACTION
        );

        eventPublisher.publishEvent(new TransactionCompletedEvent(currentUser.getId(), tx.getId()));

        return TransactionResponse.fromEntity(tx);
    }

    // ═══════════════════════════════════════════════════════════════
    //  RETRAIT (WITHDRAWAL) — vers Mobile Money
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public TransactionResponse withdraw(User currentUser, WithdrawalRequest request) {
        String idempotencyKey = normalizeKey(request.idempotencyKey());

        TransactionResponse replay = findReplay(idempotencyKey);
        if (replay != null) {
            return replay;
        }

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
                .idempotencyKey(idempotencyKey)
                .description("Retrait vers Mobile Money")
                .build();

        transactionRepository.save(tx);

        notificationService.notify(
                currentUser,
                "Retrait effectué",
                "Un retrait de " + request.amount() + " " + wallet.getCurrency() + " a été traité.",
                NotificationType.TRANSACTION
        );

        eventPublisher.publishEvent(new TransactionCompletedEvent(currentUser.getId(), tx.getId()));

        return TransactionResponse.fromEntity(tx);
    }

    // ═══════════════════════════════════════════════════════════════
    //  TRANSFERT (TRANSFER_OUT / TRANSFER_IN) — vers un bénéficiaire
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public TransactionResponse transfer(User currentUser, TransferRequest request) {
        String idempotencyKey = normalizeKey(request.idempotencyKey());

        TransactionResponse replay = findReplay(idempotencyKey);
        if (replay != null) {
            return replay;
        }

        Beneficiary beneficiary = beneficiaryRepository.findById(request.beneficiaryId())
                .orElseThrow(() -> new EntityNotFoundException("Bénéficiaire introuvable"));

        if (!beneficiary.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Ce bénéficiaire ne vous appartient pas");
        }

        // Un bénéficiaire dont le numéro correspond à un compte Kola est un
        // transfert INTERNE : l'argent doit atterrir sur son portefeuille.
        // Auparavant, aucun TRANSFER_IN n'était jamais créé et `receiver`
        // restait null : le wallet émetteur était débité et la somme
        // n'arrivait nulle part — un transfert entre deux clients Kola
        // détruisait purement et simplement de la monnaie.
        User recipient = userRepository.findByPhoneNumber(beneficiary.getPhoneNumber())
                .orElse(null);

        if (recipient != null && recipient.getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException(
                    "Vous ne pouvez pas vous transférer de l'argent à vous-même.");
        }

        // Contrôles AVANT tout verrou (cf. WalletService.lockAllForUpdate).
        Wallet source = walletService.findOwnedActiveWalletOrThrow(currentUser, request.sourceWalletId());
        Wallet destination = recipient == null
                ? null
                : walletService.findOrCreateReceivingWallet(recipient, source.getCurrency());

        // Verrouillage ordonné des deux portefeuilles : indispensable pour ne
        // pas s'interbloquer avec un transfert croisé simultané.
        Map<Long, Wallet> locked = walletService.lockAllForUpdate(
                destination == null
                        ? List.of(source.getId())
                        : List.of(source.getId(), destination.getId()));

        Wallet sourceWallet = locked.get(source.getId());
        Wallet destinationWallet = destination == null ? null : locked.get(destination.getId());

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
                .receiver(recipient) // null si le bénéficiaire est externe
                .idempotencyKey(idempotencyKey)
                .receiverPhoneNumber(beneficiary.getPhoneNumber())
                .receiverCountryCode(beneficiary.getCountryCode())
                .description(request.description() != null ? request.description() : "Transfert vers " + beneficiary.getAlias())
                .build();
        transactionRepository.save(outTx);

        // TRANSFER_IN : contrepartie côté destinataire interne.
        // Il reçoit le montant net : les frais sont à la charge de l'émetteur
        // (totalDebit = montant + frais), il ne doit donc pas les subir.
        if (destinationWallet != null) {
            destinationWallet.setBalance(destinationWallet.getBalance().add(request.amount()));

            Transaction inTx = Transaction.builder()
                    .reference(referenceGenerator.generate())
                    .type(TransactionType.TRANSFER_IN)
                    .status(TransactionStatus.SUCCESS)
                    .amount(request.amount())
                    .fee(BigDecimal.ZERO)
                    .currency(destinationWallet.getCurrency())
                    .wallet(destinationWallet)
                    .sender(currentUser)
                    .receiver(recipient)
                    .receiverPhoneNumber(recipient.getPhoneNumber())
                    .receiverCountryCode(recipient.getCountryCode())
                    .description("Transfert reçu de " + currentUser.fullName())
                    .build();
            transactionRepository.save(inTx);

            notificationService.notify(
                    recipient,
                    "Transfert reçu",
                    "Vous avez reçu " + request.amount() + " " + destinationWallet.getCurrency()
                            + " de la part de " + currentUser.fullName() + ".",
                    NotificationType.TRANSACTION
            );
        }

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

        notificationService.notify(
                currentUser,
                "Transfert envoyé",
                "Vous avez envoyé " + request.amount() + " " + sourceWallet.getCurrency()
                        + " à " + beneficiary.getAlias() + ".",
                NotificationType.TRANSACTION
        );

        eventPublisher.publishEvent(new TransactionCompletedEvent(currentUser.getId(), outTx.getId()));

        return TransactionResponse.fromEntity(outTx);
    }

    // ═══════════════════════════════════════════════════════════════
    //  PAIEMENT MARCHAND (MERCHANT_PAYMENT) — scan QR ou saisie du code
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public TransactionResponse payMerchant(User currentUser, PayMerchantRequest request) {
        String idempotencyKey = normalizeKey(request.idempotencyKey());

        TransactionResponse replay = findReplay(idempotencyKey);
        if (replay != null) {
            return replay;
        }

        Wallet sourceWallet = walletService.findOwnedWalletForUpdateOrThrow(currentUser, request.sourceWalletId());

        Merchant merchant = merchantRepository.findByMerchantCodeForUpdate(request.merchantCode())
                .orElseThrow(() -> new EntityNotFoundException("Marchand introuvable"));

        // Gratuit (pas de frais sur les paiements marchands, cf. politique fintech habituelle).
        checkSufficientFunds(sourceWallet, request.amount());
        checkDailyLimit(currentUser, request.amount());

        sourceWallet.setBalance(sourceWallet.getBalance().subtract(request.amount()));
        merchant.setBalance(merchant.getBalance().add(request.amount()));
        merchantRepository.save(merchant);

        Transaction tx = Transaction.builder()
                .reference(referenceGenerator.generate())
                .type(TransactionType.MERCHANT_PAYMENT)
                .status(TransactionStatus.SUCCESS)
                .amount(request.amount())
                .fee(BigDecimal.ZERO)
                .currency(sourceWallet.getCurrency())
                .wallet(sourceWallet)
                .sender(currentUser)
                .idempotencyKey(idempotencyKey)
                .description("Paiement à " + merchant.getName())
                .build();
        transactionRepository.save(tx);

        notificationService.notify(
                currentUser,
                "Paiement effectué",
                "Vous avez payé " + request.amount() + " " + sourceWallet.getCurrency()
                        + " à " + merchant.getName() + ".",
                NotificationType.TRANSACTION
        );

        eventPublisher.publishEvent(new TransactionCompletedEvent(currentUser.getId(), tx.getId()));

        return TransactionResponse.fromEntity(tx);
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

    /** Une clé vide ou faite d'espaces vaut absence de clé. */
    private String normalizeKey(String idempotencyKey) {
        return idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim();
    }

    /**
     * Rejoue la réponse d'une opération déjà exécutée sous la même clé.
     *
     * Cette lecture ne suffit pas à elle seule : deux requêtes strictement
     * simultanées portant la même clé passent toutes les deux ici avant que
     * l'une n'ait inséré sa ligne. C'est la contrainte d'unicité sur
     * transactions.idempotency_key qui tranche alors, et GlobalExceptionHandler
     * traduit la violation en 409 IDEMPOTENCY_CONFLICT — le client doit
     * rejouer sa requête, qui tombera cette fois sur le cas nominal.
     * Le mouvement d'argent, lui, n'a jamais lieu deux fois.
     */
    private TransactionResponse findReplay(String idempotencyKey) {
        if (idempotencyKey == null) {
            return null;
        }
        return transactionRepository.findByIdempotencyKey(idempotencyKey)
                .map(TransactionResponse::fromEntity)
                .orElse(null);
    }

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
     * Plafond journalier d'entrée. Contrairement au plafond de sortie, il ne
     * protège pas l'utilisateur mais le système : tant que le dépôt n'est pas
     * adossé à une confirmation d'opérateur, cet appel est la seule chose qui
     * empêche un compte authentifié de se créditer un montant arbitraire.
     */
    private void checkDailyDepositLimit(User currentUser, BigDecimal amount) {
        BigDecimal limit = TransactionPolicy.getDailyDepositLimit(currentUser.getKycLevel());

        LocalDateTime startOfDay = LocalDateTime.of(LocalDateTime.now().toLocalDate(), LocalTime.MIDNIGHT);

        BigDecimal alreadyDepositedToday = transactionRepository
                .sumDepositedTodayBySender(currentUser.getId(), startOfDay);

        if (alreadyDepositedToday.add(amount).compareTo(limit) > 0) {
            throw new KycLimitExceededException(
                    "Plafond journalier de rechargement dépassé pour votre niveau de vérification ("
                            + currentUser.getKycLevel() + " : " + limit + " XOF/jour). "
                            + "Déjà rechargé aujourd'hui : " + alreadyDepositedToday + " XOF."
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