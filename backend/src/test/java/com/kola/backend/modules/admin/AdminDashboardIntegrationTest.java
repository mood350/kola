package com.kola.backend.modules.admin;

import com.kola.backend.common.enums.Currency;
import com.kola.backend.common.enums.Role;
import com.kola.backend.modules.admin.service.AdminService;
import com.kola.backend.modules.user.entity.User;
import com.kola.backend.modules.user.repository.UserRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the admin dashboard is wired to real Groupe A data (KOLA.md 4.5:
 * volume de transactions, solde global) and that ROLE_ADMIN is actually
 * enforced by the security filter chain, not just assumed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminDashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WalletService walletService;
    @Autowired
    private AdminService adminService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID plainUserId;

    @BeforeEach
    void fundAUser() {
        User user = userRepository.save(User.builder()
                .firstName("Ama").lastName("Koffi")
                .phone("+22890" + (100000 + (int) (Math.random() * 899999)))
                .pinHash(passwordEncoder.encode("4172"))
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .privacyPolicyAcceptedAt(Instant.now())
                .build());
        plainUserId = user.getId();
        walletService.createWallet(plainUserId, Currency.XOF);
        walletService.deposit(plainUserId, Currency.XOF, new BigDecimal("50000"));
    }

    @Test
    void dashboardCountsARealFundedWalletInTheGlobalBalance() {
        var dashboard = adminService.getDashboard();

        assertThat(dashboard.totalUsers()).isPositive();
        assertThat(dashboard.globalBalance())
                .anyMatch(w -> w.currency() == Currency.XOF && w.totalAvailable().compareTo(new BigDecimal("50000")) >= 0);
    }

    @Test
    void dashboardEndpointIsOpenToAdminOnly() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/dashboard").header("Authorization", "Bearer " + login(plainUserId, "4172")))
                .andExpect(status().isForbidden());

        String adminPhone = "+22891" + (100000 + (int) (Math.random() * 899999));
        User admin = userRepository.save(User.builder()
                .firstName("Ama").lastName("Reviewer")
                .phone(adminPhone)
                .pinHash(passwordEncoder.encode("4172"))
                .dateOfBirth(LocalDate.of(1985, 1, 1))
                .role(Role.ADMIN)
                .phoneVerified(true)
                .privacyPolicyAcceptedAt(Instant.now())
                .build());

        mockMvc.perform(get("/api/v1/admin/dashboard").header("Authorization", "Bearer " + login(admin.getId(), "4172")))
                .andExpect(status().isOk());
    }

    private String login(UUID userId, String pin) throws Exception {
        User user = userRepository.findById(userId).orElseThrow();
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("phone", user.getPhone(), "pin", pin))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        return data.path("accessToken").asString();
    }
}
