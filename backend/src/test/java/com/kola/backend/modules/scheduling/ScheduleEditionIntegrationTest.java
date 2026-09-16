package com.kola.backend.modules.scheduling;

import com.kola.backend.common.enums.Biller;
import com.kola.backend.common.enums.Currency;
import com.kola.backend.common.enums.KycTier;
import com.kola.backend.common.enums.ScheduleFrequency;
import com.kola.backend.common.enums.ScheduledTaskStatus;
import com.kola.backend.common.enums.ScheduledTaskType;
import com.kola.backend.common.enums.WalletType;
import com.kola.backend.exception.BadRequestException;
import com.kola.backend.exception.ConflictException;
import com.kola.backend.exception.ResourceNotFoundException;
import com.kola.backend.modules.scheduling.dto.ScheduledTaskRequest;
import com.kola.backend.modules.scheduling.dto.ScheduledTaskResponse;
import com.kola.backend.modules.scheduling.dto.UpdateScheduledTaskRequest;
import com.kola.backend.modules.scheduling.service.ScheduledTaskService;
import com.kola.backend.modules.user.entity.User;
import com.kola.backend.modules.user.repository.UserRepository;
import com.kola.backend.modules.vault.dto.CreateVaultRequest;
import com.kola.backend.modules.vault.entity.Vault;
import com.kola.backend.modules.vault.service.VaultService;
import com.kola.backend.modules.wallet.entity.Wallet;
import com.kola.backend.modules.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Editing a schedule. A standing order outlives the intention that created it — a rent goes up, a
 * beneficiary changes bank, a coffre turns out to be the wrong one — so it has to be changeable
 * without cancelling and starting over.
 */
@SpringBootTest
@ActiveProfiles("test")
class ScheduleEditionIntegrationTest {

    @Autowired private ScheduledTaskService taskService;
    @Autowired private VaultService vaultService;
    @Autowired private WalletService walletService;
    @Autowired private UserRepository userRepository;

    private UUID userId;
    private Vault rentVault;
    private Vault otherVault;

    private static BigDecimal xof(String amount) {
        return new BigDecimal(amount);
    }

    private static UpdateScheduledTaskRequest empty() {
        return new UpdateScheduledTaskRequest(null, null, null, null, null, null, null, null,
                null, false, false);
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

        rentVault = vaultService.createVault(userId, new CreateVaultRequest(
                "Loyer", Currency.XOF, xof("600000"), LocalDate.now().plusYears(1), null));
        otherVault = vaultService.createVault(userId, new CreateVaultRequest(
                "Écolage", Currency.XOF, null, null, null));
    }

    private ScheduledTaskResponse aMonthlyTransfer() {
        return taskService.create(userId, new ScheduledTaskRequest(
                ScheduledTaskType.P2P_TRANSFER, ScheduleFrequency.MONTHLY,
                xof("50000"), Currency.XOF, "+22890111222",
                Instant.now().plus(10, ChronoUnit.DAYS), null, null,
                rentVault.getId(), null, 5, null));
    }

    // --- what can change --------------------------------------------------

    @Test
    void theAmountCanBeRaisedWhenTheRentGoesUp() {
        ScheduledTaskResponse task = aMonthlyTransfer();

        ScheduledTaskResponse edited = taskService.update(userId, task.id(),
                new UpdateScheduledTaskRequest(xof("65000"), null, null, null, null, null, null,
                        null, null, false, false));

        assertThat(edited.amount()).isEqualByComparingTo("65000");
    }

    /** The change the user asked for by name: pointing a schedule at a different coffre. */
    @Test
    void theFundingVaultCanBeSwapped() {
        ScheduledTaskResponse task = aMonthlyTransfer();

        ScheduledTaskResponse edited = taskService.update(userId, task.id(),
                new UpdateScheduledTaskRequest(null, null, null, otherVault.getId(), null, null,
                        null, null, null, false, false));

        assertThat(edited.fundingVaultId()).isEqualTo(otherVault.getId());
    }

