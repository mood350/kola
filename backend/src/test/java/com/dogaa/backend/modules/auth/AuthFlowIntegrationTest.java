package com.dogaa.backend.modules.auth;

import com.dogaa.backend.modules.user.repository.UserRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

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
    private static final String PIN = "8305";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void registerThenLoginThenReadOwnProfile() throws Exception {
        JsonNode registered = register(PHONE, PIN);
        assertThat(registered.path("user").path("phone").asText()).isEqualTo("+22890123456");
        assertThat(registered.path("user").path("kycTier").asText()).isEqualTo("TIER_0");
        assertThat(registered.path("accessToken").asText()).isNotBlank();

        JsonNode loggedIn = login(PHONE, PIN, status().isOk());
        String accessToken = loggedIn.path("accessToken").asText();

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value("+22890123456"))
                .andExpect(jsonPath("$.data.firstName").value("Kossi"));
    }

    @Test
    void theSamePhoneNumberCannotRegisterTwiceEvenInAnotherFormat() throws Exception {
        register(PHONE, PIN);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationPayload("+228 90 12 34 56", PIN))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void loginIsRefusedWithTheWrongPinAndSaysNothingAboutTheAccount() throws Exception {
        register(PHONE, PIN);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("phone", PHONE, "pin", "7412"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid phone number or PIN"));
    }

    @Test
    void theAccountLocksAfterFiveWrongPins() throws Exception {
        register(PHONE, PIN);

        for (int attempt = 0; attempt < 4; attempt++) {
            login(PHONE, "7412", status().isUnauthorized());
        }
        // The attempt that trips the ceiling reports the lockout rather than bad credentials.
        login(PHONE, "7412", status().isLocked());

        // Even the correct PIN is refused while the cool-down is running.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("phone", PHONE, "pin", PIN))))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    @Test
    void refreshRotatesTheTokenAndBurnsTheOldOne() throws Exception {
        String firstRefreshToken = register(PHONE, PIN).path("refreshToken").asText();

        JsonNode refreshed = readData(mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", firstRefreshToken))))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(refreshed.path("refreshToken").asText()).isNotEqualTo(firstRefreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", firstRefreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void aTrivialPinIsRejectedAtRegistration() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationPayload(PHONE, "1234"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void protectedEndpointsRejectAnAnonymousCaller() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private JsonNode register(String phone, String pin) throws Exception {
        return readData(mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationPayload(phone, pin))))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private JsonNode login(String phone, String pin,
                           org.springframework.test.web.servlet.ResultMatcher expected) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("phone", phone, "pin", pin))))
                .andExpect(expected)
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private JsonNode readData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private Map<String, Object> registrationPayload(String phone, String pin) {
        return Map.of(
                "firstName", "Kossi",
                "lastName", "Adjo",
                "phone", phone,
                "email", "kossi.adjo@example.com",
                "dateOfBirth", "1995-04-12",
                "pin", pin,
                "confirmPin", pin,
                "country", "TG");
    }
}
