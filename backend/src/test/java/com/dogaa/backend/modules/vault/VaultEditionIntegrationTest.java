package com.dogaa.backend.modules.vault;

import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.KycTier;
import com.dogaa.backend.common.enums.WalletType;
import com.dogaa.backend.exception.BadRequestException;
import com.dogaa.backend.exception.ResourceNotFoundException;
import com.dogaa.backend.modules.user.entity.User;
import com.dogaa.backend.modules.user.repository.UserRepository;
import com.dogaa.backend.modules.vault.dto.CreateVaultRequest;
import com.dogaa.backend.modules.vault.dto.UpdateVaultRequest;
import com.dogaa.backend.modules.vault.entity.Vault;
import com.dogaa.backend.modules.vault.service.VaultService;
import com.dogaa.backend.modules.wallet.entity.Wallet;
import com.dogaa.backend.modules.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A savings goal outlives the intention that created it. Forcing someone to close the vault and
 * open another would mean withdrawing the money and paying it back in — losing the history the
 * score is computed from, and punishing them for changing their mind.
 */
@SpringBootTest
@ActiveProfiles("test")
class VaultEditionIntegrationTest {

    @Autowired private VaultService vaultService;
    @Autowired private WalletService walletService;
    @Autowired private UserRepository userRepository;

    private UUID userId;
    private Vault vault;

    private static BigDecimal xof(String amount) {
        return new BigDecimal(amount);
    }

    private static UpdateVaultRequest patch(String name, BigDecimal target, LocalDate date,
                                            String description) {
        return new UpdateVaultRequest(name, target, date, description, false, false);
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User user = userRepository.save(User.builder()
                .firstName("Kossi").lastName("Adjo")
                .phone("+2289" + (int) (Math.random() * 9_000_000 + 1_000_000))
                .pinHash("x").dateOfBirth(LocalDate.of(1990, 1, 1))
                .phoneVerified(true).kycTier(KycTier.TIER_2)
                .privacyPolicyAcceptedAt(Instant.now()).build());
        userId = user.getId();

        Wallet current = walletService.provision(userId, Currency.XOF, WalletType.CURRENT);
        walletService.credit(current.getId(), xof("500000"));

        vault = vaultService.createVault(userId, new CreateVaultRequest(
                "Mariage", Currency.XOF, xof("600000"), LocalDate.now().plusMonths(6), "juin"));
        vaultService.deposit(userId, vault.getId(), xof("100000"));
    }

    // --- what can change --------------------------------------------------

    @Test
    void aGoalCanBeRenamedAndRetargeted() {
        Vault edited = vaultService.updateVault(userId, vault.getId(),
                patch("Apport logement", xof("900000"), LocalDate.now().plusMonths(18), "acompte"));

        assertThat(edited.getName()).isEqualTo("Apport logement");
        assertThat(edited.getTargetAmount()).isEqualByComparingTo("900000");
        assertThat(edited.getDescription()).isEqualTo("acompte");
    }

    /** Editing the goal must not touch the money already in it. */
    @Test
    void theBalanceIsUntouchedByAnEdit() {
        Vault edited = vaultService.updateVault(userId, vault.getId(),
                patch("Autre nom", xof("900000"), null, null));

        assertThat(edited.getBalance()).isEqualByComparingTo("100000");
        assertThat(edited.getCurrency()).isEqualTo(Currency.XOF);
    }

    @Test
    void absentFieldsAreLeftAlone() {
        Vault edited = vaultService.updateVault(userId, vault.getId(),
                patch("Apport logement", null, null, null));

        assertThat(edited.getTargetAmount()).isEqualByComparingTo("600000");
        assertThat(edited.getTargetDate()).isEqualTo(LocalDate.now().plusMonths(6));
    }

    /** Null means "unchanged", so giving up on a target needs a word of its own. */
    @Test
    void aTargetIsDroppedExplicitlyAndNotBySilence() {
        assertThat(vaultService.updateVault(userId, vault.getId(),
                patch(null, null, null, null)).getTargetAmount()).isNotNull();

        Vault cleared = vaultService.updateVault(userId, vault.getId(),
                new UpdateVaultRequest(null, null, null, null, true, true));

        assertThat(cleared.getTargetAmount()).isNull();
        assertThat(cleared.getTargetDate()).isNull();
    }

    // --- what is refused --------------------------------------------------

    /** A goal below what is already saved would show as met on the spot, which reads as a bug. */
    @Test
    void aTargetBelowWhatIsAlreadySavedIsRefused() {
        assertThatThrownBy(() -> vaultService.updateVault(userId, vault.getId(),
                patch(null, xof("50000"), null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("100000");
    }

    @Test
    void aDeadlineInThePastIsRefused() {
        assertThatThrownBy(() -> vaultService.updateVault(userId, vault.getId(),
                patch(null, null, LocalDate.now().minusDays(1), null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void aClosedVaultCannotBeEdited() {
        vaultService.closeVault(userId, vault.getId());

        assertThatThrownBy(() -> vaultService.updateVault(userId, vault.getId(),
                patch("Trop tard", null, null, null)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void anotherUsersVaultCannotBeEdited() {
        assertThatThrownBy(() -> vaultService.updateVault(UUID.randomUUID(), vault.getId(),
                patch("Le mien maintenant", null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
