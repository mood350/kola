package com.dogaa.backend.modules.admin.service;

import com.dogaa.backend.common.util.BackOfficeFormat;
import com.dogaa.backend.common.util.RelativeTime;
import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.KycTier;
import com.dogaa.backend.common.enums.LoanStatus;
import com.dogaa.backend.common.enums.UserStatus;
import com.dogaa.backend.exception.BadRequestException;
import com.dogaa.backend.exception.ConflictException;
import com.dogaa.backend.modules.admin.dto.ClientUserResponse;
import com.dogaa.backend.modules.admin.dto.KycSubmissionResponse;
import com.dogaa.backend.modules.admin.security.CurrentAdmin;
import com.dogaa.backend.modules.audit.service.AuditService;
import com.dogaa.backend.modules.credit.entity.Loan;
import com.dogaa.backend.modules.credit.repository.LoanRepository;
import com.dogaa.backend.modules.kyc.dto.ReviewDocumentRequest;
import com.dogaa.backend.modules.kyc.entity.KycDocument;
import com.dogaa.backend.modules.kyc.entity.KycDocumentStatus;
import com.dogaa.backend.modules.kyc.entity.KycDocumentType;
import com.dogaa.backend.modules.kyc.repository.KycDocumentRepository;
import com.dogaa.backend.modules.kyc.service.KycService;
import com.dogaa.backend.modules.kyc.service.KycTierRules;
import com.dogaa.backend.modules.scoring.repository.CreditScoreRepository;
import com.dogaa.backend.modules.user.entity.User;
import com.dogaa.backend.modules.user.repository.UserRepository;
import com.dogaa.backend.modules.user.service.UserService;
import com.dogaa.backend.modules.vault.entity.Vault;
import com.dogaa.backend.modules.vault.entity.VaultStatus;
import com.dogaa.backend.modules.vault.repository.VaultRepository;
import com.dogaa.backend.modules.vault.service.VaultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Customer accounts as the back-office sees them (BACKEND.md 6).
 *
 * <p>A read-and-act layer over the domain modules: it owns no data, and every decision it takes —
 * unblocking, closing a vault, approving a document — goes through the service that owns the rule,
 * then lands in the audit trail. Nothing here writes to another module's tables.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final String MODULE = "users";
    private static final List<LoanStatus> OUTSTANDING =
            List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE);


    private final UserRepository userRepository;
    private final UserService userService;
    private final VaultRepository vaultRepository;
    private final VaultService vaultService;
    private final LoanRepository loanRepository;
    private final CreditScoreRepository creditScoreRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final KycService kycService;
    private final AuditService auditService;

    // --- listing ----------------------------------------------------------

    /**
     * The whole customer table. The front-end filters and searches client-side, so no server
     * paging is offered yet — a deliberate match to the current contract, and the first thing to
     * revisit when the user base grows.
     */
    @Transactional(readOnly = true)
    public List<ClientUserResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(this::toClientUser)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClientUserResponse getUser(UUID userId) {
        return toClientUser(userService.getById(userId));
    }

    // --- actions ----------------------------------------------------------

    @Transactional
    public ClientUserResponse unblock(CurrentAdmin admin, UUID userId) {
        User user = userService.getById(userId);
        UserStatus previous = user.getStatus();

        if (previous == UserStatus.ACTIVE) {
            throw new ConflictException("Ce compte est déjà actif");
        }

        user.setStatus(UserStatus.ACTIVE);
        // A blocked account is often one that ran out of PIN attempts; leaving the lock behind
        // would let it re-lock on the next try.
        user.setFailedPinAttempts(0);
        user.setLockedUntil(null);
        userService.save(user);

        auditService.record(admin, MODULE, "Déblocage du compte de " + user.getFullName(),
                AuditService.diff(state(previous), state(UserStatus.ACTIVE)),
                "User", userId.toString());

        log.info("Admin {} unblocked user {}", admin.email(), userId);
        return toClientUser(user);
    }

    /**
     * Closes a vault by force (DOGAA.md 4.5), releasing its balance back to the wallet.
     *
     * <p>The contract sends no vault id, so the oldest open one is closed. That is a guess the
     * back-office should not have to make: when the screen offers a vault list, this endpoint
     * should take an explicit id.
     */
    @Transactional
    public ClientUserResponse forceCloseVault(CurrentAdmin admin, UUID userId) {
        User user = userService.getById(userId);

        Vault vault = vaultRepository
                .findFirstByOwnerIdAndStatusOrderByCreatedAtAsc(userId, VaultStatus.ACTIVE)
                .orElseThrow(() -> new BadRequestException("Cet utilisateur n'a aucun coffre ouvert"));

        vaultService.forceClose(vault.getId());

        auditService.record(admin, MODULE,
                "Fermeture forcée du coffre « " + vault.getName() + " » de " + user.getFullName(),
                AuditService.diff("ACTIVE", "CLOSED"), "Vault", vault.getId().toString());

        log.warn("Admin {} force-closed vault {} of user {}", admin.email(), vault.getId(), userId);
        return toClientUser(user);
    }

    // --- KYC queue --------------------------------------------------------

    @Transactional(readOnly = true)
    public List<KycSubmissionResponse> kycQueue() {
        return kycDocumentRepository.findByStatusOrderByCreatedAtAsc(KycDocumentStatus.PENDING)
                .stream()
                .map(this::toSubmission)
                .toList();
    }

    @Transactional
    public void approveKyc(CurrentAdmin admin, UUID documentId) {
        kycService.review(documentId, admin, new ReviewDocumentRequest(true, null));
    }

    /**
     * @param reason shown to the customer, so it has to say what was wrong with the document —
     *               the review service refuses a blank one
     */
    @Transactional
    public void rejectKyc(CurrentAdmin admin, UUID documentId, String reason) {
        kycService.review(documentId, admin, new ReviewDocumentRequest(false, reason));
    }

    // --- mapping ----------------------------------------------------------

    private ClientUserResponse toClientUser(User user) {
        int score = creditScoreRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())
                .map(entry -> entry.getScoreValue())
                .orElse(0);

        long vaults = vaultRepository.countByOwnerIdAndStatus(user.getId(), VaultStatus.ACTIVE);

        String loan = loanRepository.findFirstByUserIdAndStatusIn(user.getId(), OUTSTANDING)
                .map(AdminUserService::describeLoan)
                .orElse("Aucun");

        return new ClientUserResponse(
                user.getId().toString(),
                initials(user),
                user.getFullName(),
                user.getPhone(),
                user.getKycTier().name(),
                score,
                BackOfficeFormat.age(user.getCreatedAt()),
                state(user.getStatus()),
                vaults,
                loan);
    }

    private KycSubmissionResponse toSubmission(KycDocument document) {
        User owner = userService.getById(document.getUserId());
        KycTier current = owner.getKycTier();

        return new KycSubmissionResponse(
                document.getId().toString(),
                owner.getFullName(),
                current.name(),
                tierIfApproved(owner, document).name(),
                RelativeTime.since(document.getCreatedAt()),
                document.getType().label(),
                document.getContentType(),
                document.getOriginalFilename());
    }

    /**
     * What approving this document would actually grant — asked of the tier rules rather than
     * assumed to be "the next one up". A selfie on its own moves nobody, and the queue should say so.
     */
    private KycTier tierIfApproved(User owner, KycDocument document) {
        Set<KycDocumentType> approved = kycDocumentRepository
                .findByUserIdAndStatus(owner.getId(), KycDocumentStatus.APPROVED).stream()
                .map(KycDocument::getType)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(KycDocumentType.class)));
        approved.add(document.getType());

        return KycTierRules.resolve(owner, approved);
    }

    private static String describeLoan(Loan loan) {
        return BackOfficeFormat.amount(loan.getOutstanding(), currencyOf(loan));
    }

    private static String currencyOf(Loan loan) {
        return loan.getCurrency() == null ? Currency.XOF.name() : loan.getCurrency().name();
    }

    /**
     * The admin UI styles three states. "Litige" will come from the disputes module once it
     * exists; until then a closed account reads as frozen rather than inventing a fourth value the
     * front-end has no style for.
     */
    private static String state(UserStatus status) {
        return switch (status) {
            case ACTIVE -> "Actif";
            case SUSPENDED, CLOSED -> "Gelé";
        };
    }

    private static String initials(User user) {
        String first = user.getFirstName();
        String last = user.getLastName();
        char a = first == null || first.isBlank() ? '?' : first.charAt(0);
        char b = last == null || last.isBlank() ? '?' : last.charAt(0);
        return ("" + a + b).toUpperCase();
    }
}
