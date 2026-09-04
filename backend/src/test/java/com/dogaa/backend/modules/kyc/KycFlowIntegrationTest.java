package com.dogaa.backend.modules.kyc;

import com.dogaa.backend.common.enums.KycTier;
import com.dogaa.backend.common.enums.Role;
import com.dogaa.backend.modules.auth.repository.OtpCodeRepository;
import com.dogaa.backend.modules.kyc.repository.KycDocumentRepository;
import com.dogaa.backend.modules.notification.service.EmailOtpSender;
import com.dogaa.backend.modules.notification.service.OtpSender;
import com.dogaa.backend.modules.user.entity.User;
import com.dogaa.backend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KycFlowIntegrationTest {

    private static final String PHONE = "90123456";
    private static final String E164 = "+22890123456";
    private static final String EMAIL = "kossi.adjo@example.com";
    private static final String PIN = "8305";

    static class Recorder {
        final Map<String, String> codes = new ConcurrentHashMap<>();
    }

    static class RecordingOtpSender implements OtpSender {
        private final Recorder recorder;

        RecordingOtpSender(Recorder recorder) {
            this.recorder = recorder;
        }

        @Override
        public void sendOtp(String phone, String code, Duration ttl) {
            recorder.codes.put(phone, code);
        }
    }

    static class RecordingEmailOtpSender implements EmailOtpSender {
        private final Recorder recorder;

        RecordingEmailOtpSender(Recorder recorder) {
            this.recorder = recorder;
        }

        @Override
        public void sendOtp(String email, String code, Duration ttl) {
            recorder.codes.put(email, code);
        }
    }

    @TestConfiguration
    static class SenderTestConfig {

        @Bean
        Recorder recorder() {
            return new Recorder();
        }

        @Bean
        @Primary
        OtpSender testOtpSender(Recorder recorder) {
            return new RecordingOtpSender(recorder);
        }

        @Bean
        @Primary
        EmailOtpSender testEmailOtpSender(Recorder recorder) {
            return new RecordingEmailOtpSender(recorder);
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OtpCodeRepository otpCodeRepository;
    @Autowired
    private KycDocumentRepository kycDocumentRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private Recorder recorder;

    @BeforeEach
    void cleanUp() {
        kycDocumentRepository.deleteAll();
        otpCodeRepository.deleteAll();
        userRepository.deleteAll();
        recorder.codes.clear();
    }

    // ------------------------------------------------------------ the whole climb

    @Test
    void aUserClimbsFromTier0ToTier2AndIsDemotedWhenAnApprovalIsRevoked() throws Exception {
        String userToken = registerAndLogin();

        // Tier 0: phone only, and the ceilings named in the spec.
        JsonNode status = kycStatus(userToken);
        assertThat(status.path("tier").asString()).isEqualTo("TIER_0");
        assertThat(status.path("limits").path("daily").asString()).isEqualTo("50000");
        assertThat(status.path("limits").path("creditEligible").asBoolean()).isFalse();
        assertThat(status.path("requirementsForNextTier").get(0).asString())
                .contains("profile");

        // Tier 1: the declarative step, filled in from the profile endpoint.
        completeProfile(userToken);
        assertThat(kycStatus(userToken).path("tier").asString()).isEqualTo("TIER_1");

        // Tier 2: an identity document, once a reviewer approves it.
        String documentId = submitDocument(userToken, "NATIONAL_ID").path("id").asString();
        assertThat(kycStatus(userToken).path("tier").asString())
                .as("a pending document must not promote anyone")
                .isEqualTo("TIER_1");

        String adminToken = createAdminAndLogin();
        approve(adminToken, documentId);

        JsonNode afterApproval = kycStatus(userToken);
        assertThat(afterApproval.path("tier").asString()).isEqualTo("TIER_2");
        assertThat(afterApproval.path("limits").path("creditEligible").asBoolean()).isTrue();
        assertThat(afterApproval.path("limits").path("daily").asString()).isEqualTo("3000000");

        // Revoking the approval demotes on its own, because the tier is derived and not stored
        // as a decision.
        mockMvc.perform(post("/api/v1/admin/kyc/documents/" + documentId + "/revoke")
                        .param("reason", "Document turned out to be forged")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertThat(kycStatus(userToken).path("tier").asString()).isEqualTo("TIER_1");
    }

    @Test
    void aRejectedDocumentLeavesTheTierAloneAndTellsTheUserWhy() throws Exception {
        String userToken = registerAndLogin();
        completeProfile(userToken);

        String documentId = submitDocument(userToken, "NATIONAL_ID").path("id").asString();
        String adminToken = createAdminAndLogin();

        mockMvc.perform(post("/api/v1/admin/kyc/documents/" + documentId + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("approved", false, "rejectionReason", "The photo is unreadable")))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectionReason").value("The photo is unreadable"));

        assertThat(kycStatus(userToken).path("tier").asString()).isEqualTo("TIER_1");
    }

    @Test
    void aRejectionMustStateAReason() throws Exception {
        String userToken = registerAndLogin();
        completeProfile(userToken);
        String documentId = submitDocument(userToken, "NATIONAL_ID").path("id").asString();

        mockMvc.perform(post("/api/v1/admin/kyc/documents/" + documentId + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("approved", false)))
                        .header("Authorization", "Bearer " + createAdminAndLogin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    // ------------------------------------------------------------------- guardrails

    @Test
    void anOrdinaryUserCannotReachTheReviewQueue() throws Exception {
        String userToken = registerAndLogin();

        mockMvc.perform(get("/api/v1/admin/kyc/documents/pending")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void verifyingTheEmailGrantsNoTierBecauseEmailIsOptional() throws Exception {
        String userToken = registerAndLogin();

        mockMvc.perform(post("/api/v1/auth/email/request-code")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("code", recorder.codes.get(EMAIL))))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        assertThat(userRepository.findByPhone(E164).orElseThrow().isEmailVerified()).isTrue();
        assertThat(kycStatus(userToken).path("tier").asString()).isEqualTo("TIER_0");
        assertThat(userRepository.findByPhone(E164).orElseThrow().getKycTier())
                .isEqualTo(KycTier.TIER_0);
    }

    @Test
    void anAccountWithNoEmailAtAllStillReachesTheCreditTier() throws Exception {
        String userToken = registerAndLogin();
        completeProfile(userToken);

        String documentId = submitDocument(userToken, "NATIONAL_ID").path("id").asString();
        approve(createAdminAndLogin(), documentId);

        JsonNode status = kycStatus(userToken);
        assertThat(status.path("tier").asString()).isEqualTo("TIER_2");
        assertThat(status.path("limits").path("creditEligible").asBoolean()).isTrue();
    }

    @Test
    void completingTheProfileFromTheUserEndpointMovesTheTier() throws Exception {
        String userToken = registerAndLogin();
        assertThat(kycStatus(userToken).path("tier").asString()).isEqualTo("TIER_0");

        completeProfile(userToken);

        // The promotion is driven by an event from the user module, not by a KYC call.
        assertThat(userRepository.findByPhone(E164).orElseThrow().getKycTier())
                .isEqualTo(KycTier.TIER_1);
    }

    @Test
    void anExecutableIsRefusedAsADocument() throws Exception {
        String userToken = registerAndLogin();

        MockMultipartFile file = new MockMultipartFile(
                "file", "payload.exe", "application/x-msdownload", "MZ".getBytes());

        mockMvc.perform(multipart("/api/v1/kyc/documents")
                        .file(file)
                        .param("type", "NATIONAL_ID")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        assertThat(kycDocumentRepository.count()).isZero();
    }

    @Test
    void theStorageKeyIsNeverExposedToTheClient() throws Exception {
        String userToken = registerAndLogin();
        MvcResult result = mockMvc.perform(multipart("/api/v1/kyc/documents")
                        .file(idCardFile())
                        .param("type", "NATIONAL_ID")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isCreated())
                .andReturn();

        String storageKey = kycDocumentRepository.findAll().get(0).getStorageKey();
        assertThat(result.getResponse().getContentAsString()).doesNotContain(storageKey);
    }

    @Test
    void resubmittingAPendingDocumentReplacesItRatherThanQueueingASecond() throws Exception {
        String userToken = registerAndLogin();

        submitDocument(userToken, "NATIONAL_ID");
        submitDocument(userToken, "NATIONAL_ID");

        assertThat(kycDocumentRepository.count()).isEqualTo(1);
    }

    // ------------------------------------------------------------------------ helpers

    private String registerAndLogin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("phone", PHONE))))
                .andExpect(status().isOk());

        String verificationToken = data(mockMvc.perform(post("/api/v1/auth/register/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("phone", PHONE, "code", recorder.codes.get(E164)))))
                .andExpect(status().isOk())
                .andReturn()).path("verificationToken").asString();

        return data(mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "verificationToken", verificationToken,
                                "firstName", "Kossi",
                                "lastName", "Adjo",
                                "email", EMAIL,
                                "dateOfBirth", "1995-04-12",
                                "pin", PIN,
                                "confirmPin", PIN,
                                "country", "TG"))))
                .andExpect(status().isCreated())
                .andReturn()).path("accessToken").asString();
    }

    /** Creates an administrator straight in the database: there is no endpoint that grants the role. */
    private String createAdminAndLogin() throws Exception {
        String adminPhone = "+22891000000";
        userRepository.save(User.builder()
                .firstName("Ama")
                .lastName("Reviewer")
                .phone(adminPhone)
                .pinHash(passwordEncoder.encode("4971"))
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .phoneVerified(true)
                .role(Role.ADMIN)
                .build());

        return data(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("phone", adminPhone, "pin", "4971"))))
                .andExpect(status().isOk())
                .andReturn()).path("accessToken").asString();
    }

    /** Fills in the declarative fields that TIER_1 asks for. */
    private void completeProfile(String userToken) throws Exception {
        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "address", "Rue de la Paix",
                                "city", "Lome",
                                "country", "TG")))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    private JsonNode submitDocument(String userToken, String type) throws Exception {
        return data(mockMvc.perform(multipart("/api/v1/kyc/documents")
                        .file(idCardFile())
                        .param("type", type)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private void approve(String adminToken, String documentId) throws Exception {
        mockMvc.perform(post("/api/v1/admin/kyc/documents/" + documentId + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("approved", true)))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    private JsonNode kycStatus(String userToken) throws Exception {
        return data(mockMvc.perform(get("/api/v1/kyc/status")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn());
    }

    private static MockMultipartFile idCardFile() {
        return new MockMultipartFile("file", "id-card.jpg", "image/jpeg",
                "not a real photograph".getBytes());
    }

    private JsonNode data(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }
}
