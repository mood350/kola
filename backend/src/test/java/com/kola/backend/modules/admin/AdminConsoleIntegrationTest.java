package com.kola.backend.modules.admin;

import com.kola.backend.common.enums.Currency;
import com.kola.backend.common.enums.KycTier;
import com.kola.backend.common.enums.TransactionStatus;
import com.kola.backend.common.enums.TransactionType;
import com.kola.backend.common.enums.UserStatus;
import com.kola.backend.common.enums.WalletType;
import com.kola.backend.modules.admin.entity.AdminAccount;
import com.kola.backend.modules.admin.entity.AdminRole;
import com.kola.backend.modules.admin.repository.AdminAccountRepository;
import com.kola.backend.modules.audit.repository.AuditLogRepository;
import com.kola.backend.modules.credit.repository.LoanRepository;
import com.kola.backend.modules.kyc.entity.KycDocument;
import com.kola.backend.modules.kyc.entity.KycDocumentStatus;
import com.kola.backend.modules.kyc.entity.KycDocumentType;
import com.kola.backend.modules.kyc.repository.KycDocumentRepository;
import com.kola.backend.modules.transaction.entity.Transaction;
import com.kola.backend.modules.transaction.repository.TransactionRepository;
import com.kola.backend.modules.user.entity.User;
import com.kola.backend.modules.user.repository.UserRepository;
import com.kola.backend.modules.vault.entity.Vault;
import com.kola.backend.modules.vault.repository.VaultRepository;
import com.kola.backend.modules.wallet.entity.Wallet;
import com.kola.backend.modules.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The console API (BACKEND.md §17): raw values, the searches a support agent actually types, the
 * PIN-lockout release, and the role matrix.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminConsoleIntegrationTest {

    private static final String PASSWORD = "SuperSecret2026!";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AdminAccountRepository adminAccountRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WalletService walletService;
    @Autowired
    private VaultRepository vaultRepository;
    @Autowired
    private LoanRepository loanRepository;
    @Autowired
    private KycDocumentRepository kycDocumentRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID customerId;
    private String reference;
    private String superAdminToken;
    private String creditAnalystToken;

    @BeforeEach
    void setUp() throws Exception {
        auditLogRepository.deleteAll();
        transactionRepository.deleteAll();
        loanRepository.deleteAll();
        kycDocumentRepository.deleteAll();
        vaultRepository.deleteAll();
        adminAccountRepository.deleteAll();
        userRepository.deleteAll();

        admin("sena@kola.io", AdminRole.SUPER_ADMIN);
        admin("aya@kola.io", AdminRole.CREDIT_ANALYST);
        superAdminToken = login("sena@kola.io");
        creditAnalystToken = login("aya@kola.io");

        User customer = userRepository.save(User.builder()
                .firstName("Kossi").lastName("Adjo")
                .phone("+22890123456")
                .pinHash("x")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .phoneVerified(true)
                .kycTier(KycTier.TIER_1)
                .address("Rue de la Paix").city("Lomé").country("TG")
                .privacyPolicyAcceptedAt(Instant.now())
                .build());
        customerId = customer.getId();

        Wallet wallet = walletService.provision(customerId, Currency.XOF, WalletType.CURRENT);
        walletService.credit(wallet.getId(), new BigDecimal("25000"));

        reference = "TXN-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        transactionRepository.save(Transaction.builder()
                .reference(reference)
                .type(TransactionType.CASH_IN)
                .status(TransactionStatus.COMPLETED)
                .currency(Currency.XOF)
                .amount(new BigDecimal("25000"))
                .recipientId(customerId)
                .destinationWalletId(wallet.getId())
                .build());
    }

    // --- raw values ---------------------------------------------------------

    @Test
    void overviewServesNumbersNotDisplayStrings() throws Exception {
        JsonNode overview = getJson("/api/v1/admin/console/overview", superAdminToken);

        assertThat(overview.path("users").asLong()).isEqualTo(1);
        assertThat(overview.path("transactions30d").asLong()).isEqualTo(1);
        assertThat(overview.path("volume30d").isNumber()).isTrue();
        assertThat(overview.path("volume30d").decimalValue()).isEqualByComparingTo("25000");

        // Fourteen days, every one present, today last and carrying today's deposit.
        JsonNode days = overview.path("volumeByDay");
        assertThat(days.size()).isEqualTo(14);
        assertThat(days.get(13).path("amount").decimalValue()).isEqualByComparingTo("25000");
        assertThat(days.get(0).path("amount").decimalValue()).isEqualByComparingTo("0");

        // Today against yesterday, side by side, for the dashboard's comparison.
        assertThat(overview.path("today").path("transactions").asLong()).isEqualTo(1);
        assertThat(overview.path("today").path("volume").decimalValue()).isEqualByComparingTo("25000");
        assertThat(overview.path("today").path("newUsers").asLong()).isEqualTo(1);
        assertThat(overview.path("yesterday").path("volume").decimalValue()).isEqualByComparingTo("0");
        // The deposit carried no fee and no repayment happened: both present, at zero.
        assertThat(overview.path("today").path("fees").decimalValue()).isEqualByComparingTo("0");
        assertThat(overview.path("today").path("repayments").decimalValue()).isEqualByComparingTo("0");
    }

    @Test
    void theDayCountsFeesEarnedAndRepaymentsReceived() throws Exception {
        transactionRepository.save(Transaction.builder()
                .reference("TXN-FEE" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .type(TransactionType.P2P_TRANSFER).status(TransactionStatus.COMPLETED)
                .currency(Currency.XOF).amount(new BigDecimal("10000")).fee(new BigDecimal("150"))
                .senderId(customerId).build());
        transactionRepository.save(Transaction.builder()
                .reference("TXN-REP" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .type(TransactionType.LOAN_REPAYMENT).status(TransactionStatus.COMPLETED)
                .currency(Currency.XOF).amount(new BigDecimal("5000"))
                .senderId(customerId).build());

        JsonNode today = getJson("/api/v1/admin/console/overview", superAdminToken).path("today");

        assertThat(today.path("fees").decimalValue()).isEqualByComparingTo("150");
        assertThat(today.path("repayments").decimalValue()).isEqualByComparingTo("5000");
    }

    @Test
    void userDetailShowsWhatTheAppShowsTheCustomer() throws Exception {
        JsonNode detail = getJson("/api/v1/admin/console/users/" + customerId, superAdminToken);

        assertThat(detail.path("profile").path("fullName").asString()).isEqualTo("Kossi Adjo");
        assertThat(detail.path("profile").path("kycTier").asString()).isEqualTo("TIER_1");
        assertThat(detail.path("wallets").get(0).path("available").decimalValue()).isEqualByComparingTo("25000");
        // The history is paginated separately, never embedded in the profile.
        assertThat(detail.has("transactions")).isFalse();

        JsonNode history = getJson("/api/v1/admin/console/transactions?userId=" + customerId + "&size=10",
                superAdminToken);
        assertThat(history.path("size").asInt()).isEqualTo(10);
        assertThat(history.path("items").get(0).path("reference").asString()).isEqualTo(reference);
        assertThat(history.path("items").get(0).path("recipientName").asString()).isEqualTo("Kossi Adjo");
        // Nothing secret leaves the server.
        assertThat(detail.toString()).doesNotContain("pinHash");
    }

    // --- searches -----------------------------------------------------------

    @Test
    void usersAreFoundByNameOrByPhone() throws Exception {
        assertThat(getJson("/api/v1/admin/console/users?q=adjo", superAdminToken).path("total").asLong())
                .isEqualTo(1);
        assertThat(getJson("/api/v1/admin/console/users?q=90123", superAdminToken).path("total").asLong())
                .isEqualTo(1);
        assertThat(getJson("/api/v1/admin/console/users?q=inconnu", superAdminToken).path("total").asLong())
                .isZero();
    }

    @Test
    void transactionsAreFoundByReferenceOrByLocalPhoneFormat() throws Exception {
        String byReference = "/api/v1/admin/console/transactions?q=" + reference.toLowerCase();
        assertThat(getJson(byReference, superAdminToken).path("total").asLong()).isEqualTo(1);

        // Typed the way a customer dictates it: local format, with spaces.
        String byPhone = "/api/v1/admin/console/transactions?q=90 12 34 56";
        assertThat(getJson(byPhone, superAdminToken).path("items").get(0).path("reference").asString())
                .isEqualTo(reference);
    }

    @Test
    void aPhoneThatMatchesNoAccountFindsNothingRatherThanEverything() throws Exception {
        JsonNode page = getJson("/api/v1/admin/console/transactions?q=99999999", superAdminToken);

        assertThat(page.path("total").asLong()).isZero();
    }

    @Test
    void theUserListShowsTheKycTierAndWhoIsWaitingForReview() throws Exception {
        kycDocumentRepository.save(KycDocument.builder()
                .userId(customerId).type(KycDocumentType.NATIONAL_ID)
                .storageKey("key").contentType("image/jpeg").sizeBytes(10).build());

        JsonNode row = getJson("/api/v1/admin/console/users", superAdminToken).path("items").get(0);
        assertThat(row.path("kycTier").asString()).isEqualTo("TIER_1");
        assertThat(row.path("pendingDocuments").asLong()).isEqualTo(1);

        assertThat(getJson("/api/v1/admin/console/users?kycPending=true", superAdminToken).path("total").asLong())
                .isEqualTo(1);
        assertThat(getJson("/api/v1/admin/console/users?kycTier=TIER_3", superAdminToken).path("total").asLong())
                .isZero();
    }

    @Test
    void periodsAreCalendarDaysWithBothBoundsIncluded() throws Exception {
        String today = LocalDate.now(ZoneOffset.UTC).toString();
        String yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1).toString();

        assertThat(getJson("/api/v1/admin/console/transactions?from=" + today + "&to=" + today, superAdminToken)
                .path("total").asLong()).isEqualTo(1);
        assertThat(getJson("/api/v1/admin/console/transactions?from=" + yesterday + "&to=" + yesterday, superAdminToken)
                .path("total").asLong()).isZero();

        mockMvc.perform(get("/api/v1/admin/console/transactions?from=" + today + "&to=" + yesterday)
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void theAccountingSummaryTotalsTheFilteredTransactions() throws Exception {
        JsonNode summary = getJson("/api/v1/admin/console/transactions/summary", superAdminToken);

        assertThat(summary.path("count").asLong()).isEqualTo(1);
        assertThat(summary.path("volume").decimalValue()).isEqualByComparingTo("25000");
        assertThat(summary.path("byType").get(0).path("type").asString()).isEqualTo("CASH_IN");
    }

    @Test
    void theCsvExportOpensInAFrenchExcel() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/admin/console/transactions/export?format=csv")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk()).andReturn();

        byte[] body = result.getResponse().getContentAsByteArray();
        String csv = new String(body, StandardCharsets.UTF_8);
        // UTF-8 byte-order mark, semicolons, French labels.
        assertThat(csv).startsWith("\uFEFFDate (UTC);Référence;Type");
        assertThat(csv).contains(reference + ";Dépôt;Réussie;25000;0;XOF");
        assertThat(result.getResponse().getHeader("Content-Disposition")).contains("transactions.csv");
    }

    @Test
    void theExcelExportIsARealWorkbookWithASummarySheet() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/admin/console/transactions/export?format=xlsx")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk()).andReturn();

        Map<String, String> parts = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(
                new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))) {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null; ) {
                parts.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        assertThat(parts).containsKeys("[Content_Types].xml", "xl/workbook.xml",
                "xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml");
        assertThat(parts.get("xl/workbook.xml")).contains("Transactions", "Synthèse");
        // The amount is a number cell, so a SUM works in Excel.
        assertThat(parts.get("xl/worksheets/sheet1.xml")).contains("<c s=\"3\"><v>25000");
    }

    @Test
    void loansCanBeFilteredByDisbursementDayAndTotalled() throws Exception {
        String today = LocalDate.now(ZoneOffset.UTC).toString();

        JsonNode summary = getJson("/api/v1/admin/console/loans/summary?from=" + today + "&to=" + today,
                creditAnalystToken);
        assertThat(summary.path("count").asLong()).isZero();
        assertThat(summary.path("principal").decimalValue()).isEqualByComparingTo("0");
    }

    // --- actions ------------------------------------------------------------

    @Test
    void aPinLockoutCanBeReleasedEvenThoughTheStatusIsStillActive() throws Exception {
        User customer = userRepository.findById(customerId).orElseThrow();
        customer.setLockedUntil(Instant.now().plus(Duration.ofMinutes(15)));
        userRepository.save(customer);

        MvcResult result = mockMvc.perform(post("/api/v1/admin/console/users/" + customerId + "/unblock")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk()).andReturn();

        JsonNode profile = objectMapper.readTree(result.getResponse().getContentAsString()).path("profile");
        assertThat(profile.path("locked").asBoolean()).isFalse();
        assertThat(profile.path("status").asString()).isEqualTo(UserStatus.ACTIVE.name());
    }

    @Test
    void aKycRejectionWithoutAReasonIsRefused() throws Exception {
        KycDocument document = kycDocumentRepository.save(KycDocument.builder()
                .userId(customerId).type(KycDocumentType.NATIONAL_ID)
                .storageKey("key").contentType("image/jpeg").sizeBytes(10).build());

        assertThat(getJson("/api/v1/admin/console/kyc", superAdminToken).size()).isEqualTo(1);

        mockMvc.perform(post("/api/v1/admin/console/kyc/" + document.getId() + "/reject")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void kycDecisionsAreListedByStatusNewestDecisionFirst() throws Exception {
        Instant now = Instant.now();
        KycDocument older = decided(KycDocumentStatus.APPROVED, null, now.minus(Duration.ofDays(2)));
        KycDocument newer = decided(KycDocumentStatus.APPROVED, null, now.minus(Duration.ofDays(1)));
        decided(KycDocumentStatus.REJECTED, "Photo floue : merci de la reprendre.", now);
        kycDocumentRepository.save(KycDocument.builder().userId(customerId).type(KycDocumentType.PASSPORT)
                .storageKey("k").contentType("image/jpeg").sizeBytes(10).build()); // en attente : jamais dans l'historique

        JsonNode approved = getJson("/api/v1/admin/console/kyc/history?status=APPROVED", superAdminToken);
        assertThat(approved.path("total").asLong()).isEqualTo(2);
        // The most recent decision first, with the day it was taken.
        assertThat(approved.path("items").get(0).path("id").asString()).isEqualTo(newer.getId().toString());
        assertThat(approved.path("items").get(0).path("reviewedAt").isString()).isTrue();
        assertThat(approved.path("items").get(0).path("userName").asString()).isEqualTo("Kossi Adjo");

        JsonNode oldestFirst = getJson("/api/v1/admin/console/kyc/history?status=APPROVED&order=asc", superAdminToken);
        assertThat(oldestFirst.path("items").get(0).path("id").asString()).isEqualTo(older.getId().toString());

        // The reason for a refusal travels with the document.
        JsonNode rejected = getJson("/api/v1/admin/console/kyc/history?status=REJECTED", superAdminToken);
        assertThat(rejected.path("total").asLong()).isEqualTo(1);
        assertThat(rejected.path("items").get(0).path("rejectionReason").asString()).contains("Photo floue");

        // Paginated: one document per page, two pages.
        JsonNode firstPage = getJson("/api/v1/admin/console/kyc/history?status=APPROVED&size=1", superAdminToken);
        assertThat(firstPage.path("items").size()).isEqualTo(1);
        assertThat(firstPage.path("totalPages").asInt()).isEqualTo(2);
    }

    @Test
    void kycHistoryRefusesAnUnknownStatusOrOrder() throws Exception {
        mockMvc.perform(get("/api/v1/admin/console/kyc/history?status=NOPE")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/admin/console/kyc/history?status=APPROVED&order=sideways")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void kycHistoryIsBehindTheUsersModule() throws Exception {
        // 403, never 401: a credit analyst is signed in, they just may not read customer documents.
        mockMvc.perform(get("/api/v1/admin/console/kyc/history?status=APPROVED")
                        .header("Authorization", "Bearer " + creditAnalystToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/console/kyc/history?status=APPROVED"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void savingsListsWhoeverHoldsSomethingBiggestFirstAndTotalsThem() throws Exception {
        // Kossi: 20 000 on the savings account and a vault of 30 000 -> 50 000.
        Wallet kossiSavings = walletService.provision(customerId, Currency.XOF, WalletType.SAVINGS);
        walletService.credit(kossiSavings.getId(), new BigDecimal("20000"));
        vaultRepository.save(Vault.builder().ownerId(customerId)
                .walletId(walletService.getWallet(customerId, Currency.XOF).getId())
                .name("Vacances").currency(Currency.XOF).balance(new BigDecimal("30000")).build());

        // Awa: 100 000 on the savings account, 40 000 of it locked behind a loan -> 100 000.
        UUID awaId = customer("Awa", "Diallo", "+22891234567");
        Wallet awaSavings = walletService.provision(awaId, Currency.XOF, WalletType.SAVINGS);
        walletService.credit(awaSavings.getId(), new BigDecimal("100000"));
        walletService.lock(awaSavings.getId(), new BigDecimal("40000"));

        // Yao opened the account at sign-up and put nothing in: not a saver.
        UUID yaoId = customer("Yao", "Mensah", "+22892345678");
        walletService.provision(yaoId, Currency.XOF, WalletType.SAVINGS);

        JsonNode list = getJson("/api/v1/admin/console/savings", superAdminToken);
        assertThat(list.path("total").asLong()).isEqualTo(2);
        assertThat(list.path("items").get(0).path("fullName").asString()).isEqualTo("Awa Diallo");
        assertThat(list.path("items").get(0).path("total").decimalValue()).isEqualByComparingTo("100000");
        assertThat(list.path("items").get(0).path("collateral").decimalValue()).isEqualByComparingTo("40000");
        JsonNode kossi = list.path("items").get(1);
        assertThat(kossi.path("savings").decimalValue()).isEqualByComparingTo("20000");
        assertThat(kossi.path("vaults").asLong()).isEqualTo(1);
        assertThat(kossi.path("vaultsBalance").decimalValue()).isEqualByComparingTo("30000");
        assertThat(kossi.path("total").decimalValue()).isEqualByComparingTo("50000");

        // Found by name or by phone number, whatever the case.
        assertThat(getJson("/api/v1/admin/console/savings?q=KOSSI", superAdminToken).path("total").asLong()).isEqualTo(1);
        assertThat(getJson("/api/v1/admin/console/savings?q=91234", superAdminToken).path("items").get(0)
                .path("fullName").asString()).isEqualTo("Awa Diallo");

        JsonNode summary = getJson("/api/v1/admin/console/savings/summary", superAdminToken);
        assertThat(summary.path("savers").asLong()).isEqualTo(2);
        assertThat(summary.path("total").decimalValue()).isEqualByComparingTo("150000");
        assertThat(summary.path("savings").decimalValue()).isEqualByComparingTo("120000");
        assertThat(summary.path("collateral").decimalValue()).isEqualByComparingTo("40000");
        assertThat(summary.path("vaults").asLong()).isEqualTo(1);
        assertThat(summary.path("vaultsBalance").decimalValue()).isEqualByComparingTo("30000");
    }

    @Test
    void savingsAnEmptyBaseAnswersZerosAndIsBehindTheUsersModule() throws Exception {
        JsonNode summary = getJson("/api/v1/admin/console/savings/summary", superAdminToken);
        assertThat(summary.path("savers").asLong()).isZero();
        assertThat(summary.path("total").decimalValue()).isEqualByComparingTo("0");
        assertThat(getJson("/api/v1/admin/console/savings", superAdminToken).path("items").size()).isZero();

        // 403, never 401: a credit analyst is signed in, they just may not read customer savings.
        mockMvc.perform(get("/api/v1/admin/console/savings").header("Authorization", "Bearer " + creditAnalystToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/console/savings/summary")).andExpect(status().isUnauthorized());
    }

    @Test
    void ladderShowsAMissingCeilingAsSuchAndRefusesAZeroOne() throws Exception {
        String url = "/api/v1/admin/credit/tier-config";
        JsonNode original = getJson(url, superAdminToken);
        try {
            // Round trip with no ceiling: what the screen shows is what it can send back, nothing is zeroed.
            String body = "{\"tiers\":[{\"name\":\"TIER 1\",\"minScore\":60,\"maxAmount\":\"Aucun plafond\",\"monthlyRate\":\"8 %/mois\"},"
                    + "{\"name\":\"TIER 2\",\"minScore\":60,\"maxAmount\":\"150 000 XOF\",\"monthlyRate\":\"7 %/mois\"},"
                    + "{\"name\":\"TIER 3\",\"minScore\":75,\"maxAmount\":\"300000\",\"monthlyRate\":\"6,5 %/mois\"},"
                    + "{\"name\":\"TIER 4\",\"minScore\":85,\"maxAmount\":\"Aucun plafond\",\"monthlyRate\":\"6 %/mois\"}]}";
            mockMvc.perform(put(url).header("Authorization", "Bearer " + superAdminToken)
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
            JsonNode saved = getJson(url, superAdminToken);
            assertThat(saved.get(0).path("maxAmount").asString()).isEqualTo("Aucun plafond");
            assertThat(saved.get(1).path("maxAmount").asString()).contains("150");
            assertThat(saved.get(3).path("maxAmount").asString()).isEqualTo("Aucun plafond");

            // A ceiling of 0 would cap every loan at 0: refused, whatever way it is written.
            mockMvc.perform(put(url).header("Authorization", "Bearer " + superAdminToken)
                    .contentType(MediaType.APPLICATION_JSON).content(body.replace("150 000 XOF", "0 XOF")))
                    .andExpect(status().isBadRequest());
        } finally {
            // The ladder is shared by the whole context: put back what the other tests expect.
            mockMvc.perform(put(url).header("Authorization", "Bearer " + superAdminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"tiers\":" + original.toString() + "}")).andExpect(status().isOk());
        }
    }

    // --- role matrix ----------------------------------------------------------

    @Test
    void aCreditAnalystSeesLoansButNotCustomers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/console/loans")
                        .header("Authorization", "Bearer " + creditAnalystToken))
                .andExpect(status().isOk());

        // 403, never 401: the console would otherwise sign a legitimate analyst out.
        mockMvc.perform(get("/api/v1/admin/console/users")
                        .header("Authorization", "Bearer " + creditAnalystToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/console/transactions")
                        .header("Authorization", "Bearer " + creditAnalystToken))
                .andExpect(status().isForbidden());
    }

    // --- helpers --------------------------------------------------------------

    private UUID customer(String firstName, String lastName, String phone) {
        return userRepository.save(User.builder()
                .firstName(firstName).lastName(lastName).phone(phone).pinHash("x")
                .dateOfBirth(LocalDate.of(1992, 3, 4)).phoneVerified(true).kycTier(KycTier.TIER_1)
                .address("Rue de la Paix").city("Lomé").country("TG")
                .privacyPolicyAcceptedAt(Instant.now()).build()).getId();
    }

    private KycDocument decided(KycDocumentStatus status, String reason, Instant at) {
        return kycDocumentRepository.save(KycDocument.builder()
                .userId(customerId).type(KycDocumentType.NATIONAL_ID)
                .storageKey("key-" + UUID.randomUUID()).contentType("image/jpeg").sizeBytes(10)
                .status(status).rejectionReason(reason).reviewedAt(at).build());
    }

    private void admin(String email, AdminRole role) {
        adminAccountRepository.save(AdminAccount.builder()
                .email(email).name(email).role(role).scope("scope")
                .passwordHash(passwordEncoder.encode(PASSWORD)).enabled(true).build());
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("token").asString();
    }

    private JsonNode getJson(String path, String token) throws Exception {
        MvcResult result = mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
