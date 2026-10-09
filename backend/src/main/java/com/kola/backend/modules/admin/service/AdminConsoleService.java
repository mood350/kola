package com.kola.backend.modules.admin.service;

import com.kola.backend.common.enums.KycTier;
import com.kola.backend.common.enums.LoanStatus;
import com.kola.backend.common.enums.TransactionStatus;
import com.kola.backend.common.enums.TransactionType;
import com.kola.backend.common.enums.UserStatus;
import com.kola.backend.common.util.PhoneNumbers;
import com.kola.backend.config.AuthProperties;
import com.kola.backend.exception.BadRequestException;
import com.kola.backend.modules.admin.dto.console.ConsoleDailyVolume;
import com.kola.backend.modules.admin.dto.console.ConsoleDayStats;
import com.kola.backend.modules.admin.dto.console.ConsoleKycDocument;
import com.kola.backend.modules.admin.dto.console.ConsoleLoan;
import com.kola.backend.modules.admin.dto.console.ConsoleLoanSummary;
import com.kola.backend.modules.admin.dto.console.ConsoleOverview;
import com.kola.backend.modules.admin.dto.console.ConsolePage;
import com.kola.backend.modules.admin.dto.console.ConsoleScheduledTask;
import com.kola.backend.modules.admin.dto.console.ConsoleTransaction;
import com.kola.backend.modules.admin.dto.console.ConsoleTransactionSummary;
import com.kola.backend.modules.admin.dto.console.ConsoleTypeTotal;
import com.kola.backend.modules.admin.dto.console.ConsoleUserDetail;
import com.kola.backend.modules.admin.dto.console.ConsoleUserProfile;
import com.kola.backend.modules.admin.dto.console.ConsoleUserRow;
import com.kola.backend.modules.admin.dto.console.ConsoleVault;
import com.kola.backend.modules.admin.dto.console.ConsoleWallet;
import com.kola.backend.modules.admin.security.CurrentAdmin;
import com.kola.backend.modules.credit.entity.Loan;
import com.kola.backend.modules.credit.repository.LoanRepository;
import com.kola.backend.modules.kyc.entity.KycDocument;
import com.kola.backend.modules.kyc.entity.KycDocumentStatus;
import com.kola.backend.modules.kyc.repository.KycDocumentRepository;
import com.kola.backend.modules.scheduling.service.ScheduledTaskService;
import com.kola.backend.modules.transaction.entity.Transaction;
import com.kola.backend.modules.transaction.repository.TransactionRepository;
import com.kola.backend.modules.transaction.repository.TransactionSpecifications;
import com.kola.backend.modules.user.entity.User;
import com.kola.backend.modules.user.repository.UserRepository;
import com.kola.backend.modules.user.service.UserService;
import com.kola.backend.modules.vault.repository.VaultRepository;
import com.kola.backend.modules.wallet.service.WalletService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The back-office console: a read layer over the customer-facing modules, limited to what the
 * mobile app itself offers (accounts, KYC, wallets, vaults, transfers and payments, scheduled
 * payments, credit).
 *
 * <p>Unlike the older {@code /admin/**} endpoints, which ship display strings ("709 k XOF",
 * "1 prêts en cours"), everything here is raw — amounts as numbers, instants as ISO-8601, states as
 * enum names — so the console can sort, filter and format them. Actions go through the service that
 * owns the rule ({@link AdminUserService}), which also writes the audit line.
 *
 * <p>Periods are calendar days in UTC, which is Lomé time: {@code from} and {@code to} are both
 * inclusive, so "du 1er au 1er" is one day.
 */
@Service
@RequiredArgsConstructor
public class AdminConsoleService {

    private static final int MAX_PAGE_SIZE = 100;
    /** An export is one download, not a data dump: past this, narrow the period. */
    static final int MAX_EXPORT_ROWS = 50_000;
    private static final Duration RECENT = Duration.ofDays(30);
    private static final int CHART_DAYS = 14;
    private static final List<LoanStatus> OUTSTANDING = List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE);
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    private final UserRepository userRepository;
    private final UserService userService;
    private final WalletService walletService;
    private final VaultRepository vaultRepository;
    private final LoanRepository loanRepository;
    private final ScheduledTaskService scheduledTaskService;
    private final TransactionRepository transactionRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final AdminUserService adminUserService;
    private final AuthProperties authProperties;
    private final EntityManager entityManager;

    /** Filters shared by the transaction list, its summary and its export. */
    public record TransactionFilter(String query,
                                    UUID userId,
                                    TransactionType type,
                                    TransactionStatus status,
                                    LocalDate from,
                                    LocalDate to) {
    }

    // --- overview ---------------------------------------------------------

    @Transactional(readOnly = true)
    public ConsoleOverview overview() {
        Instant since = Instant.now().minus(RECENT);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant todayStart = startOf(today);
        Instant yesterdayStart = startOf(today.minusDays(1));

        return new ConsoleOverview(
                userRepository.count(),
                userRepository.countByCreatedAtAfter(since),
                userRepository.countByStatus(UserStatus.SUSPENDED),
                kycDocumentRepository.countByStatus(KycDocumentStatus.PENDING),
                transactionRepository.countByStatusAndCreatedAtGreaterThanEqual(TransactionStatus.COMPLETED, since),
                transactionRepository.sumAmountSince(TransactionStatus.COMPLETED, since),
                loanRepository.countByStatus(LoanStatus.ACTIVE),
                loanRepository.countByStatus(LoanStatus.OVERDUE),
                loanRepository.sumOutstanding(OUTSTANDING),
                dayStats(todayStart, Instant.now()),
                dayStats(yesterdayStart, todayStart),
                volumeByDay(today));
    }

    private ConsoleDayStats dayStats(Instant from, Instant to) {
        ConsoleLoanSummary loans = loanSummary(null, from, to);
        return new ConsoleDayStats(
                transactionRepository.countByStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        TransactionStatus.COMPLETED, from, to),
                transactionRepository.sumAmountBetween(TransactionStatus.COMPLETED, from, to),
                userRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to),
                loans.count(),
                loans.principal(),
                transactionRepository.sumFeeBetween(TransactionStatus.COMPLETED, from, to),
                transactionRepository.sumAmountOfTypeBetween(TransactionStatus.COMPLETED,
                        TransactionType.LOAN_REPAYMENT, from, to));
    }

    /**
     * Completed volume per UTC day over the last {@link #CHART_DAYS} days, today included. Every
     * day is present so the chart keeps its width on a quiet week.
     */
    private List<ConsoleDailyVolume> volumeByDay(LocalDate today) {
        Instant since = startOf(today.minusDays(CHART_DAYS - 1L));

        Map<LocalDate, BigDecimal> perDay = new LinkedHashMap<>();
        for (int i = CHART_DAYS - 1; i >= 0; i--) {
            perDay.put(today.minusDays(i), BigDecimal.ZERO);
        }
        for (Object[] row : transactionRepository.completedAmountsSince(TransactionStatus.COMPLETED, since)) {
            LocalDate day = LocalDate.ofInstant((Instant) row[0], ZoneOffset.UTC);
            perDay.computeIfPresent(day, (key, total) -> total.add((BigDecimal) row[1]));
        }
        return perDay.entrySet().stream()
                .map(e -> new ConsoleDailyVolume(e.getKey(), e.getValue()))
                .toList();
    }

    // --- users ------------------------------------------------------------

    /**
     * Customers, newest first.
     *
     * @param query       name or phone, case-insensitive substring; blank matches everyone
     * @param kycTier     only this tier, or {@code null}
     * @param kycPending  only customers with a document waiting for review
     * @param from        registered on or after this day
     * @param to          registered on or before this day
     */
    @Transactional(readOnly = true)
    public ConsolePage<ConsoleUserRow> users(String query, KycTier kycTier, boolean kycPending,
                                             LocalDate from, LocalDate to, int page, int size) {
        checkPeriod(from, to);
        String needle = query == null ? "" : query.strip().toLowerCase();

        Specification<User> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!needle.isEmpty()) {
                String pattern = "%" + needle + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.concat(cb.concat(root.get("firstName"), " "), root.get("lastName"))), pattern),
                        cb.like(root.get("phone"), pattern)));
            }
            if (kycTier != null) {
                predicates.add(cb.equal(root.get("kycTier"), kycTier));
            }
            if (kycPending) {
                Subquery<UUID> pending = cq.subquery(UUID.class);
                Root<KycDocument> doc = pending.from(KycDocument.class);
                pending.select(doc.get("userId"))
                        .where(cb.equal(doc.get("status"), KycDocumentStatus.PENDING));
                predicates.add(root.get("id").in(pending));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startOf(from)));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), startOf(to.plusDays(1))));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<User> users = userRepository.findAll(spec, pageable(page, size, NEWEST_FIRST));
        Map<UUID, Long> pending = pendingDocuments(users.getContent().stream().map(User::getId).toList());
        return ConsolePage.of(users, users.getContent().stream()
                .map(u -> toRow(u, pending.getOrDefault(u.getId(), 0L)))
                .toList());
    }

    private Map<UUID, Long> pendingDocuments(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return kycDocumentRepository.countByUserIds(KycDocumentStatus.PENDING, userIds).stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
    }

    @Transactional(readOnly = true)
    public ConsoleUserDetail user(UUID userId) {
        User user = userService.getById(userId);

        return new ConsoleUserDetail(
                toProfile(user),
                walletService.listWallets(userId).stream()
                        .map(w -> new ConsoleWallet(w.getType(), w.getCurrency(),
                                w.getAvailableBalance(), w.getLockedBalance()))
                        .toList(),
                vaultRepository.findByOwnerIdOrderByCreatedAtDesc(userId).stream()
                        .map(v -> new ConsoleVault(v.getId(), v.getName(), v.getCurrency(), v.getBalance(),
                                v.getTargetAmount(), v.getTargetDate(), v.getStatus()))
                        .toList(),
                loanRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                        .map(l -> toLoan(l, user))
                        .toList(),
                scheduledTaskService.listByUser(userId).stream()
                        .map(t -> new ConsoleScheduledTask(t.id(), t.type(), t.frequency(), t.amount(),
                                t.currency(), t.description(), t.status(), t.nextRunAt(), t.lastFailureReason()))
                        .toList(),
                kycDocumentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                        .map(d -> toDocument(d, user))
                        .toList());
    }

    /** Lifts a suspension or a PIN lockout. The rule and the audit line live in {@link AdminUserService}. */
    public ConsoleUserDetail unblock(CurrentAdmin admin, UUID userId) {
        adminUserService.unblock(admin, userId);
        return user(userId);
    }

    // --- KYC --------------------------------------------------------------

    /** Documents waiting for a decision, oldest first — the order they should be handled in. */
    @Transactional(readOnly = true)
    public List<ConsoleKycDocument> kycQueue() {
        List<KycDocument> pending = kycDocumentRepository.findByStatusOrderByCreatedAtAsc(KycDocumentStatus.PENDING);
        Map<UUID, User> owners = usersById(pending.stream().map(KycDocument::getUserId).collect(Collectors.toSet()));
        return pending.stream()
                .map(d -> toDocument(d, owners.get(d.getUserId())))
                .toList();
    }

    public void approveDocument(CurrentAdmin admin, UUID documentId) {
        adminUserService.approveKyc(admin, documentId);
    }

    public void rejectDocument(CurrentAdmin admin, UUID documentId, String reason) {
        adminUserService.rejectKyc(admin, documentId, reason);
    }

    // --- loans ------------------------------------------------------------

    /** Loans newest first; {@code from}/{@code to} bound the disbursement day, both inclusive. */
    @Transactional(readOnly = true)
    public ConsolePage<ConsoleLoan> loans(LoanStatus status, LocalDate from, LocalDate to, int page, int size) {
        checkPeriod(from, to);
        Page<Loan> loans = loanRepository.findAll(
                loanSpec(status, from == null ? null : startOf(from), to == null ? null : startOf(to.plusDays(1))),
                pageable(page, size, NEWEST_FIRST));
        Map<UUID, User> borrowers = usersById(loans.getContent().stream().map(Loan::getUserId)
                .collect(Collectors.toSet()));
        return ConsolePage.of(loans, loans.getContent().stream()
                .map(l -> toLoan(l, borrowers.get(l.getUserId())))
                .toList());
    }

    @Transactional(readOnly = true)
    public ConsoleLoanSummary loanSummary(LoanStatus status, LocalDate from, LocalDate to) {
        checkPeriod(from, to);
        return loanSummary(status, from == null ? null : startOf(from), to == null ? null : startOf(to.plusDays(1)));
    }

    private ConsoleLoanSummary loanSummary(LoanStatus status, Instant from, Instant to) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Loan> root = query.from(Loan.class);
        Predicate where = loanSpec(status, from, to).toPredicate(root, query, cb);

        query.multiselect(
                        cb.count(root),
                        cb.coalesce(cb.sum(root.get("principal")), BigDecimal.ZERO),
                        cb.coalesce(cb.sum(cb.<BigDecimal>selectCase()
                                .when(root.get("status").in(OUTSTANDING),
                                        cb.diff(cb.sum(cb.sum(root.get("principal"), root.get("interestAmount")),
                                                root.get("penaltyAmount")), root.get("amountRepaid")))
                                .otherwise(BigDecimal.ZERO)), BigDecimal.ZERO),
                        cb.sum(cb.<Integer>selectCase()
                                .when(cb.equal(root.get("status"), LoanStatus.OVERDUE), 1).otherwise(0)))
                .where(where);

        Object[] row = entityManager.createQuery(query).getSingleResult();
        return new ConsoleLoanSummary(
                ((Number) row[0]).longValue(),
                (BigDecimal) row[1],
                ((BigDecimal) row[2]).max(BigDecimal.ZERO),
                row[3] == null ? 0 : ((Number) row[3]).longValue());
    }

    private static Specification<Loan> loanSpec(LoanStatus status, Instant from, Instant to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("disbursedAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("disbursedAt"), to));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    // --- transactions -----------------------------------------------------

    /**
     * Ledger search, newest first.
     *
     * <p>{@code query} is read as a phone number when it holds only digits, spaces and a leading
     * {@code +} — normalised the same way sign-up normalises it, so "90 12 34 56" and
     * "+22890123456" find the same account — and as a transaction reference otherwise.
     */
    @Transactional(readOnly = true)
    public ConsolePage<ConsoleTransaction> transactions(TransactionFilter filter, int page, int size) {
        Page<Transaction> result = transactionRepository.findAll(transactionSpec(filter),
                pageable(page, size, NEWEST_FIRST));
        Map<UUID, User> names = usersById(participants(result.getContent()));
        return ConsolePage.of(result, result.getContent().stream()
                .map(t -> toTransaction(t, names))
                .toList());
    }

    /** The accounting strip: count, volume and fees of everything matching, broken down by type. */
    @Transactional(readOnly = true)
    public ConsoleTransactionSummary transactionSummary(TransactionFilter filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<Transaction> root = query.from(Transaction.class);
        Predicate where = transactionSpec(filter).toPredicate(root, query, cb);

        query.multiselect(root.get("type"), cb.count(root),
                        cb.coalesce(cb.sum(root.get("amount")), BigDecimal.ZERO),
                        cb.coalesce(cb.sum(root.get("fee")), BigDecimal.ZERO))
                .where(where)
                .groupBy(root.get("type"));

        List<ConsoleTypeTotal> byType = entityManager.createQuery(query).getResultList().stream()
                .map(r -> new ConsoleTypeTotal((TransactionType) r[0], ((Number) r[1]).longValue(),
                        (BigDecimal) r[2], (BigDecimal) r[3]))
                .sorted((a, b) -> b.amount().compareTo(a.amount()))
                .toList();

        return new ConsoleTransactionSummary(
                byType.stream().mapToLong(ConsoleTypeTotal::count).sum(),
                byType.stream().map(ConsoleTypeTotal::amount).reduce(BigDecimal.ZERO, BigDecimal::add),
                byType.stream().map(ConsoleTypeTotal::fees).reduce(BigDecimal.ZERO, BigDecimal::add),
                byType);
    }

    /** A spreadsheet of the matching transactions plus a per-type summary, for the accountant. */
    @Transactional(readOnly = true)
    public byte[] exportTransactions(TransactionFilter filter, boolean xlsx) {
        long matching = transactionRepository.count(transactionSpec(filter));
        if (matching > MAX_EXPORT_ROWS) {
            throw new BadRequestException("L'export est limité à " + MAX_EXPORT_ROWS
                    + " lignes (" + matching + " demandées) : réduisez la période.");
        }

        List<Transaction> rows = transactionRepository.findAll(transactionSpec(filter), NEWEST_FIRST);
        Map<UUID, User> names = usersById(participants(rows));

        SpreadsheetWriter.Sheet detail = new SpreadsheetWriter.Sheet("Transactions",
                List.of("Date (UTC)", "Référence", "Type", "Statut", "Montant", "Frais", "Devise",
                        "Émetteur", "Destinataire", "Contrepartie externe", "Description", "Motif d'échec"),
                rows.stream().map(t -> {
                    User sender = t.getSenderId() == null ? null : names.get(t.getSenderId());
                    User recipient = t.getRecipientId() == null ? null : names.get(t.getRecipientId());
                    List<Object> cells = new ArrayList<>();
                    cells.add(t.getCreatedAt());
                    cells.add(t.getReference());
                    cells.add(ExportLabels.type(t.getType()));
                    cells.add(ExportLabels.status(t.getStatus()));
                    cells.add(t.getAmount());
                    cells.add(t.getFee());
                    cells.add(t.getCurrency().name());
                    cells.add(sender == null ? null : sender.getFullName() + " (" + sender.getPhone() + ")");
                    cells.add(recipient == null ? null : recipient.getFullName() + " (" + recipient.getPhone() + ")");
                    cells.add(ExportLabels.counterparty(t.getCounterpartyName(), t.getCounterparty()));
                    cells.add(t.getDescription());
                    cells.add(t.getFailureReason());
                    return cells;
                }).toList());

        if (!xlsx) {
            return SpreadsheetWriter.csv(detail);
        }

        ConsoleTransactionSummary summary = transactionSummary(filter);
        List<List<Object>> summaryRows = new ArrayList<>();
        for (ConsoleTypeTotal total : summary.byType()) {
            summaryRows.add(List.of(ExportLabels.type(total.type()), total.count(), total.amount(), total.fees()));
        }
        summaryRows.add(List.of("Total", summary.count(), summary.volume(), summary.fees()));
        SpreadsheetWriter.Sheet synthesis = new SpreadsheetWriter.Sheet("Synthèse",
                List.of("Type", "Nombre", "Montant", "Frais perçus"), summaryRows);

        return SpreadsheetWriter.xlsx(List.of(detail, synthesis));
    }

    private Specification<Transaction> transactionSpec(TransactionFilter filter) {
        checkPeriod(filter.from(), filter.to());
        String reference = null;
        Collection<UUID> participants = filter.userId() == null ? null : List.of(filter.userId());

        String q = filter.query() == null ? "" : filter.query().strip();
        if (!q.isEmpty()) {
            if (q.matches("\\+?[0-9 .-]+")) {
                Collection<UUID> byPhone = participantsFor(q);
                participants = participants == null
                        ? byPhone
                        : participants.stream().filter(byPhone::contains).toList();
            } else {
                reference = q;
            }
        }

        return TransactionSpecifications.forAdmin(reference, participants, filter.type(), filter.status(),
                filter.from() == null ? null : startOf(filter.from()),
                filter.to() == null ? null : startOf(filter.to().plusDays(1)));
    }

    private Collection<UUID> participantsFor(String rawPhone) {
        try {
            String phone = PhoneNumbers.normalize(rawPhone, authProperties.getDefaultCallingCode());
            return userRepository.findByPhone(phone).map(u -> List.of(u.getId())).orElse(List.of());
        } catch (IllegalArgumentException unreadable) {
            return List.of();
        }
    }

    // --- mapping ----------------------------------------------------------

    private static Instant startOf(LocalDate day) {
        return day.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static void checkPeriod(LocalDate from, LocalDate to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new BadRequestException("La date de fin précède la date de début.");
        }
    }

    private static Pageable pageable(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), sort);
    }

    private Map<UUID, User> usersById(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private static Set<UUID> participants(List<Transaction> transactions) {
        return transactions.stream()
                .flatMap(t -> Stream.of(t.getSenderId(), t.getRecipientId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private ConsoleUserRow toRow(User user, long pendingDocuments) {
        return new ConsoleUserRow(user.getId(), user.getFullName(), user.getPhone(), user.getKycTier(),
                user.getStatus(), user.isLocked(), pendingDocuments, user.getCreatedAt(), user.getLastLoginAt());
    }

    private ConsoleUserProfile toProfile(User user) {
        return new ConsoleUserProfile(user.getId(), user.getFullName(), user.getPhone(), user.getEmail(),
                user.getCity(), user.getCountry(), user.getKycTier(), user.getStatus(), user.isLocked(),
                user.getLockedUntil(), user.getCreatedAt(), user.getLastLoginAt());
    }

    private static ConsoleLoan toLoan(Loan loan, User borrower) {
        BigDecimal owed = loan.getPrincipal()
                .add(loan.getInterestAmount())
                .add(loan.getPenaltyAmount())
                .subtract(loan.getAmountRepaid());
        boolean settled = loan.getStatus() == LoanStatus.REPAID;
        return new ConsoleLoan(loan.getId(), loan.getUserId(), borrower == null ? null : borrower.getFullName(),
                loan.getCurrency(), loan.getPrincipal(), loan.getInterestAmount(), loan.getPenaltyAmount(),
                loan.getAmountRepaid(), settled ? BigDecimal.ZERO : owed.max(BigDecimal.ZERO),
                loan.getStatus(), loan.getDisbursedAt(), loan.getDueAt(), loan.getSettledAt());
    }

    private static ConsoleTransaction toTransaction(Transaction tx, Map<UUID, User> users) {
        User sender = tx.getSenderId() == null ? null : users.get(tx.getSenderId());
        User recipient = tx.getRecipientId() == null ? null : users.get(tx.getRecipientId());
        return new ConsoleTransaction(tx.getReference(), tx.getType(), tx.getStatus(), tx.getCurrency(),
                tx.getAmount(), tx.getFee(),
                tx.getSenderId(), sender == null ? null : sender.getFullName(),
                tx.getRecipientId(), recipient == null ? null : recipient.getFullName(),
                tx.getCounterparty(), tx.getCounterpartyName(), tx.getDescription(), tx.getFailureReason(),
                tx.getCreatedAt());
    }

    private static ConsoleKycDocument toDocument(KycDocument document, User owner) {
        return new ConsoleKycDocument(document.getId(), document.getUserId(),
                owner == null ? null : owner.getFullName(),
                owner == null ? null : owner.getPhone(),
                document.getType(), document.getStatus(),
                owner == null ? null : owner.getKycTier(),
                document.getOriginalFilename(), document.getContentType(),
                document.getCreatedAt(), document.getRejectionReason());
    }
}
