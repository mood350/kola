package com.dogaa.backend.modules.dispute.service;

import com.dogaa.backend.common.enums.TransactionType;
import com.dogaa.backend.common.util.BackOfficeFormat;
import com.dogaa.backend.common.util.PhoneNumbers;
import com.dogaa.backend.config.DisputeProperties;
import com.dogaa.backend.exception.BadRequestException;
import com.dogaa.backend.exception.ConflictException;
import com.dogaa.backend.exception.ResourceNotFoundException;
import com.dogaa.backend.modules.admin.security.CurrentAdmin;
import com.dogaa.backend.modules.audit.service.AuditService;
import com.dogaa.backend.modules.dispute.dto.DisputeDetailResponse;
import com.dogaa.backend.modules.dispute.dto.DisputeResponse;
import com.dogaa.backend.modules.dispute.dto.OpenDisputeRequest;
import com.dogaa.backend.modules.dispute.entity.Dispute;
import com.dogaa.backend.modules.dispute.entity.DisputeStatus;
import com.dogaa.backend.modules.dispute.entity.DisputeValidation;
import com.dogaa.backend.modules.dispute.repository.DisputeRepository;
import com.dogaa.backend.modules.dispute.repository.DisputeValidationRepository;
import com.dogaa.backend.modules.transaction.entity.Transaction;
import com.dogaa.backend.modules.transaction.service.TransactionService;
import com.dogaa.backend.modules.user.entity.User;
import com.dogaa.backend.modules.user.service.UserService;
import com.dogaa.backend.modules.wallet.entity.Wallet;
import com.dogaa.backend.modules.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Contested transactions and their chargebacks (DOGAA.md 4.5, BACKEND.md 9).
 *
 * <p>A chargeback takes money from one customer and gives it to another on nothing but an internal
 * decision, so it requires two <em>distinct</em> administrators. That is enforced three ways: this
 * service refuses a repeat validator, the unique index on {@code (dispute_id, admin_id)} refuses one
 * even under a race, and both validations are kept with their author's name. A counter alone would
 * let one person sign twice and pay themselves.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DisputeService {

    private static final String MODULE = "disputes";

    private final DisputeRepository disputeRepository;
    private final DisputeValidationRepository validationRepository;
    private final TransactionService transactionService;
    private final WalletService walletService;
    private final UserService userService;
    private final AuditService auditService;
    private final DisputeProperties properties;

    // --- opening ----------------------------------------------------------

    /** A customer contests one of their own transactions. */
    @Transactional
    public DisputeResponse open(UUID reporterId, OpenDisputeRequest request) {
        Transaction transaction = transactionService.getForUser(reporterId, request.transactionReference());

        if (disputeRepository.existsByTransactionReference(transaction.getReference())) {
            throw new ConflictException("Cette transaction fait déjà l'objet d'un litige");
        }

        Dispute dispute = disputeRepository.save(Dispute.builder()
                .transactionReference(transaction.getReference())
                .transactionId(transaction.getId())
                .reportedBy(reporterId)
                .tag(request.tag())
                .title(request.title().trim())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .validationsRequired(properties.getValidationsRequired())
                .status(DisputeStatus.OPEN)
                .build());

        log.info("Dispute opened on {} by {} ({})",
                transaction.getReference(), reporterId, request.tag());
        return toResponse(dispute);
    }

    // --- back-office reads ------------------------------------------------

    @Transactional(readOnly = true)
    public List<DisputeResponse> list() {
        return disputeRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DisputeDetailResponse detail(String reference) {
        return toDetail(getByReference(reference));
    }

    // --- decisions --------------------------------------------------------

    /** Proposes a chargeback. Nothing moves yet: the money waits for the validations. */
    @Transactional
    public DisputeResponse proposeChargeback(CurrentAdmin admin, String reference) {
        Dispute dispute = getByReference(reference);
        requireOpen(dispute);

        dispute.setStatus(DisputeStatus.CHARGEBACK_PENDING);
        dispute.setLastValidationNote("Chargeback proposé par " + admin.name()
                + " — " + dispute.getValidationsRequired() + " validations requises");
        disputeRepository.save(dispute);

        auditService.record(admin, MODULE, "Chargeback proposé sur " + reference,
                AuditService.diff(DisputeStatus.OPEN.getCode(),
                        DisputeStatus.CHARGEBACK_PENDING.getCode()),
                "Dispute", reference);

        return toResponse(dispute);
    }

    @Transactional
    public DisputeResponse reject(CurrentAdmin admin, String reference) {
        Dispute dispute = getByReference(reference);
        if (dispute.getStatus().isClosed()) {
            throw new ConflictException("Ce litige est déjà clos");
        }
        DisputeStatus previous = dispute.getStatus();

        dispute.setStatus(DisputeStatus.REJECTED);
        dispute.setLastValidationNote("Classé sans suite par " + admin.name());
        disputeRepository.save(dispute);

        auditService.record(admin, MODULE, "Litige " + reference + " classé sans suite",
                AuditService.diff(previous.getCode(), DisputeStatus.REJECTED.getCode()),
                "Dispute", reference);

        return toResponse(dispute);
    }

    /**
     * Signs off on a pending chargeback. The last signature required executes it.
     *
     * @throws ConflictException if this administrator has already validated this dispute
     */
    @Transactional
    public DisputeDetailResponse validate(CurrentAdmin admin, String reference) {
        Dispute dispute = getByReference(reference);

        if (dispute.getStatus() != DisputeStatus.CHARGEBACK_PENDING) {
            throw new ConflictException(
                    "Seul un chargeback en attente peut être validé (statut actuel : "
                            + dispute.getStatus().getCode() + ")");
        }
        if (validationRepository.existsByDisputeIdAndAdminId(dispute.getId(), admin.id())) {
            // The whole point of four eyes: the second pair must belong to someone else.
            throw new ConflictException("Vous avez déjà validé ce litige — "
                    + "la seconde validation doit venir d'un autre administrateur");
        }

        validationRepository.save(DisputeValidation.builder()
                .disputeId(dispute.getId())
                .adminId(admin.id())
                .adminName(admin.name())
                .build());

        int done = (int) validationRepository.countByDisputeId(dispute.getId());
        auditService.record(admin, MODULE,
                "Validation " + done + "/" + dispute.getValidationsRequired()
                        + " du chargeback " + reference,
                null, "Dispute", reference);

        if (done >= dispute.getValidationsRequired()) {
            executeChargeback(dispute, admin);
        } else {
            dispute.setLastValidationNote(done + "re validation : " + admin.name()
                    + " — en attente d'un " + (done + 1) + "e administrateur");
            disputeRepository.save(dispute);
        }
        return toDetail(dispute);
    }

    /**
     * Moves the money back.
     *
     * <p>The victim is made whole in full. Recovery from the party who received the money is
     * limited to what they still hold — they may well have spent it — and whatever is missing is
     * recorded as a shortfall the platform absorbed. Refusing to refund until the beneficiary
     * happens to be solvent would punish the wrong person.
     */
    private void executeChargeback(Dispute dispute, CurrentAdmin admin) {
        Transaction transaction = transactionService.getById(dispute.getTransactionId());

        BigDecimal recovered = BigDecimal.ZERO;
        if (transaction.getDestinationWalletId() != null) {
            Wallet beneficiary = walletService.getById(transaction.getDestinationWalletId());
            recovered = beneficiary.getAvailableBalance().min(dispute.getAmount());
            if (recovered.signum() > 0) {
                walletService.debit(beneficiary.getId(), recovered);
            }
        }
        if (transaction.getSourceWalletId() != null) {
            walletService.credit(transaction.getSourceWalletId(), dispute.getAmount());
        }

        BigDecimal shortfall = dispute.getAmount().subtract(recovered).max(BigDecimal.ZERO);
        dispute.setShortfallAmount(shortfall);
        dispute.setStatus(DisputeStatus.RESOLVED);

        List<DisputeValidation> validations =
                validationRepository.findByDisputeIdOrderByCreatedAtAsc(dispute.getId());
        String signatories = validations.stream()
                .map(DisputeValidation::getAdminName)
                .reduce((a, b) -> a + " et " + b)
                .orElse(admin.name());

        dispute.setLastValidationNote("Chargeback exécuté après validation de " + signatories
                + (shortfall.signum() > 0
                ? " — " + BackOfficeFormat.amount(shortfall, dispute.getCurrency().name())
                + " non récupérés auprès du bénéficiaire"
                : ""));
        disputeRepository.save(dispute);

        transactionService.recordVaultMovement(TransactionType.CHARGEBACK, dispute.getReportedBy(),
                transaction.getSourceWalletId(), dispute.getCurrency(), dispute.getAmount(),
                "Chargeback " + dispute.getTransactionReference());

        auditService.record(admin, MODULE,
                "Chargeback exécuté sur " + dispute.getTransactionReference()
                        + " (validé par " + signatories + ")",
                AuditService.diff(DisputeStatus.CHARGEBACK_PENDING.getCode(),
                        DisputeStatus.RESOLVED.getCode()),
                "Dispute", dispute.getTransactionReference());

        log.warn("Chargeback executed on {}: {} returned, {} unrecovered",
                dispute.getTransactionReference(), dispute.getAmount(), shortfall);
    }

    // --- mapping ----------------------------------------------------------

    private DisputeResponse toResponse(Dispute dispute) {
        return new DisputeResponse(
                dispute.getTransactionReference(),
                dispute.getTag(),
                dispute.getTag().getLabel(),
                BackOfficeFormat.amount(dispute.getAmount(), dispute.getCurrency().name()),
                dispute.getTitle(),
                meta(dispute),
                dispute.getStatus());
    }

    private DisputeDetailResponse toDetail(Dispute dispute) {
        Transaction transaction = transactionService.getById(dispute.getTransactionId());

        return new DisputeDetailResponse(
                dispute.getTransactionReference(),
                describeParty(transaction.getSenderId(), transaction.getCounterparty()),
                describeParty(transaction.getRecipientId(), transaction.getCounterparty()),
                BackOfficeFormat.amount(dispute.getAmount(), dispute.getCurrency().name()),
                dispute.getValidationsRequired(),
                (int) validationRepository.countByDisputeId(dispute.getId()),
                dispute.getLastValidationNote());
    }

    /** "Ouvert il y a 2 h · TIER_2 · Lomé" — assembled here, displayed verbatim. */
    private String meta(Dispute dispute) {
        User reporter = userService.getById(dispute.getReportedBy());
        String age = openedAgo(dispute.getCreatedAt());
        String city = reporter.getCity() == null || reporter.getCity().isBlank()
                ? "—" : reporter.getCity();
        return "Ouvert " + age + " · " + reporter.getKycTier() + " · " + city;
    }

    private static String openedAgo(Instant createdAt) {
        if (createdAt == null) {
            return "à l'instant";
        }
        long hours = ChronoUnit.HOURS.between(createdAt, Instant.now());
        if (hours < 1) {
            return "il y a moins d'une heure";
        }
        if (hours < 24) {
            return "il y a " + hours + " h";
        }
        long days = hours / 24;
        return "il y a " + days + (days == 1 ? " jour" : " jours");
    }

    /** Phone numbers are masked even for staff: reviewing a dispute needs identity, not a number. */
    private String describeParty(UUID userId, String fallback) {
        if (userId == null) {
            return fallback == null ? "—" : fallback;
        }
        User user = userService.getById(userId);
        return user.getFullName() + " (" + PhoneNumbers.mask(user.getPhone()) + ")";
    }

    private Dispute getByReference(String reference) {
        return disputeRepository.findByTransactionReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Litige introuvable : " + reference));
    }

    private static void requireOpen(Dispute dispute) {
        if (dispute.getStatus() != DisputeStatus.OPEN) {
            throw new BadRequestException("Ce litige n'est plus ouvert (statut : "
                    + dispute.getStatus().getCode() + ")");
        }
    }
}