    @Test
    void theDayOfTheMonthCanBeMoved() {
        ScheduledTaskResponse task = aMonthlyTransfer();

        ScheduledTaskResponse edited = taskService.update(userId, task.id(),
                new UpdateScheduledTaskRequest(null, null, null, null, null, 28, null, null, null,
                        false, false));

        assertThat(edited.dayOfMonth()).isEqualTo(28);
    }

    /** A field left out is left alone — that is what makes a partial update safe to send. */
    @Test
    void absentFieldsAreLeftAlone() {
        ScheduledTaskResponse task = aMonthlyTransfer();

        ScheduledTaskResponse edited = taskService.update(userId, task.id(),
                new UpdateScheduledTaskRequest(xof("65000"), null, null, null, null, null, null,
                        null, null, false, false));

        assertThat(edited.beneficiaryReference()).isEqualTo(task.beneficiaryReference());
        assertThat(edited.fundingVaultId()).isEqualTo(task.fundingVaultId());
        assertThat(edited.dayOfMonth()).isEqualTo(task.dayOfMonth());
        assertThat(edited.frequency()).isEqualTo(task.frequency());
    }

    /**
     * On a partial update null means "unchanged", so removing an end date needs a word of its own
     * or it cannot be said at all.
     */
    @Test
    void anEndDateIsClearedExplicitlyAndNotBySilence() {
        ScheduledTaskResponse task = taskService.create(userId, new ScheduledTaskRequest(
                ScheduledTaskType.P2P_TRANSFER, ScheduleFrequency.MONTHLY,
                xof("50000"), Currency.XOF, "+22890111222",
                Instant.now().plus(10, ChronoUnit.DAYS),
                Instant.now().plus(300, ChronoUnit.DAYS), null, rentVault.getId(), null, 5, null));
        assertThat(task.endDate()).isNotNull();

        assertThat(taskService.update(userId, task.id(), empty()).endDate()).isNotNull();

        ScheduledTaskResponse cleared = taskService.update(userId, task.id(),
                new UpdateScheduledTaskRequest(null, null, null, null, null, null, null, null,
                        null, true, false));
        assertThat(cleared.endDate()).isNull();
    }

    // --- what is refused --------------------------------------------------

    @Test
    void aVaultInAnotherCurrencyIsRefused() {
        walletService.createWallet(userId, Currency.USD);
        Vault dollars = vaultService.createVault(userId, new CreateVaultRequest(
                "Dollars", Currency.USD, null, null, null));
        ScheduledTaskResponse task = aMonthlyTransfer();

        assertThatThrownBy(() -> taskService.update(userId, task.id(),
                new UpdateScheduledTaskRequest(null, null, null, dollars.getId(), null, null, null,
                        null, null, false, false)))
                .isInstanceOf(BadRequestException.class);
    }

    /** History is not edited: reviving a cancelled schedule would make the trail lie. */
    @Test
    void aCancelledScheduleCanNoLongerBeEdited() {
        ScheduledTaskResponse task = aMonthlyTransfer();
        taskService.cancel(userId, task.id());

        assertThatThrownBy(() -> taskService.update(userId, task.id(),
                new UpdateScheduledTaskRequest(xof("1000"), null, null, null, null, null, null,
                        null, null, false, false)))
                .isInstanceOf(ConflictException.class);
    }

    /** A paused schedule is still alive, so it can be adjusted before being resumed. */
    @Test
    void aPausedScheduleCanStillBeEdited() {
        ScheduledTaskResponse task = aMonthlyTransfer();
        taskService.pause(userId, task.id());

        ScheduledTaskResponse edited = taskService.update(userId, task.id(),
                new UpdateScheduledTaskRequest(xof("60000"), null, null, null, null, null, null,
                        null, null, false, false));

        assertThat(edited.amount()).isEqualByComparingTo("60000");
        assertThat(edited.status()).isEqualTo(ScheduledTaskStatus.PAUSED);
    }

