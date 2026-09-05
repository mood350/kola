package com.dogaa.backend.modules.admin.service;

import com.dogaa.backend.common.util.RelativeTime;
import com.dogaa.backend.exception.ConflictException;
import com.dogaa.backend.exception.ForbiddenException;
import com.dogaa.backend.exception.ResourceNotFoundException;
import com.dogaa.backend.modules.admin.dto.DisputeDetailResponse;
import com.dogaa.backend.modules.admin.dto.DisputeResponse;
import com.dogaa.backend.modules.admin.entity.Dispute;
import com.dogaa.backend.modules.admin.entity.DisputeStatus;
import com.dogaa.backend.modules.admin.entity.DisputeValidation;
import com.dogaa.backend.modules.admin.repository.DisputeRepository;
import com.dogaa.backend.modules.admin.repository.DisputeValidationRepository;
import com.dogaa.backend.modules.admin.security.CurrentAdmin;
import com.dogaa.backend.modules.audit.service.AuditService;
import com.dogaa.backend.modules.transaction.entity.Transaction;
import com.dogaa.backend.modules.transaction.service.TransactionService;
import com.dogaa.backend.modules.user.entity.User;
import com.dogaa.backend.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Claims and chargebacks (DOGAA.md 4.5, BACKEND.md 9).
 *
 * <p>The rule that matters here is four-eyes: reversing a customer's money takes two <em>different</em>
 * administrators. That is enforced twice over — a unique constraint on (dispute, admin) in the
 * database, and an explicit check here that returns a message the console can show. A counter would
 * satisfy neither: it cannot tell one person clicking twice from two people agreeing.
 *
 * <p>The money itself moves through {@link TransactionService#reverse}, which owns the
 * {@code Transaction} aggregate; this service decides <em>whether</em>, never <em>how</em>.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDisputeService {

    private static final String MODULE = "disputes";

    private final DisputeRepository disputeRepository;
    private final DisputeValidationRepository validationRepository;
    private final TransactionService transactionService;
    private final UserService userService;
    private final AuditService auditService;

    // --- reading ----------------------------------------------------------

    @Transactional(readOnly = true)
    public List<DisputeResponse> list() {
        return disputeRepository.findAllByOrderByOpenedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DisputeDetailResponse detail(String reference) {
        return toDetail(require(reference));
    }

    // --- decisions --------------------------------------------------------

    /** Opens the chargeback procedure. Nothing moves yet — that takes the validations. */
    @Transactional
    public DisputeResponse startChargeback(CurrentAdmin admin, String reference) {
        Dispute dispute = require(reference);

        if (dispute.getStatus() != DisputeStatus.OPEN) {
            throw new ConflictException("Ce litige n'est plus ouvert : "
                    + dispute.getStatus().wireValue());
        }

        dispute.setStatus(DisputeStatus.CHARGEBACK_PENDING);
        dispute.setLastValidationNote("Chargeback lancé par " + shortName(admin.name())
                + " — en attente de " + dispute.getValidationsRequired() + " validations");
        disputeRepository.save(dispute);

        auditService.record(admin, MODULE, "Lancement d'un chargeback sur " + reference,
                AuditService.diff("open", "chargeback_pending"), "Dispute", reference);

        log.info("Admin {} started a chargeback on {}", admin.email(), reference);
        return toResponse(dispute);
    }

    /** Closes the claim without moving money. */
    @Transactional
    public DisputeResponse reject(CurrentAdmin admin, String reference) {
        Dispute dispute = require(reference);

        if (dispute.getStatus() == DisputeStatus.RESOLVED) {
            throw new ConflictException("Ce litige est déjà résolu : le chargeback a été exécuté");
        }
        if (dispute.getStatus() == DisputeStatus.REJECTED) {
            throw new ConflictException("Ce litige est déjà classé sans suite");
        }

        DisputeStatus previous = dispute.getStatus();
        dispute.setStatus(DisputeStatus.REJECTED);
        dispute.setLastValidationNote("Classé sans suite par " + shortName(admin.name()));
        disputeRepository.save(dispute);

        auditService.record(admin, MODULE, "Rejet du litige " + reference,
                AuditService.diff(previous.wireValue(), "rejected"), "Dispute", reference);

        log.info("Admin {} rejected dispute {}", admin.email(), reference);
        return toResponse(dispute);
    }

    /**
     * Signs off a pending chargeback, and executes it once enough distinct admins have.
     *
     * <p>The reversal runs inside this transaction: if the beneficiary's wallet can no longer cover
     * it, the validation is rolled back with it. A dispute recorded as resolved while the money
     * never moved is worse than one still waiting.
     */
    @Transactional
    public DisputeDetailResponse validate(CurrentAdmin admin, String reference) {
        Dispute dispute = require(reference);

        if (dispute.getStatus() != DisputeStatus.CHARGEBACK_PENDING) {
            throw new ConflictException("Aucun chargeback en attente sur ce litige");
        }
        if (validationRepository.existsByDisputeIdAndAdminId(dispute.getId(), admin.id())) {
            throw new ForbiddenException(
                    "Vous avez déjà validé ce chargeback ; la 2e validation doit venir d'un autre admin");
        }

        validationRepository.save(DisputeValidation.builder()
                .disputeId(dispute.getId())
                .adminId(admin.id())
                .adminName(admin.name())
                .build());

        long done = validationRepository.countByDisputeId(dispute.getId());

        if (done >= dispute.getValidationsRequired()) {
            execute(dispute, admin, done);
        } else {
            dispute.setLastValidationNote(ordinal(done) + " validation : " + shortName(admin.name())
                    + " — en attente d'un " + ordinal(done + 1) + " admin conformité");
            disputeRepository.save(dispute);

            auditService.record(admin, MODULE,
                    ordinal(done) + " validation du chargeback " + reference,
                    AuditService.diff(done - 1 + " validation(s)", done + " validation(s)"),
                    "Dispute", reference);
        }

        return toDetail(dispute);
    }

    private void execute(Dispute dispute, CurrentAdmin admin, long done) {
        Transaction original = transactionService.getByReference(dispute.getTransactionReference());
        transactionService.reverse(original,
                "Chargeback litige " + dispute.getTransactionReference());

        dispute.setStatus(DisputeStatus.RESOLVED);
        dispute.setLastValidationNote(ordinal(done) + " validation : " + shortName(admin.name())
                + " — chargeback exécuté, fonds reversés");
        disputeRepository.save(dispute);

        auditService.record(admin, MODULE,
                "Exécution du chargeback " + dispute.getTransactionReference(),
                AuditService.diff("chargeback_pending", "resolved"),
                "Dispute", dispute.getTransactionReference());

        log.warn("Chargeback executed on {} after {} validations (last: {})",
                dispute.getTransactionReference(), done, admin.email());
    }

    // --- mapping ----------------------------------------------------------

    private Dispute require(String reference) {
        return disputeRepository.findByTransactionReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Litige introuvable pour la référence " + reference));
    }

    private DisputeResponse toResponse(Dispute dispute) {
        User claimant = userService.getById(dispute.getClaimantId());

        String meta = "Ouvert " + RelativeTime.sinceInSentence(dispute.getOpenedAt())
                + " · " + claimant.getKycTier().name()
                + (claimant.getCity() == null || claimant.getCity().isBlank()
                        ? "" : " · " + claimant.getCity());

        return new DisputeResponse(
                dispute.getTransactionReference(),
                dispute.getTag().wireValue(),
                dispute.getTag().label(),
                AdminFormat.amount(dispute.getAmount(), dispute.getCurrency().name()),
                dispute.getTitle(),
                meta,
                dispute.getStatus().wireValue());
    }

    private DisputeDetailResponse toDetail(Dispute dispute) {
        Transaction tx = transactionService.getByReference(dispute.getTransactionReference());
        int done = (int) validationRepository.countByDisputeId(dispute.getId());

        return new DisputeDetailResponse(
                dispute.getTransactionReference(),
                party(tx.getSenderId(), tx.getCounterparty()),
                party(tx.getRecipientId(), tx.getCounterparty()),
                AdminFormat.amount(dispute.getAmount(), dispute.getCurrency().name()),
                dispute.getValidationsRequired(),
                done,
                dispute.getLastValidationNote());
    }

    /** A Dogaa account shows as its holder; the other side of an external movement as itself. */
    private String party(UUID userId, String counterparty) {
        if (userId == null) {
            return counterparty == null ? "Compte externe" : counterparty;
        }
        return userService.getById(userId).getFullName();
    }

    /** "Sena Amétépé" → "Sena A." — how the console names an approver. */
    private static String shortName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "un admin";
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts.length == 1 ? parts[0] : parts[0] + " " + parts[1].charAt(0) + ".";
    }

    private static String ordinal(long n) {
        return n == 1 ? "1re" : n + "e";
    }
}
