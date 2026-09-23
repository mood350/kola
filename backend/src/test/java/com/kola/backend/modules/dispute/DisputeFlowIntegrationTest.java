package com.kola.backend.modules.dispute;

import com.kola.backend.common.enums.Currency;
import com.kola.backend.common.enums.KycTier;
import com.kola.backend.common.enums.WalletType;
import com.kola.backend.modules.admin.entity.AdminAccount;
import com.kola.backend.modules.admin.entity.AdminRole;
import com.kola.backend.modules.admin.repository.AdminAccountRepository;
import com.kola.backend.modules.audit.repository.AuditLogRepository;
import com.kola.backend.modules.dispute.entity.DisputeTag;
import com.kola.backend.modules.dispute.repository.DisputeRepository;
import com.kola.backend.modules.dispute.repository.DisputeValidationRepository;
import com.kola.backend.modules.transaction.dto.TransferRequest;
import com.kola.backend.modules.transaction.entity.Transaction;
import com.kola.backend.modules.transaction.repository.TransactionRepository;
import com.kola.backend.modules.transaction.service.TransactionService;
import com.kola.backend.modules.user.entity.User;
import com.kola.backend.modules.user.repository.UserRepository;
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
class DisputeFlowIntegrationTest {

    private static final String PASSWORD = "SuperSecret2026!";
    private static final BigDecimal AMOUNT = new BigDecimal("50000");

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
    private TransactionService transactionService;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private DisputeRepository disputeRepository;
    @Autowired
    private DisputeValidationRepository validationRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID victimId;
    private Wallet victimWallet;
    private Wallet beneficiaryWallet;
    private String reference;
    private String firstAdminToken;
    private String secondAdminToken;

    @BeforeEach
    void setUp() throws Exception {
        auditLogRepository.deleteAll();
        validationRepository.deleteAll();
        disputeRepository.deleteAll();
        transactionRepository.deleteAll();
        adminAccountRepository.deleteAll();
        userRepository.deleteAll();

        admin("sena@kola.io", "Sena Amétépé");
        admin("koffi@kola.io", "Koffi Messan");
        firstAdminToken = login("sena@kola.io");
        secondAdminToken = login("koffi@kola.io");

        victimId = customer("Kossi", "Adjo", "+22890123456").getId();
        UUID beneficiaryId = customer("Ama", "Sossou", "+22891234567").getId();

        victimWallet = walletService.provision(victimId, Currency.XOF, WalletType.CURRENT);
        beneficiaryWallet = walletService.provision(beneficiaryId, Currency.XOF, WalletType.CURRENT);
        walletService.credit(victimWallet.getId(), new BigDecimal("200000"));

        Transaction transfer = transactionService.transfer(victimId,
                new TransferRequest(Currency.XOF, AMOUNT, "+22891234567", "Paiement contesté"));
        reference = transfer.getReference();
    }

    private User customer(String first, String last, String phone) {
        return userRepository.save(User.builder()
                .firstName(first).lastName(last).phone(phone).pinHash("x")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .phoneVerified(true).kycTier(KycTier.TIER_2).city("Lomé")
                .privacyPolicyAcceptedAt(Instant.now())
                .build());
    }

    private void admin(String email, String name) {
        adminAccountRepository.save(AdminAccount.builder()
                .email(email).name(name).role(AdminRole.SUPER_ADMIN).scope("scope")
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

    private void openDispute() throws Exception {
        // The customer contests through their own API; the back-office never invents disputes.
        mockMvc.perform(post("/api/v1/disputes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "transactionReference", reference,
                                "tag", "fraud",
                                "title", "Transfert non autorisé")))
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isCreated());
    }

    /** The victim signs in the ordinary way to contest their transaction. */
    private String customerToken() {
        return jwtFor(victimId);
    }

    @Autowired
    private com.kola.backend.modules.auth.security.JwtService jwtService;

    private String jwtFor(UUID userId) {
        return jwtService.generateAccessToken(userRepository.findById(userId).orElseThrow());
    }