    @Test
    void theNextRunCannotBeMovedIntoThePast() {
        ScheduledTaskResponse task = aMonthlyTransfer();

        assertThatThrownBy(() -> taskService.update(userId, task.id(),
                new UpdateScheduledTaskRequest(null, null, null, null, null, null,
                        Instant.now().minus(1, ChronoUnit.DAYS), null, null, false, false)))
                .isInstanceOf(BadRequestException.class);
    }

    /** Someone else's schedule is not theirs to edit, and reads as missing rather than forbidden. */
    @Test
    void anotherUsersScheduleCannotBeEdited() {
        ScheduledTaskResponse task = aMonthlyTransfer();
        UUID stranger = UUID.randomUUID();

        assertThatThrownBy(() -> taskService.update(stranger, task.id(),
                new UpdateScheduledTaskRequest(xof("1"), null, null, null, null, null, null, null,
                        null, false, false)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- bills --------------------------------------------------------------

    /**
     * The biller and the identifier move together: a Canal+ card number means nothing once the
     * biller becomes Togocom, so changing one re-validates against the other.
     */
    @Test
    void changingTheBillerRevalidatesTheSubscriberNumber() {
        ScheduledTaskResponse bill = taskService.create(userId, new ScheduledTaskRequest(
                ScheduledTaskType.BILL_PAYMENT, ScheduleFrequency.MONTHLY,
                xof("15000"), Currency.XOF, "12345678901234",
                Instant.now().plus(10, ChronoUnit.DAYS), null, null,
                rentVault.getId(), Biller.CANAL_PLUS, 5, null));

        // A 14-digit Canal+ card is not a valid Togocom contract on its own terms, but it is
        // alphanumeric and within range, so switching biller is accepted and re-checked.
        ScheduledTaskResponse edited = taskService.update(userId, bill.id(),
                new UpdateScheduledTaskRequest(null, "TG-4417", Biller.TOGOCOM_FIBRE, null, null,
                        null, null, null, null, false, false));

        assertThat(edited.biller()).isEqualTo(Biller.TOGOCOM_FIBRE);
        assertThat(edited.beneficiaryReference()).isEqualTo("TG-4417");
    }

    @Test
    void aNewCardNumberIsCheckedAgainstTheBillerItBelongsTo() {
        ScheduledTaskResponse bill = taskService.create(userId, new ScheduledTaskRequest(
                ScheduledTaskType.BILL_PAYMENT, ScheduleFrequency.MONTHLY,
                xof("15000"), Currency.XOF, "12345678901234",
                Instant.now().plus(10, ChronoUnit.DAYS), null, null,
                rentVault.getId(), Biller.CANAL_PLUS, 5, null));

        assertThatThrownBy(() -> taskService.update(userId, bill.id(),
                new UpdateScheduledTaskRequest(null, "123", null, null, null, null, null, null,
                        null, false, false)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("14");
    }

    /** Switching to a consumption bill is refused for the same reason it cannot be created. */
    @Test
    void aScheduleCannotBeSwitchedToAConsumptionBill() {
        ScheduledTaskResponse bill = taskService.create(userId, new ScheduledTaskRequest(
                ScheduledTaskType.BILL_PAYMENT, ScheduleFrequency.MONTHLY,
                xof("15000"), Currency.XOF, "12345678901234",
                Instant.now().plus(10, ChronoUnit.DAYS), null, null,
                rentVault.getId(), Biller.CANAL_PLUS, 5, null));

        assertThatThrownBy(() -> taskService.update(userId, bill.id(),
                new UpdateScheduledTaskRequest(null, "REF9090", Biller.CEET, null, null, null,
                        null, null, null, false, false)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("consommation");
    }
}
