package com.dogaa.backend.modules.auth;

import com.dogaa.backend.modules.auth.repository.OtpCodeRepository;
import com.dogaa.backend.modules.notification.service.OtpSender;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    private static final String PHONE = "90123456";
    private static final String E164 = "+22890123456";
    private static final String PIN = "8305";

    /** Captures the code instead of sending an SMS, so the test can play the user's part. */
    static class RecordingOtpSender implements OtpSender {

        private final Map<String, String> codes = new ConcurrentHashMap<>();

        @Override
        public void sendOtp(String phone, String code, Duration ttl) {
            codes.put(phone, code);
        }

        String lastCodeFor(String phone) {
            return codes.get(phone);
        }
    }

    @TestConfiguration
    static class OtpTestConfig {

        @Bean
        @Primary
        RecordingOtpSender recordingOtpSender() {
            return new RecordingOtpSender();
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
    private RecordingOtpSender otpSender;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
        otpCodeRepository.deleteAll();
    }

    // ---------------------------------------------------------------- the happy path

    @Test
    void registerInThreeStepsThenLogInAndReadOwnProfile() throws Exception {
        String verificationToken = requestAndVerifyOtp(PHONE);

        JsonNode registered = register(verificationToken, PIN, status().isCreated());
        assertThat(registered.path("user").path("phone").asString()).isEqualTo(E164);
        assertThat(registered.path("user").path("phoneVerified").asBoolean()).isTrue();
        assertThat(registered.path("user").path("kycTier").asString()).isEqualTo("TIER_0");

        String accessToken = login(PHONE, PIN, status().isOk()).path("accessToken").asString();

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value(E164))
                .andExpect(jsonPath("$.data.firstName").value("Kossi"));
    }

    @Test
    void theCodeIsNeverReturnedByTheApi() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("phone", PHONE))))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain(otpSender.lastCodeFor(E164));
        // The number comes back masked as well.
        assertThat(body).doesNotContain(E164);
    }

    // ------------------------------------------------- no OTP means no account, ever

    @Test
    void registrationIsRefusedWithoutAVerificationToken() throws Exception {
        Map<String, Object> payload = new HashMap<>(registrationPayload("", PIN));
        payload.remove("verificationToken");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void registrationIsRefusedWithAForgedVerificationToken() throws Exception {
        register("not-a-real-token", PIN, status().isBadRequest());
        assertThat(userRepository.count()).isZero();
    }

    @Test
    void requestingACodeDoesNotByItselfAllowRegistration() throws Exception {
        // Step 1 done, step 2 skipped: the code was never entered.
        requestOtp(PHONE);

        register(otpSender.lastCodeFor(E164), PIN, status().isBadRequest());
        assertThat(userRepository.count()).isZero();
    }

    @Test
    void aVerificationTokenCannotBeUsedTwice() throws Exception {
        String verificationToken = requestAndVerifyOtp(PHONE);
        register(verificationToken, PIN, status().isCreated());

        userRepository.deleteAll();
        register(verificationToken, PIN, status().isBadRequest());
        assertThat(userRepository.count()).isZero();
    }

    // ------------------------------------------------------------ the OTP step itself

    @Test
    void aWrongCodeIsRefused() throws Exception {
        requestOtp(PHONE);
        verifyOtp(PHONE, wrongCode(), status().isBadRequest());
    }

    @Test
    void theChallengeIsBurnedAfterFiveWrongCodes() throws Exception {
        requestOtp(PHONE);
        String realCode = otpSender.lastCodeFor(E164);

        for (int attempt = 0; attempt < 5; attempt++) {
            verifyOtp(PHONE, wrongCode(), status().isBadRequest());
        }

        // Even the right code no longer works: the user has to ask for a new one.
        verifyOtp(PHONE, realCode, status().isBadRequest());
    }

    @Test
    void aSecondCodeCannotBeRequestedImmediately() throws Exception {
        requestOtp(PHONE);

        mockMvc.perform(post("/api/v1/auth/register/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("phone", PHONE))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
    }

    @Test
    void aRegisteredNumberCannotBeSentAnotherRegistrationCode() throws Exception {
        register(requestAndVerifyOtp(PHONE), PIN, status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("phone", "+228 90 12 34 56"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    // -------------------------------------------------------------------- login rules

    @Test
    void loginIsRefusedWithTheWrongPinAndSaysNothingAboutTheAccount() throws Exception {
        register(requestAndVerifyOtp(PHONE), PIN, status().isCreated());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("phone", PHONE, "pin", "7412"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid phone number or PIN"));
    }

    @Test
    void theAccountLocksAfterFiveWrongPins() throws Exception {
        register(requestAndVerifyOtp(PHONE), PIN, status().isCreated());

        for (int attempt = 0; attempt < 4; attempt++) {
            login(PHONE, "7412", status().isUnauthorized());
        }
        // The attempt that trips the ceiling reports the lockout rather than bad credentials.
        login(PHONE, "7412", status().isLocked());

        // Even the correct PIN is refused while the cool-down is running.
        login(PHONE, PIN, status().isLocked());
    }

    @Test
    void refreshRotatesTheTokenAndBurnsTheOldOne() throws Exception {
        String firstRefreshToken = register(requestAndVerifyOtp(PHONE), PIN, status().isCreated())
                .path("refreshToken").asString();

        JsonNode refreshed = readData(mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", firstRefreshToken))))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(refreshed.path("refreshToken").asString()).isNotEqualTo(firstRefreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", firstRefreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void aTrivialPinIsRejectedAtRegistration() throws Exception {
        register(requestAndVerifyOtp(PHONE), "1234", status().isBadRequest());
        assertThat(userRepository.count()).isZero();
    }

    @Test
    void protectedEndpointsRejectAnAnonymousCaller() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    // ------------------------------------------------------------------------ helpers

    private void requestOtp(String phone) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/request-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("phone", phone))))
                .andExpect(status().isOk());
    }

    private JsonNode verifyOtp(String phone, String code, ResultMatcher expected) throws Exception {
        return readData(mockMvc.perform(post("/api/v1/auth/register/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("phone", phone, "code", code))))
                .andExpect(expected)
                .andReturn());
    }

    private String requestAndVerifyOtp(String phone) throws Exception {
        requestOtp(phone);
        return verifyOtp(phone, otpSender.lastCodeFor(E164), status().isOk())
                .path("verificationToken").asString();
    }

    private JsonNode register(String verificationToken, String pin, ResultMatcher expected) throws Exception {
        return readData(mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationPayload(verificationToken, pin))))
                .andExpect(expected)
                .andReturn());
    }

    private JsonNode login(String phone, String pin, ResultMatcher expected) throws Exception {
        return readData(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("phone", phone, "pin", pin))))
                .andExpect(expected)
                .andReturn());
    }

    private JsonNode readData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    /** A code that is guaranteed not to be the real one. */
    private String wrongCode() {
        String real = otpSender.lastCodeFor(E164);
        return "000000".equals(real) ? "111111" : "000000";
    }

    private Map<String, Object> registrationPayload(String verificationToken, String pin) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("verificationToken", verificationToken);
        payload.put("firstName", "Kossi");
        payload.put("lastName", "Adjo");
        payload.put("email", "kossi.adjo@example.com");
        payload.put("dateOfBirth", "1995-04-12");
        payload.put("pin", pin);
        payload.put("confirmPin", pin);
        payload.put("country", "TG");
        payload.put("acceptedPrivacyPolicy", true);
        return payload;
    }
}
