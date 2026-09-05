package com.dogaa.backend.modules.transaction.service;

import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.Role;
import com.dogaa.backend.common.enums.TransactionStatus;
import com.dogaa.backend.common.enums.TransactionType;
import com.dogaa.backend.common.enums.WalletType;
import com.dogaa.backend.common.util.PhoneNumbers;
import com.dogaa.backend.common.util.Tokens;
import com.dogaa.backend.config.AuthProperties;
import com.dogaa.backend.exception.BadRequestException;
import com.dogaa.backend.exception.ConflictException;
import com.dogaa.backend.exception.ResourceNotFoundException;
import com.dogaa.backend.modules.transaction.dto.BillPaymentRequest;
import com.dogaa.backend.modules.transaction.dto.CashOutRequest;
import com.dogaa.backend.modules.transaction.dto.FeeAggregate;
import com.dogaa.backend.modules.transaction.dto.FeeQuoteRequest;
import com.dogaa.backend.modules.transaction.dto.FeeQuoteResponse;
import com.dogaa.backend.modules.transaction.dto.MerchantPaymentRequest;
import com.dogaa.backend.modules.transaction.dto.TransactionAggregate;
import com.dogaa.backend.modules.transaction.dto.TransferRequest;
import com.dogaa.backend.modules.transaction.entity.Transaction;
import com.dogaa.backend.modules.transaction.repository.TransactionRepository;
import com.dogaa.backend.modules.transaction.repository.TransactionSpecifications;
import com.dogaa.backend.modules.user.entity.User;
import com.dogaa.backend.modules.user.service.UserService;
import com.dogaa.backend.modules.wallet.entity.Wallet;
import com.dogaa.backend.modules.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Runs the outgoing money movements of DOGAA.md 4.1 — P2P transfer, merchant payment,
 * cash-out — and writes the trace for each one.
 *
 * <p>Each operation is one transaction: the wallet debit, the matching credit (or external
 * payout) and the saved {@link Transaction} row either all happen or none do. Money moves
 * only through {@link WalletService}; users are resolved only through {@link UserService}.
 * A refused move (insufficient funds, KYC limit, frozen wallet) rolls the whole thing back
 * and surfaces as a 4xx — the {@code FAILED} trace status is produced by the scheduler
 * (DOGAA.md 4.6), not this path.
 */
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletService walletService;
    private final UserService userService;
    private final FeeCalculator feeCalculator;
    private final KycLimitPolicy kycLimitPolicy;
    private final ExternalTransferGateway externalTransferGateway;
    private final AuthProperties authProperties;
    private final TransactionEventBroadcaster eventBroadcaster;

    // --- Fee preview ---------------------------------------------------

    @Transactional(readOnly = true)
    public FeeQuoteResponse quote(UUID userId, FeeQuoteRequest request) {
        User user = userService.getById(userId);
        BigDecimal fee = feeCalculator.feeFor(
                request.type(), request.amount(), request.currency(), user.getKycTier());
        return new FeeQuoteResponse(
                request.currency(), request.amount(), fee, request.amount().add(fee));
    }

    // --- Cash-in (DOGAA.md 5.3.A) --------------------------------------

    /**
     * Credits the wallet and writes the matching {@code CASH_IN} trace — free by default
     * (spec: a deliberate acquisition strategy), still routed through {@link FeeCalculator}
     * so a configured {@code app.fees.cash-in-percent} is honoured if one is ever set.
     */
    @Transactional
    public Transaction cashIn(UUID ownerId, Currency currency, BigDecimal amount) {
        User owner = userService.getById(ownerId);
        // The balance ceiling of the tier, checked before the money lands rather than after: the
        // answer to hitting it is "raise your KYC level", which only makes sense as a refusal.
        // Summed over the user's wallets in this currency only — there is no FX source to pool
        // currencies with, and pretending otherwise would compare unrelated numbers.
        kycLimitPolicy.checkResultingBalance(owner.getKycTier(), heldIn(ownerId, currency), amount);
        Wallet wallet = walletService.deposit(ownerId, currency, amount);
        BigDecimal fee = feeCalculator.feeFor(TransactionType.CASH_IN, amount, currency, owner.getKycTier());

        return complete(Transaction.builder()
                .reference(newReference())
                .type(TransactionType.CASH_IN)
                .currency(currency)
                .amount(amount)
                .fee(fee)
                .recipientId(ownerId)
                .destinationWalletId(wallet.getId())
                .counterparty("EXTERNAL")
                .description("Cash-in"));
    }

    // --- P2P transfer -------------------------------------------------

    @Transactional
    public Transaction transfer(UUID senderId, TransferRequest request) {
        User sender = userService.getById(senderId);
        Currency currency = request.currency();
        BigDecimal amount = request.amount();
        String recipientPhone = normalize(request.recipientPhone());

        Wallet source = walletService.getWallet(senderId, currency);
        BigDecimal fee = feeCalculator.feeFor(
                TransactionType.P2P_TRANSFER, amount, currency, sender.getKycTier());
        kycLimitPolicy.checkSendLimits(senderId, sender.getKycTier(), currency, amount);

        String reference = newReference();
        Transaction.TransactionBuilder trace = Transaction.builder()
                .reference(reference)
                .type(TransactionType.P2P_TRANSFER)
                .currency(currency)
                .amount(amount)
                .fee(fee)
                .senderId(senderId)
                .sourceWalletId(source.getId())
                .counterparty(recipientPhone)
                .description(request.description());

        Optional<User> recipient = userService.findByPhone(recipientPhone);
        if (recipient.isPresent()) {
            User r = recipient.get();
            if (r.getId().equals(senderId)) {
                throw new BadRequestException("You cannot transfer to yourself");
            }
            Wallet destination = walletInCurrency(r.getId(), currency,
                    "Recipient has no " + currency + " wallet");
            walletService.debit(source.getId(), amount.add(fee));
            walletService.credit(destination.getId(), amount);
            trace.recipientId(r.getId()).destinationWalletId(destination.getId());
        } else {
            // Unknown number -> pay it out through Mobile Money.
            walletService.debit(source.getId(), amount.add(fee));
            externalTransferGateway.payout(recipientPhone, amount, currency, reference);
        }
        return complete(trace);
    }

    // --- Merchant payment -------------------------------------------

    @Transactional
    public Transaction payMerchant(UUID payerId, MerchantPaymentRequest request) {
        User payer = userService.getById(payerId);
        Currency currency = request.currency();
        BigDecimal amount = request.amount();

        User merchant = userService.findByPhone(normalize(request.merchantCode()))
                .filter(u -> u.getRole() == Role.MERCHANT)
                .orElseThrow(() -> new BadRequestException(
                        "No registered merchant for code " + request.merchantCode()));
        if (merchant.getId().equals(payerId)) {
            throw new BadRequestException("You cannot pay yourself");
        }

        Wallet source = walletService.getWallet(payerId, currency);
        Wallet destination = walletInCurrency(merchant.getId(), currency,
                "Merchant has no " + currency + " wallet");
        BigDecimal fee = feeCalculator.feeFor(
                TransactionType.MERCHANT_PAYMENT, amount, currency, payer.getKycTier());
        kycLimitPolicy.checkSendLimits(payerId, payer.getKycTier(), currency, amount);

        walletService.debit(source.getId(), amount.add(fee));
        walletService.credit(destination.getId(), amount);

        return complete(Transaction.builder()
                .reference(newReference())
                .type(TransactionType.MERCHANT_PAYMENT)
                .currency(currency)
                .amount(amount)
                .fee(fee)
                .senderId(payerId)
                .sourceWalletId(source.getId())
                .recipientId(merchant.getId())
                .destinationWalletId(destination.getId())
                .counterparty(request.merchantCode())
                .description(request.description()));
    }

    // --- Cash-out --------------------------------------------------

    @Transactional
    public Transaction cashOut(UUID userId, CashOutRequest request) {
        User user = userService.getById(userId);
        Currency currency = request.currency();
        BigDecimal amount = request.amount();
        String phone = normalize(request.phoneNumber());

        Wallet source = walletService.getWallet(userId, currency);
        BigDecimal fee = feeCalculator.feeFor(
                TransactionType.CASH_OUT, amount, currency, user.getKycTier());
        kycLimitPolicy.checkSendLimits(userId, user.getKycTier(), currency, amount);

        String reference = newReference();
        walletService.debit(source.getId(), amount.add(fee));
        externalTransferGateway.payout(phone, amount, currency, reference);

        return complete(Transaction.builder()
                .reference(reference)
                .type(TransactionType.CASH_OUT)
                .currency(currency)
                .amount(amount)
                .fee(fee)
                .senderId(userId)
                .sourceWalletId(source.getId())
                .counterparty(request.provider() == null ? phone : request.provider() + ":" + phone)
                .description("Cash-out"));
    }

    // --- Bill payment (DOGAA.md 4.6.1) ------------------------------

    @Transactional
    public Transaction payBill(UUID payerId, BillPaymentRequest request) {
        User payer = userService.getById(payerId);
        Currency currency = request.currency();
        BigDecimal amount = request.amount();

        Wallet source = walletService.getWallet(payerId, currency);
        BigDecimal fee = feeCalculator.feeFor(
                TransactionType.BILL_PAYMENT, amount, currency, payer.getKycTier());
        kycLimitPolicy.checkSendLimits(payerId, payer.getKycTier(), currency, amount);

        String reference = newReference();
        walletService.debit(source.getId(), amount.add(fee));
        externalTransferGateway.payout(request.billerReference(), amount, currency, reference);

        return complete(Transaction.builder()
                .reference(reference)
                .type(TransactionType.BILL_PAYMENT)
                .currency(currency)
                .amount(amount)
                .fee(fee)
                .senderId(payerId)
                .sourceWalletId(source.getId())
                .counterparty(request.billerReference())
                .description(request.description() == null ? "Bill payment" : request.description()));
    }

    // --- Traces for movements owned by other modules ------------

    /**
     * Records a completed {@code VAULT_DEPOSIT} / {@code VAULT_WITHDRAWAL} trace. The vault
     * module has already moved the money through {@link WalletService}; this only writes the
     * history row so vault activity shows up in the user's transaction list (DOGAA.md 4.2).
     */
    @Transactional
    public Transaction recordVaultMovement(TransactionType type, UUID ownerId, UUID walletId,
                                           Currency currency, BigDecimal amount, String vaultName) {
        boolean deposit = type == TransactionType.VAULT_DEPOSIT;
        return complete(Transaction.builder()
                .reference(newReference())
                .type(type)
                .currency(currency)
                .amount(amount)
                .fee(BigDecimal.ZERO)
                .senderId(ownerId)
                .recipientId(ownerId)
                .sourceWalletId(deposit ? walletId : null)
                .destinationWalletId(deposit ? null : walletId)
                .counterparty(vaultName)
                .description(deposit ? "Vault deposit" : "Vault withdrawal"));
    }

    /** Everything the user holds in one currency, spendable and locked alike. */
    private BigDecimal heldIn(UUID ownerId, Currency currency) {
        return walletService.listWallets(ownerId).stream()
                .filter(w -> w.getCurrency() == currency)
                .map(w -> w.getAvailableBalance().add(w.getLockedBalance()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // --- Savings account --------------------------------------------

    /**
     * Moves money from the current account into the savings account that secures loans.
     *
     * <p>Without this the savings wallet was provisioned at sign-up and could never be funded: no
     * route reached it, so {@code CreditService} — which requires a minimum collateral — refused
     * every borrower, and the savings-discipline axis of the score, worth 30 points, was
     * structurally stuck at zero.
     *
     * <p>Free and not outgoing. The user is not spending, they are putting money aside; charging a
     * commission or counting it against the KYC send ceiling would penalise the exact behaviour
     * the product is built to encourage. Both wallet ids are recorded, which is what lets
     * {@code ScoringDataCollector} recognise it as an internal move and count it as savings rather
     * than as new income.
     */
    @Transactional
    public Transaction depositToSavings(UUID userId, Currency currency, BigDecimal amount) {
        Wallet current = walletService.getWallet(userId, currency);
        Wallet savings = walletService.getWallet(userId, currency, WalletType.SAVINGS);

        walletService.debit(current.getId(), amount);
        walletService.credit(savings.getId(), amount);

        return complete(Transaction.builder()
                .reference(newReference())
                .type(TransactionType.SAVINGS_DEPOSIT)
                .currency(currency)
                .amount(amount)
                .fee(BigDecimal.ZERO)
                .senderId(userId)
                .recipientId(userId)
                .sourceWalletId(current.getId())
                .destinationWalletId(savings.getId())
                .counterparty("Compte épargne")
                .description("Versement sur l'épargne"));
    }

    /**
     * Moves money back from savings to the current account.
     *
     * <p>A running loan needs no special case here: it freezes the collateral by moving the whole
     * savings balance into {@code lockedBalance}, and {@link WalletService#debit} only ever spends
     * the available side. The refusal is therefore structural rather than a rule someone has to
     * remember to write — which is the same reason the two accounts are one entity with a type.
     */
    @Transactional
    public Transaction withdrawFromSavings(UUID userId, Currency currency, BigDecimal amount) {
        Wallet savings = walletService.getWallet(userId, currency, WalletType.SAVINGS);
        Wallet current = walletService.getWallet(userId, currency);

        walletService.debit(savings.getId(), amount);
        walletService.credit(current.getId(), amount);

        return complete(Transaction.builder()
                .reference(newReference())
                .type(TransactionType.SAVINGS_WITHDRAWAL)
                .currency(currency)
                .amount(amount)
                .fee(BigDecimal.ZERO)
                .senderId(userId)
                .recipientId(userId)
                .sourceWalletId(savings.getId())
                .destinationWalletId(current.getId())
                .counterparty("Compte épargne")
                .description("Retrait de l'épargne"));
    }

    /**
     * Persists a {@code FAILED} trace for a due item the scheduler could not execute
     * (DOGAA.md 4.6.3 step 4: "la transaction est marquée en échec, l'utilisateur est
     * notifié").
     *
     * <p>Runs in its own transaction: the failed execution attempt is rolled back by its
     * caller, but the trace has to survive so the user's dashboard and any retry can see it —
     * same reason {@code UserService.registerFailedPinAttempt} uses {@code REQUIRES_NEW}.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction recordFailed(FailedTransactionCommand command) {
        Transaction tx = Transaction.builder()
                .reference(newReference())
                .type(command.type())
                .currency(command.currency())
                .amount(command.amount())
                .fee(command.fee() == null ? BigDecimal.ZERO : command.fee())
                .senderId(command.senderId())
                .sourceWalletId(command.sourceWalletId())
                .recipientId(command.recipientId())
                .destinationWalletId(command.destinationWalletId())
                .counterparty(command.counterparty())
                .description(command.description())
                .build();
        tx.setStatus(TransactionStatus.FAILED);
        tx.setFailureReason(command.failureReason());
        Transaction saved = transactionRepository.save(tx);
        eventBroadcaster.publish(saved);
        return saved;
    }

    // --- History -------------------------------------------------

    @Transactional(readOnly = true)
    public Page<Transaction> history(UUID userId, Pageable pageable) {
        return history(userId, null, null, null, null, pageable);
    }

    /** Same as {@link #history(UUID, Pageable)}, narrowed by type/status/date window (any {@code null} is ignored). */
    @Transactional(readOnly = true)
    public Page<Transaction> history(UUID userId, TransactionType type, TransactionStatus status,
                                     Instant from, Instant to, Pageable pageable) {
        return transactionRepository.findAll(
                TransactionSpecifications.forUser(userId, type, status, from, to),
                withNewestFirst(pageable));
    }

    /** The old JPQL carried its own ORDER BY; a specification does not, so it is added here. */
    private static Pageable withNewestFirst(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /**
     * Reads a transaction by its id, with no ownership check: for modules acting on behalf of the
     * platform rather than a user — the disputes module resolving a chargeback, for instance.
     */
    @Transactional(readOnly = true)
    public Transaction getById(UUID transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction introuvable : " + transactionId));
    }

    @Transactional(readOnly = true)
    public Transaction getForUser(UUID userId, String reference) {
        Transaction tx = transactionRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + reference));
        if (!userId.equals(tx.getSenderId()) && !userId.equals(tx.getRecipientId())) {
            throw new ResourceNotFoundException("Transaction not found: " + reference);
        }
        return tx;
    }

    // --- Chargeback (DOGAA.md 4.5) --------------------------------

    /** The trace behind a public reference, for the back-office. */
    @Transactional(readOnly = true)
    public Transaction getByReference(String reference) {
        return transactionRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + reference));
    }

    /**
     * Undoes a completed movement and writes the counter-entry (DOGAA.md 4.5).
     *
     * <p>The payer is made whole for what they actually parted with — amount <em>and</em> fee —
     * while the beneficiary gives back only the amount they received: the commission was Dogaa's,
     * so Dogaa is what absorbs it. Reversing is deliberately not free for the platform.
     *
     * <p>Two rows come out of it: the original flips to {@code REVERSED} and a new
     * {@code CHARGEBACK} trace records the money going the other way. Editing the original alone
     * would erase the fact that it ever completed, and the customer's history would lose a movement
     * that really happened.
     *
     * <p>When the beneficiary has already spent the money the debit fails and the whole reversal
     * rolls back — deliberately. Handing the payer money that was never recovered is a decision for
     * a human, not a side effect of clicking "valider".
     */
    @Transactional
    public Transaction reverse(Transaction original, String description) {
        if (original.getStatus() != TransactionStatus.COMPLETED) {
            throw new ConflictException("Only a completed transaction can be reversed; this one is "
                    + original.getStatus().name().toLowerCase());
        }
        if (original.getType() == TransactionType.CHARGEBACK) {
            throw new ConflictException("A chargeback cannot itself be charged back");
        }

        if (original.getDestinationWalletId() != null) {
            walletService.debit(original.getDestinationWalletId(), original.getAmount());
        }
        if (original.getSourceWalletId() != null) {
            walletService.credit(original.getSourceWalletId(), original.getTotalDebited());
        }

        original.setStatus(TransactionStatus.REVERSED);
        transactionRepository.save(original);

        // Sides swapped: the money travels back the way it came.
        return complete(Transaction.builder()
                .reference(newReference())
                .type(TransactionType.CHARGEBACK)
                .currency(original.getCurrency())
                .amount(original.getTotalDebited())
                .fee(BigDecimal.ZERO)
                .senderId(original.getRecipientId())
                .sourceWalletId(original.getDestinationWalletId())
                .recipientId(original.getSenderId())
                .destinationWalletId(original.getSourceWalletId())
                .counterparty(original.getReference())
                .description(description));
    }

    // --- Admin aggregates (DOGAA.md 4.5) --------------------------

    /**
     * Transaction volume and fees collected, one row per currency. Read-only
     * metric for dogaa-admin — go through this method, never
     * {@code TransactionRepository} directly.
     */
    @Transactional(readOnly = true)
    public List<TransactionAggregate> aggregateByCurrency(TransactionStatus status) {
        return transactionRepository.aggregateByCurrency(status);
    }

    /** Commission collected per movement type — the revenue breakdown of BACKEND.md 8. */
    @Transactional(readOnly = true)
    public List<FeeAggregate> aggregateFeesByType(TransactionStatus status) {
        return transactionRepository.aggregateFeesByType(status);
    }

    // --- Helpers -----------------------------------------------

    private Transaction complete(Transaction.TransactionBuilder trace) {
        Transaction tx = trace.build();
        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setCompletedAt(Instant.now());
        Transaction saved = transactionRepository.save(tx);
        eventBroadcaster.publish(saved);
        return saved;
    }

    private Wallet walletInCurrency(UUID ownerId, Currency currency, String messageIfMissing) {
        try {
            return walletService.getWallet(ownerId, currency);
        } catch (ResourceNotFoundException ex) {
            throw new BadRequestException(messageIfMissing);
        }
    }

    private String normalize(String rawPhone) {
        try {
            return PhoneNumbers.normalize(rawPhone, authProperties.getDefaultCallingCode());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }

    private String newReference() {
        return "TXN-" + com.dogaa.backend.common.util.Tokens.numericCode(10);
    }
}
