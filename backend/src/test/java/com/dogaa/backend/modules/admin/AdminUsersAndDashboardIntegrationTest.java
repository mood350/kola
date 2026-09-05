package com.dogaa.backend.modules.admin;

import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.KycTier;
import com.dogaa.backend.common.enums.UserStatus;
import com.dogaa.backend.common.enums.WalletType;
import com.dogaa.backend.modules.admin.entity.AdminAccount;
import com.dogaa.backend.modules.admin.entity.AdminRole;
import com.dogaa.backend.modules.admin.repository.AdminAccountRepository;
import com.dogaa.backend.modules.audit.repository.AuditLogRepository;
import com.dogaa.backend.modules.credit.repository.LoanRepository;
import com.dogaa.backend.modules.kyc.entity.KycDocument;
import com.dogaa.backend.modules.kyc.entity.KycDocumentType;
import com.dogaa.backend.modules.kyc.repository.KycDocumentRepository;
import com.dogaa.backend.modules.scoring.entity.CreditScore;
import com.dogaa.backend.modules.scoring.repository.CreditScoreRepository;
import com.dogaa.backend.modules.user.entity.User;
import com.dogaa.backend.modules.user.repository.UserRepository;
import com.dogaa.backend.modules.vault.dto.CreateVaultRequest;
import com.dogaa.backend.modules.vault.entity.Vault;
import com.dogaa.backend.modules.vault.entity.VaultStatus;
import com.dogaa.backend.modules.vault.repository.VaultRepository;
import com.dogaa.backend.modules.vault.service.VaultService;
import com.dogaa.backend.modules.wallet.entity.Wallet;
import com.dogaa.backend.modules.wallet.service.WalletService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUsersAndDashboardIntegrationTest {

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
    private VaultRepository vaultRepository;
    @Autowired
    private VaultService vaultService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private LoanRepository loanRepository;
    @Autowired
    private CreditScoreRepository creditScoreRepository;
    @Autowired
    private KycDocumentRepository kycDocumentRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID customerId;
    private String superAdminToken;
    private String creditAnalystToken;

    @BeforeEach
    void setUp() throws Exception {
        auditLogRepository.deleteAll();
        loanRepository.deleteAll();
        creditScoreRepository.deleteAll();
        kycDocumentRepository.deleteAll();
        vaultRepository.deleteAll();
        adminAccountRepository.deleteAll();
        userRepository.deleteAll();

        admin("sena@dogaa.io", "Sena Amétépé", AdminRole.SUPER_ADMIN);
        admin("aya@dogaa.io", "Aya Djobo", AdminRole.CREDIT_ANALYST);
        superAdminToken = login("sena@dogaa.io");
        creditAnalystToken = login("aya@dogaa.io");

        User customer = userRepository.save(User.builder()
                .firstName("Kossi").lastName("Adjo")
                .phone("+22890123456")
                .pinHash("x")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .phoneVerified(true)
                .kycTier(KycTier.TIER_1)
                // The tier is derived, not declared: without the profile fields that justify
                // TIER_1, any recomputation would legitimately drop this account to TIER_0.
                .address("Rue de la Paix").city("Lomé").country("TG")
                .privacyPolicyAcceptedAt(Instant.now())
                .build());
        customerId = customer.getId();
    }

    private void admin(String email, String name, AdminRole role) {
        adminAccountRepository.save(AdminAccount.builder()
                .email(email).name(name).role(role).scope("scope")
                .passwordHash(passwordEncoder.encode(PASSWORD)).enabled(true).build());
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("token").asString();
    }

    private JsonNode getJson(String path, String token) throws Exception {
        MvcResult result = mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    // --- users ------------------------------------------------------------

    @Test
    void theCustomerTableIsPreFormattedForTheFrontEnd() throws Exception {
        creditScoreRepository.save(CreditScore.builder()
                .userId(customerId).scoreValue(72).rawScoreValue(75)
                .kycTier(KycTier.TIER_1).windowDays(30).build());

        JsonNode users = getJson("/api/v1/admin/users", superAdminToken);

        assertThat(users).hasSize(1);
        JsonNode user = users.get(0);
        assertThat(user.path("name").asString()).isEqualTo("Kossi Adjo");
        assertThat(user.path("initials").asString()).isEqualTo("KA");
        assertThat(user.path("tier").asString()).isEqualTo("TIER_1");
        assertThat(user.path("score").asInt()).isEqualTo(72);
        assertThat(user.path("state").asString()).isEqualTo("Actif");
        // Ages and amounts arrive as strings: the front-end formats nothing itself.
        assertThat(user.path("age").asString()).isNotBlank();
        assertThat(user.path("loan").asString()).isEqualTo("Aucun");
        assertThat(user.path("vaults").asInt()).isZero();
    }

    @Test
    void aFrozenAccountReadsAsGeleAndCanBeUnblocked() throws Exception {
        User user = userRepository.findById(customerId).orElseThrow();
        user.setStatus(UserStatus.SUSPENDED);
        user.setFailedPinAttempts(5);
        user.setLockedUntil(Instant.now().plusSeconds(900));
        userRepository.save(user);

        assertThat(getJson("/api/v1/admin/users", superAdminToken).get(0).path("state").asString())
                .isEqualTo("Gelé");

        mockMvc.perform(post("/api/v1/admin/users/" + customerId + "/unblock")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("Actif"));

        User unblocked = userRepository.findById(customerId).orElseThrow();
        // The PIN lockout has to go too, or the account re-freezes on the next wrong attempt.
        assertThat(unblocked.getLockedUntil()).isNull();
        assertThat(unblocked.getFailedPinAttempts()).isZero();

        assertThat(auditLogRepository.search(null, "users", null, null))
                .singleElement()
                .satisfies(entry -> assertThat(entry.getDiff()).isEqualTo("Gelé → Actif"));
    }

    @Test
    void unblockingAnAlreadyActiveAccountIsRefused() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users/" + customerId + "/unblock")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isConflict());
    }

    @Test
    void forcingAVaultClosedReleasesTheMoneyAndIsRecorded() throws Exception {
        Wallet wallet = walletService.provision(customerId, Currency.XOF, WalletType.CURRENT);
        walletService.credit(wallet.getId(), new BigDecimal("100000"));
        Vault vault = vaultService.createVault(customerId, new CreateVaultRequest(
                "Réparation camion", Currency.XOF, new BigDecimal("50000"), null, null));
        vaultService.deposit(customerId, vault.getId(), new BigDecimal("30000"));

        assertThat(getJson("/api/v1/admin/users", superAdminToken).get(0).path("vaults").asInt())
                .isEqualTo(1);

        mockMvc.perform(post("/api/v1/admin/users/" + customerId + "/force-close-vault")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vaults").value(0));

        assertThat(vaultRepository.findById(vault.getId()).orElseThrow().getStatus())
                .isEqualTo(VaultStatus.CLOSED);
        assertThat(walletService.getById(wallet.getId()).getAvailableBalance())
                .isEqualByComparingTo("100000");
        assertThat(auditLogRepository.search(null, "users", null, null))
                .anyMatch(entry -> entry.getAction().contains("Fermeture forcée"));
    }

    @Test
    void closingAVaultOnSomeoneWhoHasNoneIsRefused() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users/" + customerId + "/force-close-vault")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isBadRequest());
    }

    // --- KYC queue --------------------------------------------------------

    @Test
    void theQueueSaysWhichTierApprovingWouldActuallyGrant() throws Exception {
        kycDocumentRepository.save(KycDocument.builder()
                .userId(customerId).type(KycDocumentType.NATIONAL_ID)
                .storageKey("key").contentType("image/jpeg").sizeBytes(10).build());

        JsonNode queue = getJson("/api/v1/admin/users/kyc-queue", superAdminToken);

        assertThat(queue).hasSize(1);
        assertThat(queue.get(0).path("name").asString()).isEqualTo("Kossi Adjo");
        assertThat(queue.get(0).path("fromTier").asString()).isEqualTo("TIER_1");
        // Asked of the tier rules, not assumed to be "the next one up".
        assertThat(queue.get(0).path("toTier").asString()).isEqualTo("TIER_2");
    }

    @Test
    void aSelfieAloneMovesNobodyAndTheQueueSaysSo() throws Exception {
        kycDocumentRepository.save(KycDocument.builder()
                .userId(customerId).type(KycDocumentType.SELFIE)
                .storageKey("key").contentType("image/jpeg").sizeBytes(10).build());

        JsonNode queue = getJson("/api/v1/admin/users/kyc-queue", superAdminToken);

        assertThat(queue.get(0).path("fromTier").asString()).isEqualTo("TIER_1");
        assertThat(queue.get(0).path("toTier").asString()).isEqualTo("TIER_1");
    }

    @Test
    void approvingFromTheQueueRaisesTheTierAndLeavesATrail() throws Exception {
        KycDocument document = kycDocumentRepository.save(KycDocument.builder()
                .userId(customerId).type(KycDocumentType.NATIONAL_ID)
                .storageKey("key").contentType("image/jpeg").sizeBytes(10).build());

        mockMvc.perform(post("/api/v1/admin/users/kyc-queue/" + document.getId() + "/approve")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(customerId).orElseThrow().getKycTier())
                .isEqualTo(KycTier.TIER_2);
        assertThat(getJson("/api/v1/admin/users/kyc-queue", superAdminToken)).isEmpty();
        assertThat(auditLogRepository.search("Sena", "users", null, null))
                .anyMatch(entry -> entry.getDiff() != null && entry.getDiff().contains("TIER_2"));
    }

    @Test
    void rejectingFromTheQueueLeavesTheTierAlone() throws Exception {
        KycDocument document = kycDocumentRepository.save(KycDocument.builder()
                .userId(customerId).type(KycDocumentType.NATIONAL_ID)
                .storageKey("key").contentType("image/jpeg").sizeBytes(10).build());

        mockMvc.perform(post("/api/v1/admin/users/kyc-queue/" + document.getId() + "/reject")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(customerId).orElseThrow().getKycTier())
                .isEqualTo(KycTier.TIER_1);
    }

    // --- permissions ------------------------------------------------------

    @Test
    void aCreditAnalystCannotReachTheCustomerTable() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + creditAnalystToken))
                .andExpect(status().isForbidden());
    }

    // --- dashboard --------------------------------------------------------

    @Test
    void theFiveMetricTilesComeInTheOrderTheFrontEndExpects() throws Exception {
        JsonNode metrics = getJson("/api/v1/admin/dashboard/metrics", superAdminToken);

        assertThat(metrics).hasSize(5);
        assertThat(metrics.get(0).path("label").asString()).isEqualTo("Volume 24 h");
        assertThat(metrics.get(1).path("label").asString()).isEqualTo("Solde global");
        assertThat(metrics.get(2).path("label").asString()).isEqualTo("Croissance utilisateurs");
        assertThat(metrics.get(3).path("label").asString()).isEqualTo("Encours de prêts");
        assertThat(metrics.get(4).path("label").asString()).isEqualTo("Taux de défaut");

        for (JsonNode metric : metrics) {
            assertThat(metric.path("value").asString()).isNotBlank();
            assertThat(metric.path("delta").asString()).isNotBlank();
        }
    }

    @Test
    void anEmptyDatabaseGivesZerosRatherThanInventedFigures() throws Exception {
        JsonNode metrics = getJson("/api/v1/admin/dashboard/metrics", superAdminToken);

        assertThat(metrics.get(3).path("value").asString()).contains("0");
        assertThat(metrics.get(4).path("value").asString()).isEqualTo("0,0 %");
    }

    @Test
    void theVolumeChartHasOneBarPerDayAndMarksToday() throws Exception {
        JsonNode fortnight = getJson("/api/v1/admin/dashboard/transaction-volume", superAdminToken);
        assertThat(fortnight).hasSize(14);
        assertThat(fortnight.get(13).path("last").asBoolean()).isTrue();
        assertThat(fortnight.get(0).path("last").asBoolean()).isFalse();

        assertThat(getJson("/api/v1/admin/dashboard/transaction-volume?period=30d", superAdminToken))
                .hasSize(30);
    }

    @Test
    void barHeightsStayWithinTheZeroToHundredRangeTheChartExpects() throws Exception {
        for (JsonNode point : getJson("/api/v1/admin/dashboard/transaction-volume", superAdminToken)) {
            assertThat(point.path("h").asInt()).isBetween(0, 100);
            assertThat(point.path("day").asString()).isNotBlank();
        }
    }

    @Test
    void noAlertsIsAValidAnswerWhenNothingIsWrong() throws Exception {
        assertThat(getJson("/api/v1/admin/dashboard/alerts", superAdminToken)).isEmpty();
    }

    @Test
    void theLoanBookSummaryIsSafeOnAnEmptyBook() throws Exception {
        JsonNode summary = getJson("/api/v1/admin/dashboard/loan-book-summary", superAdminToken);

        // No division by zero, no NaN, no "undefined" leaking into the UI.
        assertThat(summary.path("outstanding").asString()).isEqualTo("0 XOF");
        assertThat(summary.path("allocatedPct").asInt()).isZero();
        assertThat(summary.path("defaultRate").asString()).isEqualTo("0,0 %");
        assertThat(summary.path("activeUsersTotal").asString()).isEqualTo("1");
    }
}
