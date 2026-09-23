package com.kola.backend.transaction;

import com.kola.backend.aml.TransactionCompletedEvent;
import com.kola.backend.beneficiary.Beneficiary;
import com.kola.backend.beneficiary.BeneficiaryRepository;
import com.kola.backend.exception.InsufficientFundsException;
import com.kola.backend.exception.KycLimitExceededException;
import com.kola.backend.merchant.Merchant;
import com.kola.backend.merchant.MerchantRepository;
import com.kola.backend.payment.MobileMoneyWithdrawalRequest;
import com.kola.backend.notification.NotificationService;
import com.kola.backend.notification.NotificationType;
import com.kola.backend.user.User;
import com.kola.backend.user.UserRepository;
import com.kola.backend.wallet.Wallet;
import com.kola.backend.wallet.WalletService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
    //  RETRAIT MOBILE MONEY — versement par un prestataire externe
    // ═══════════════════════════════════════════════════════════════

    /**
     * Ouvre un retrait : DÉBITE IMMÉDIATEMENT, puis attend le prestataire.
     *
     * ═══ POURQUOI L'ARGENT PART DU PORTEFEUILLE TOUT DE SUITE ═══
     *
     * C'est l'inverse du dépôt, et pour une raison qui n'a rien de symétrique.
     * Un dépôt en attente ne crédite pas : au pire l'utilisateur attend son
     * argent. Un retrait en attente qui ne débiterait pas laisserait la somme
     * disponible pendant tout le traitement — le temps de la dépenser une
     * seconde fois, par virement ou par paiement marchand. Le portefeuille
     * afficherait alors un solde que la banque ne peut plus honorer.
     *
     * Le débit est donc immédiat, et c'est l'échec qui recrédite.
     *
     * Les frais suivent la même règle que le retrait de test
     * ({@code TransactionPolicy.computeWithdrawalFee}) : le prestataire ne
     * change pas le tarif appliqué au client.
     */
    @Transactional
    public Transaction openMobileMoneyWithdrawal(User currentUser, MobileMoneyWithdrawalRequest request) {
        String idempotencyKey = normalizeKey(request.idempotencyKey());

        if (idempotencyKey != null) {
            Transaction replay = transactionRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
            if (replay != null) {
                return replay;
            }
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
                .status(TransactionStatus.PENDING)
                .amount(request.amount())
                .fee(fee)
                .currency(wallet.getCurrency())
                .wallet(wallet)
                .sender(currentUser)
                .receiverPhoneNumber(request.phoneNumber())
                .receiverCountryCode(request.mode().getCountryCode())
                .idempotencyKey(idempotencyKey)
                .description("Retrait " + request.mode().getLabel())
                .build();

        return transactionRepository.save(tx);
    }

    /**
     * Applique l'issue d'un versement : solde le retrait, ou rend l'argent.
     *
     * ═══ LE RECRÉDIT EST LA PARTIE CRITIQUE ═══
     *
     * Un versement qui échoue chez l'opérateur (numéro invalide, compte
     * plafonné, réseau indisponible) laisse un portefeuille débité d'un argent
     * qui n'est jamais arrivé. Sans ce chemin, l'utilisateur perd la somme et
     * les frais, et seul un correctif manuel le rattrape.
     *
     * Les FRAIS SONT RENDUS AUSSI : ils rémunèrent un service qui n'a pas été
     * rendu. Les garder sur un échec technique est indéfendable.
     *
     * Idempotent par la même mécanique que le dépôt — verrou pessimiste sur
     * l'écriture, puis garde de statut. La réconciliation peut repasser sur le
     * même retrait autant de fois qu'elle veut.
     */
    @Transactional
    public void settleMobileMoneyWithdrawal(String providerTransactionId, boolean sent) {
        Transaction tx = transactionRepository
                .findByProviderTransactionIdForUpdate(providerTransactionId)
                .orElse(null);

        if (tx == null) {
            log.warn("Issue de versement pour une opération inconnue : {}", providerTransactionId);
            return;
        }

        if (tx.getStatus() != TransactionStatus.PENDING) {
            log.info("Versement déjà soldé pour {} (statut {}) — ignoré",
                    tx.getReference(), tx.getStatus());
            return;
        }

        if (sent) {
            tx.setStatus(TransactionStatus.SUCCESS);
            transactionRepository.save(tx);

            notificationService.notify(
                    tx.getSender(),
                    "Retrait effectué",
                    "Votre retrait de " + tx.getAmount() + " " + tx.getCurrency()
                            + " a été envoyé sur votre compte Mobile Money.",
                    NotificationType.TRANSACTION
            );

            eventPublisher.publishEvent(new TransactionCompletedEvent(tx.getSender().getId(), tx.getId()));
            log.info("Retrait {} confirmé par le prestataire ({})",
                    tx.getReference(), providerTransactionId);
            return;
        }

        /* Échec : on rend le montant ET les frais, sous verrou — le solde a pu
           bouger depuis le débit (un transfert reçu, un dépôt confirmé). */
        Wallet wallet = walletService.lockForUpdate(tx.getWallet().getId());
        wallet.setBalance(wallet.getBalance().add(tx.getAmount()).add(tx.getFee()));

        tx.setStatus(TransactionStatus.FAILED);
        transactionRepository.save(tx);

        notificationService.notify(
                tx.getSender(),
                "Retrait échoué",
                "Votre retrait de " + tx.getAmount() + " " + tx.getCurrency()
                        + " n'a pas abouti. Le montant et les frais ont été recrédités.",
                NotificationType.TRANSACTION
        );

        log.warn("Retrait {} échoué chez le prestataire ({}) — portefeuille recrédité",
                tx.getReference(), providerTransactionId);
    }

    /**
     * Referme un retrait dont l'ordre n'a jamais pu être ouvert.
     *
     * Le portefeuille a été débité à l'ouverture, le prestataire n'a rien pris
     * en charge : il faut rendre, exactement comme sur un échec.
     */
    @Transactional
    public void abandonPendingWithdrawal(Long transactionId) {
        transactionRepository.findById(transactionId).ifPresent(tx -> {
            if (tx.getStatus() != TransactionStatus.PENDING) return;

            Wallet wallet = walletService.lockForUpdate(tx.getWallet().getId());
            wallet.setBalance(wallet.getBalance().add(tx.getAmount()).add(tx.getFee()));

            tx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(tx);
        });
    }

    /** Retraits encore en attente chez le prestataire, pour la réconciliation. */
    @Transactional(readOnly = true)
    public List<Transaction> findPendingProviderWithdrawals() {
        return transactionRepository.findByStatusAndTypeAndProviderNotNull(
                TransactionStatus.PENDING, TransactionType.WITHDRAWAL);
    }

    // ═══════════════════════════════════════════════════════════════
    //  DÉPÔT MOBILE MONEY — encaissement par un prestataire externe
    // ═══════════════════════════════════════════════════════════════

    /**
     * Ouvre une écriture de dépôt EN ATTENTE, sans créditer quoi que ce soit.
     *
     * ═══ POURQUOI L'ÉCRITURE EXISTE AVANT L'ARGENT ═══
     *
     * Parce que l'ordre inverse perd des dépôts. Si l'on appelait d'abord le
     * prestataire, une panne juste après son acceptation laisserait un débit
     * réel chez l'opérateur sans aucune trace côté Kola — de l'argent parti de
     * chez le client et arrivé nulle part, introuvable au support. En ouvrant
     * l'écriture d'abord, le pire cas devient une ligne PENDING orpheline :
     * visible, rapprochable, réparable.
     *
     * Le plafond KYC est vérifié ICI et pas à la confirmation : refuser après
     * que le client a saisi son code Mobile Money serait le prévenir trop tard.
     * Contrepartie assumée — une demande abandonnée consomme du plafond
     * journalier jusqu'à son échec.
     *
     * Le solde n'est pas touché, donc aucun verrou de wallet n'est pris : la
     * transaction reste courte, ce qui compte puisqu'un appel réseau suit.
     */
    @Transactional
    public Transaction openMobileMoneyDeposit(User currentUser, MobileMoneyDepositRequest request) {
        String idempotencyKey = normalizeKey(request.idempotencyKey());

        if (idempotencyKey != null) {
            Transaction replay = transactionRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
            if (replay != null) {
                return replay;
            }
        }

        Wallet wallet = walletService.findOwnedActiveWalletOrThrow(currentUser, request.walletId());

        checkDailyDepositLimit(currentUser, request.amount());

        Transaction tx = Transaction.builder()
                .reference(referenceGenerator.generate())
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.PENDING)
                .amount(request.amount())
                .fee(BigDecimal.ZERO)
                .currency(wallet.getCurrency())
                .wallet(wallet)
                .sender(currentUser)
                .receiverPhoneNumber(request.phoneNumber())
                .receiverCountryCode(request.mode().getCountryCode())
                .idempotencyKey(idempotencyKey)
                .description("Rechargement " + request.mode().getLabel())
                .build();

        return transactionRepository.save(tx);
    }

    /** Relie l'écriture en attente à l'opération ouverte chez le prestataire. */
    @Transactional
    public void attachProviderTransaction(Long transactionId, String provider,
                                          String providerTransactionId, String paymentUrl) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction introuvable : " + transactionId));
        tx.setProvider(provider);
        tx.setProviderTransactionId(providerTransactionId);
        /* Écrite telle quelle, nulle comprise : un prélèvement direct n'a pas
           de page, et laisser traîner l'URL d'une tentative précédente ferait
           rouvrir un paiement qui n'a plus cours. */
        tx.setProviderPaymentUrl(paymentUrl);
        transactionRepository.save(tx);
    }

    /**
     * Referme une écriture dont la demande n'a jamais été prise en charge.
     *
     * Appelée quand le prestataire refuse ou reste injoignable : rien n'a été
     * débité chez l'opérateur, la ligne ne deviendra jamais un dépôt. La
     * marquer FAILED plutôt que la supprimer garde la trace de la tentative —
     * le grand livre d'un service financier n'efface pas.
     */
    @Transactional
    public void abandonPendingDeposit(Long transactionId) {
        transactionRepository.findById(transactionId).ifPresent(tx -> {
            if (tx.getStatus() == TransactionStatus.PENDING) {
                tx.setStatus(TransactionStatus.FAILED);
                transactionRepository.save(tx);
            }
        });
    }

    /**
     * Applique le verdict du prestataire : crédite, ou classe l'échec.
     *
     * ═══ LE SEUL ENDROIT OÙ UN DÉPÔT MOBILE MONEY CRÉE DE L'ARGENT ═══
     *
     * Et il est idempotent par construction. Le webhook de FedaPay est rejoué
     * jusqu'à neuf fois ; une notification qui arrive deux fois doit créditer
     * une seule fois. Deux protections superposées, volontairement :
     *
     * 1. le verrou pessimiste sur l'écriture sérialise les notifications
     *    concurrentes — la seconde attend, puis voit SUCCESS ;
     * 2. le contrôle de statut ci-dessous ignore tout ce qui n'est plus en
     *    attente.
     *
     * La première sans la seconde ne protégerait pas d'un rejeu tardif ; la
     * seconde sans la première ne protégerait pas de deux threads simultanés.
     *
     * Une opération inconnue n'est pas une erreur à faire remonter : elle
     * signifie que la notification concerne une transaction étrangère à ce
     * grand livre (autre environnement, autre application partageant le compte
     * marchand). On la journalise et on l'acquitte — sans quoi FedaPay la
     * rejouerait jusqu'à désactiver l'endpoint.
     */
    @Transactional
    public void settleMobileMoneyDeposit(String providerTransactionId, boolean approved) {
        Transaction tx = transactionRepository
                .findByProviderTransactionIdForUpdate(providerTransactionId)
                .orElse(null);

        if (tx == null) {
            log.warn("Notification de paiement pour une opération inconnue : {}", providerTransactionId);
            return;
        }

        if (tx.getStatus() != TransactionStatus.PENDING) {
            log.info("Notification déjà traitée pour {} (statut {}) — ignorée",
                    tx.getReference(), tx.getStatus());
            return;
        }

        if (!approved) {
            tx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(tx);
            notificationService.notify(
                    tx.getSender(),
                    "Rechargement échoué",
                    "Votre rechargement de " + tx.getAmount() + " " + tx.getCurrency()
                            + " n'a pas abouti. Aucun montant n'a été débité.",
                    NotificationType.TRANSACTION
            );
            return;
        }

        /* Le wallet est relu SOUS VERROU : celui porté par l'écriture a été
           chargé avant l'appel réseau, et son solde peut avoir changé depuis
           (un virement reçu, un coffre débloqué). Créditer à partir de cette
           valeur périmée écraserait ces mouvements. */
        Wallet wallet = walletService.lockForUpdate(tx.getWallet().getId());
        wallet.setBalance(wallet.getBalance().add(tx.getAmount()));

        tx.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(tx);

        notificationService.notify(
                tx.getSender(),
                "Dépôt effectué",
                "Votre wallet a été crédité de " + tx.getAmount() + " " + tx.getCurrency() + ".",
                NotificationType.TRANSACTION
        );

        eventPublisher.publishEvent(new TransactionCompletedEvent(tx.getSender().getId(), tx.getId()));

        log.info("Dépôt {} crédité après confirmation du prestataire ({})",
                tx.getReference(), providerTransactionId);
    }

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
            /* Message court et sans jargon interne. Il disait le palier
               ("TIER_1"), le plafond et le cumul du jour : trois chiffres dont
               aucun ne dit à l'utilisateur quoi faire, et un identifiant
               technique qui ne veut rien dire hors du code. Ce qui compte tient
               en deux informations — c'est atteint, et voilà comment le lever. */
            throw new KycLimitExceededException(
                    "Plafond journalier de rechargement atteint. "
                            + "Complétez votre vérification d'identité pour l'augmenter."
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
                    "Plafond journalier atteint. "
                            + "Complétez votre vérification d'identité pour l'augmenter."
            );
        }
    }
}