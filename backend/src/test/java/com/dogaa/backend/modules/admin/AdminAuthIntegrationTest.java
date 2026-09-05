package com.dogaa.backend.modules.admin;

import com.dogaa.backend.modules.admin.entity.AdminAccount;
import com.dogaa.backend.modules.admin.entity.AdminRole;
import com.dogaa.backend.modules.admin.repository.AdminAccountRepository;
import com.dogaa.backend.modules.audit.repository.AuditLogRepository;
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
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAuthIntegrationTest {

    private static final String PASSWORD = "SuperSecret2026!";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AdminAccountRepository adminAccountRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private AdminAccount superAdmin;
    private AdminAccount support;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        adminAccountRepository.deleteAll();

        superAdmin = save("sena@dogaa.io", "Sena Amétépé", AdminRole.SUPER_ADMIN);
        support = save("prisca@dogaa.io", "Prisca Lawson", AdminRole.SUPPORT);
    }

    private AdminAccount save(String email, String name, AdminRole role) {
        return adminAccountRepository.save(AdminAccount.builder()
                .email(email)
                .name(name)
                .role(role)
                .scope("scope")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .enabled(true)
                .build());
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("token").asString();
    }

    // --- sign-in ----------------------------------------------------------

    @Test
    void loginReturnsATokenAndTheIdentityWithoutAnyEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "sena@dogaa.io", "password", PASSWORD))))
                .andExpect(status().isOk())
                // The front-end maps the JSON one-to-one: no "data" wrapper.
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("sena@dogaa.io"))
                // The role travels as the label the admin UI expects, not the enum name.
                .andExpect(jsonPath("$.user.role").value("Super-admin"));
    }

    @Test
    void aWrongPasswordSaysNothingAboutWhetherTheAccountExists() throws Exception {
        mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "sena@dogaa.io", "password", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email ou mot de passe incorrect"));

        mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "nobody@dogaa.io", "password", PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email ou mot de passe incorrect"));
    }

    @Test
    void aDisabledAccountCannotSignIn() throws Exception {
        superAdmin.setEnabled(false);
        adminAccountRepository.save(superAdmin);

        mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "sena@dogaa.io", "password", PASSWORD))))
                .andExpect(status().isLocked());
    }

    @Test
    void meReturnsTheSignedInAdministrator() throws Exception {
        mockMvc.perform(get("/api/v1/admin/auth/me")
                        .header("Authorization", "Bearer " + login("prisca@dogaa.io")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Prisca Lawson"))
                .andExpect(jsonPath("$.role").value("Support"));
    }

    @Test
    void anAnonymousCallerGets401AndNot403SoTheFrontEndCanRedirectToLogin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // --- the permission matrix, enforced server-side ----------------------

    @Test
    void supportCannotReachTheAuditLog() throws Exception {
        // Hiding the menu entry client-side stops nobody from calling the endpoint; this does.
        mockMvc.perform(get("/api/v1/admin/audit/log")
                        .header("Authorization", "Bearer " + login("prisca@dogaa.io")))
                .andExpect(status().isForbidden());
    }

    @Test
    void supportCannotReachTheRolesScreen() throws Exception {
        mockMvc.perform(get("/api/v1/admin/roles/admins")
                        .header("Authorization", "Bearer " + login("prisca@dogaa.io")))
                .andExpect(status().isForbidden());
    }

    @Test
    void aSuperAdminReachesBoth() throws Exception {
        String token = login("sena@dogaa.io");

        mockMvc.perform(get("/api/v1/admin/audit/log").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/roles/admins").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].initials").isNotEmpty());
    }

    /** A customer token must never be usable as an administrator's, whatever its role claim. */
    @Test
    void aCustomerTokenIsNotAnAdminToken() throws Exception {
        mockMvc.perform(get("/api/v1/admin/auth/me")
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    // --- password ---------------------------------------------------------

    @Test
    void changingThePasswordRequiresTheCurrentOneAndIsRecorded() throws Exception {
        String token = login("sena@dogaa.io");

        mockMvc.perform(post("/api/v1/admin/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "wrong-password",
                                "newPassword", "AnotherSecret2026!")))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/admin/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", PASSWORD,
                                "newPassword", "AnotherSecret2026!")))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(auditLogRepository.search(null, "profile", null, null))
                .anyMatch(entry -> entry.getAction().contains("mot de passe"));
    }

    @Test
    void aResetRequestNeverRevealsWhetherTheAddressExists() throws Exception {
        mockMvc.perform(post("/api/v1/admin/auth/password-reset-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "ghost@dogaa.io"))))
                .andExpect(status().isNoContent());
    }

    // --- roles ------------------------------------------------------------

    @Test
    void changingSomeonesRoleIsRecordedWithABeforeAndAfter() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/roles/admins/" + support.getId() + "/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "role", "Analyste crédit", "scope", "Scoring")))
                        .header("Authorization", "Bearer " + login("sena@dogaa.io")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("Analyste crédit"));

        assertThat(auditLogRepository.search("Sena", "roles", null, null))
                .singleElement()
                .satisfies(entry -> {
                    assertThat(entry.getDiff()).isEqualTo("Support → Analyste crédit");
                    assertThat(entry.getTargetId()).isEqualTo(support.getId().toString());
                });
    }

    @Test
    void anAdministratorCannotChangeTheirOwnRole() throws Exception {
        // Otherwise the last super-admin can demote themselves and lock everyone out.
        mockMvc.perform(patch("/api/v1/admin/roles/admins/" + superAdmin.getId() + "/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "Support")))
                        .header("Authorization", "Bearer " + login("sena@dogaa.io")))
                .andExpect(status().isBadRequest());
    }

    // --- audit ------------------------------------------------------------

    @Test
    void theAuditLogIsShapedForTheFrontEndAndNewestFirst() throws Exception {
        login("sena@dogaa.io");

        mockMvc.perform(get("/api/v1/admin/audit/log")
                        .header("Authorization", "Bearer " + login("sena@dogaa.io")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNotEmpty())
                .andExpect(jsonPath("$[0].admin").value("Sena Amétépé"))
                .andExpect(jsonPath("$[0].action").isNotEmpty())
                .andExpect(jsonPath("$[0].time").isNotEmpty());
    }

    @Test
    void theComplianceReportCatalogueIsServed() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit/reports")
                        .header("Authorization", "Bearer " + login("sena@dogaa.io")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").isNotEmpty())
                .andExpect(jsonPath("$[0].period").isNotEmpty());
    }

    @Test
    void exportsAnswerWithANullUrlWhileGenerationIsNotImplemented() throws Exception {
        mockMvc.perform(post("/api/v1/admin/audit/log/export")
                        .header("Authorization", "Bearer " + login("sena@dogaa.io")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").doesNotExist());
    }
}