    private JsonNode detail(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/admin/disputes/" + reference)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private void proposeChargeback() throws Exception {
        mockMvc.perform(post("/api/v1/admin/disputes/" + reference + "/chargeback")
                        .header("Authorization", "Bearer " + firstAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("chargeback_pending"));
    }

    // --- opening ----------------------------------------------------------

    @Test
    void aCustomerOpensADisputeAndTheBackOfficeSeesIt() throws Exception {
        openDispute();

        MvcResult result = mockMvc.perform(get("/api/v1/admin/disputes")
                        .header("Authorization", "Bearer " + firstAdminToken))
                .andExpect(status().isOk()).andReturn();
        JsonNode disputes = objectMapper.readTree(result.getResponse().getContentAsString());

        assertThat(disputes).hasSize(1);
        assertThat(disputes.get(0).path("ref").asString()).isEqualTo(reference);
        assertThat(disputes.get(0).path("tag").asString()).isEqualTo("fraud");
        assertThat(disputes.get(0).path("tagLabel").asString()).isEqualTo("Fraude");
        assertThat(disputes.get(0).path("status").asString()).isEqualTo("open");
        // meta is assembled server-side and displayed verbatim.
        assertThat(disputes.get(0).path("meta").asString()).contains("TIER_2").contains("Lomé");
    }

    @Test
    void theSameTransactionCannotBeContestedTwice() throws Exception {
        openDispute();

        mockMvc.perform(post("/api/v1/disputes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "transactionReference", reference,
                                "tag", "double_debit",
                                "title", "Encore")))
                        .header("Authorization", "Bearer " + customerToken()))
                .andExpect(status().isConflict());
    }

    // --- the four-eyes rule -----------------------------------------------

    /** The rule the whole module exists for: one administrator cannot sign twice. */
    @Test
    void theSameAdministratorCannotValidateTwice() throws Exception {
        openDispute();
        proposeChargeback();

        mockMvc.perform(post("/api/v1/admin/disputes/" + reference + "/validate")
                        .header("Authorization", "Bearer " + firstAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validationsDone").value(1));

        mockMvc.perform(post("/api/v1/admin/disputes/" + reference + "/validate")
                        .header("Authorization", "Bearer " + firstAdminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("autre administrateur")));

        assertThat(validationRepository.count()).isEqualTo(1);
        // Nothing moved: one signature is not enough.
        assertThat(walletService.getById(beneficiaryWallet.getId()).getAvailableBalance())
                .isEqualByComparingTo(AMOUNT);
    }

    @Test
    void oneValidationLeavesTheMoneyWhereItIsAndSaysWhoIsMissing() throws Exception {
        openDispute();
        proposeChargeback();

        mockMvc.perform(post("/api/v1/admin/disputes/" + reference + "/validate")
                        .header("Authorization", "Bearer " + firstAdminToken))
                .andExpect(status().isOk());

        JsonNode detail = detail(firstAdminToken);
        assertThat(detail.path("validationsRequired").asInt()).isEqualTo(2);
        assertThat(detail.path("validationsDone").asInt()).isEqualTo(1);
        assertThat(detail.path("lastValidationNote").asString())
                .contains("Sena Amétépé")
                .contains("en attente");
    }

    @Test
    void twoDistinctAdministratorsExecuteTheChargeback() throws Exception {
        openDispute();
        proposeChargeback();

        BigDecimal victimBefore = walletService.getById(victimWallet.getId()).getAvailableBalance();

        mockMvc.perform(post("/api/v1/admin/disputes/" + reference + "/validate")
                        .header("Authorization", "Bearer " + firstAdminToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/disputes/" + reference + "/validate")
                        .header("Authorization", "Bearer " + secondAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validationsDone").value(2));

        // The money went back.
        assertThat(walletService.getById(victimWallet.getId()).getAvailableBalance())
                .isEqualByComparingTo(victimBefore.add(AMOUNT));
        assertThat(walletService.getById(beneficiaryWallet.getId()).getAvailableBalance())
                .isEqualByComparingTo("0");

        assertThat(disputeRepository.findByTransactionReference(reference).orElseThrow()
                .getStatus().getCode()).isEqualTo("resolved");
        assertThat(detail(firstAdminToken).path("lastValidationNote").asString())
                .contains("Sena Amétépé").contains("Koffi Messan");
    }

    /**
     * The beneficiary spent the money before the decision. The victim is still made whole, and the
     * gap is recorded rather than quietly absorbed.
     */
    @Test
    void aSpentBeneficiaryLeavesAShortfallButTheVictimIsStillRefunded() throws Exception {
        openDispute();
        walletService.debit(beneficiaryWallet.getId(), new BigDecimal("40000"));
        proposeChargeback();

        BigDecimal victimBefore = walletService.getById(victimWallet.getId()).getAvailableBalance();

        mockMvc.perform(post("/api/v1/admin/disputes/" + reference + "/validate")
                        .header("Authorization", "Bearer " + firstAdminToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/disputes/" + reference + "/validate")
                        .header("Authorization", "Bearer " + secondAdminToken))
                .andExpect(status().isOk());

        assertThat(walletService.getById(victimWallet.getId()).getAvailableBalance())
                .isEqualByComparingTo(victimBefore.add(AMOUNT));
        assertThat(disputeRepository.findByTransactionReference(reference).orElseThrow()
                .getShortfallAmount()).isEqualByComparingTo("40000");
        assertThat(detail(firstAdminToken).path("lastValidationNote").asString())
                .contains("non récupérés");
    }

    // --- guards -----------------------------------------------------------

    @Test
    void validatingBeforeAChargebackIsProposedIsRefused() throws Exception {
        openDispute();

        mockMvc.perform(post("/api/v1/admin/disputes/" + reference + "/validate")
                        .header("Authorization", "Bearer " + firstAdminToken))
                .andExpect(status().isConflict());
        assertThat(validationRepository.count()).isZero();
    }

    @Test
    void aRejectedDisputeMovesNoMoneyAndCannotBeValidated() throws Exception {
        openDispute();

        mockMvc.perform(post("/api/v1/admin/disputes/" + reference + "/reject")
                        .header("Authorization", "Bearer " + firstAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("rejected"));

        mockMvc.perform(post("/api/v1/admin/disputes/" + reference + "/validate")
                        .header("Authorization", "Bearer " + firstAdminToken))
                .andExpect(status().isConflict());
        assertThat(walletService.getById(beneficiaryWallet.getId()).getAvailableBalance())
                .isEqualByComparingTo(AMOUNT);
    }

    @Test
    void everyDecisionLandsInTheAuditTrailWithItsAuthor() throws Exception {
        openDispute();
        proposeChargeback();
        mockMvc.perform(post("/api/v1/admin/disputes/" + reference + "/validate")
                .header("Authorization", "Bearer " + firstAdminToken));
        mockMvc.perform(post("/api/v1/admin/disputes/" + reference + "/validate")
                .header("Authorization", "Bearer " + secondAdminToken));

        var entries = auditLogRepository.search(null, "disputes", null, null);
        assertThat(entries).hasSizeGreaterThanOrEqualTo(4);
        assertThat(entries).anyMatch(e -> e.getAction().contains("Chargeback proposé"));
        assertThat(entries).anyMatch(e -> e.getAction().contains("Chargeback exécuté"));
        assertThat(entries).anyMatch(e -> e.getActorName().equals("Koffi Messan"));
    }
}
