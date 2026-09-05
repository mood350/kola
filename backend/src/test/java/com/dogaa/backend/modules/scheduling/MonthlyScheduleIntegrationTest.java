package com.dogaa.backend.modules.scheduling;

import com.dogaa.backend.common.enums.Biller;
import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.KycTier;
import com.dogaa.backend.common.enums.ScheduleFrequency;
import com.dogaa.backend.common.enums.ScheduledTaskType;
import com.dogaa.backend.common.enums.WalletType;
import com.dogaa.backend.exception.BadRequestException;
import com.dogaa.backend.exception.InsufficientFundsException;
import com.dogaa.backend.modules.scheduling.dto.ScheduledTaskRequest;
import com.dogaa.backend.modules.scheduling.dto.ScheduledTaskResponse;
import com.dogaa.backend.modules.scheduling.service.ScheduledTaskService;
import com.dogaa.backend.modules.user.entity.User;
import com.dogaa.backend.modules.user.repository.UserRepository;
import com.dogaa.backend.modules.vault.dto.CreateVaultRequest;
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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Scheduling a monthly payment: the vault that funds it, the biller identifier, and the date.
 */
@SpringBootTest
@ActiveProfiles("test")
class MonthlyScheduleIntegrationTest {

    @Autowired private ScheduledTaskService scheduledTaskService;
    @Autowired private VaultService vaultService;
    @Autowired private WalletService walletService;
    @Autowired private UserRepository userRepository;

    private UUID userId;
    private Wallet current;
    private Vault rentVault;

    private static BigDecimal xof(String amount) {
        return new BigDecimal(amount);
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User user = userRepository.save(User.builder()
                .firstName("Kossi").lastName("Adjo")
                .phone("+2289" + (int) (Math.random() * 9_000_000 + 1_000_000))
                .pinHash("x")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .phoneVerified(true)
                .kycTier(KycTier.TIER_2)
                .privacyPolicyAcceptedAt(Instant.now())
                .build());
        userId = user.getId();

        current = walletService.provision(userId, Currency.XOF, WalletType.CURRENT);
        walletService.provision(userId, Currency.XOF, WalletType.SAVINGS);
        walletService.credit(current.getId(), xof("500000"));

        rentVault = vaultService.createVault(userId, new CreateVaultRequest(
                "Loyer", Currency.XOF, xof("600000"), LocalDate.now().plusYears(1), null));
        vaultService.deposit(userId, rentVault.getId(), xof("300000"));
    }

    private ScheduledTaskRequest monthlyTransfer(UUID vaultId, Integer dayOfMonth) {
        return new ScheduledTaskRequest(
                ScheduledTaskType.P2P_TRANSFER, ScheduleFrequency.MONTHLY,
                xof("50000"), Currency.XOF, "+22890111222",
                Instant.parse("2026-10-05T00:00:00Z"), null, null,
                vaultId, null, dayOfMonth, null);
    }

    // --- the funding vault ------------------------------------------------

    /**
     * The rule the whole change exists for: a schedule must not help itself to the everyday
     * balance on a date the user chose weeks earlier and no longer has in mind.
     */
    @Test
    void aScheduleWithoutAFundingVaultIsRefused() {
        assertThatThrownBy(() -> scheduledTaskService.create(userId, monthlyTransfer(null, 5)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("coffre");
    }

    @Test
    void aScheduleNamesTheVaultItSpendsFrom() {
        ScheduledTaskResponse task =
                scheduledTaskService.create(userId, monthlyTransfer(rentVault.getId(), 5));

        assertThat(task.fundingVaultId()).isEqualTo(rentVault.getId());
    }

    /** Someone else's vault is not theirs to spend, and reads as missing rather than forbidden. */
    @Test
    void anotherUsersVaultCannotFundASchedule() {
        User other = userRepository.save(User.builder()
                .firstName("Ama").lastName("Kossi")
                .phone("+2289" + (int) (Math.random() * 9_000_000 + 1_000_000))
                .pinHash("x").dateOfBirth(LocalDate.of(1990, 1, 1))
                .phoneVerified(true).kycTier(KycTier.TIER_2)
                .privacyPolicyAcceptedAt(Instant.now()).build());
        walletService.provision(other.getId(), Currency.XOF, WalletType.CURRENT);
        Vault theirs = vaultService.createVault(other.getId(), new CreateVaultRequest(
                "Le leur", Currency.XOF, null, null, null));

        assertThatThrownBy(() ->
                scheduledTaskService.create(userId, monthlyTransfer(theirs.getId(), 5)))
                .isInstanceOf(RuntimeException.class);
    }

    /**
     * Checked at creation, not at midnight: telling someone their rent failed is a far worse
     * moment to discover the vault was in the wrong currency.
     */
    @Test
    void aVaultInAnotherCurrencyIsRefusedUpFront() {
        walletService.createWallet(userId, Currency.USD);
        Vault dollars = vaultService.createVault(userId, new CreateVaultRequest(
                "Dollars", Currency.USD, null, null, null));

        assertThatThrownBy(() ->
                scheduledTaskService.create(userId, monthlyTransfer(dollars.getId(), 5)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("USD");
    }

    /** A vault deposit's source is the current account by nature — requiring a vault is circular. */
    @Test
    void aVaultDepositNeedsNoFundingVault() {
        ScheduledTaskRequest deposit = new ScheduledTaskRequest(
                ScheduledTaskType.VAULT_DEPOSIT, ScheduleFrequency.MONTHLY,
                xof("20000"), Currency.XOF, rentVault.getId().toString(),
                Instant.parse("2026-10-05T00:00:00Z"), null, null, null, null, 5, null);

        assertThatCode(() -> scheduledTaskService.create(userId, deposit)).doesNotThrowAnyException();
    }

    /** A recurring Bankivi contribution moves the owner's own current balance into savings. */
    @Test
    void aSavingsDepositNeedsNoFundingVault() {
        ScheduledTaskRequest contribution = new ScheduledTaskRequest(
                ScheduledTaskType.SAVINGS_DEPOSIT, ScheduleFrequency.MONTHLY,
                xof("25000"), Currency.XOF, "BANKIVI",
                Instant.parse("2026-10-05T00:00:00Z"), null, null, null, null, 5, null);

        ScheduledTaskResponse task = scheduledTaskService.create(userId, contribution);

        assertThat(task.fundingVaultId()).isNull();
        assertThat(task.type()).isEqualTo(ScheduledTaskType.SAVINGS_DEPOSIT);
    }

    // --- the vault actually pays -------------------------------------------

    /**
     * The release covers the commission too. Releasing only the transfer amount and taking the fee
     * from the current account would be the very raid on everyday money this feature prevents.
     */
    @Test
    void releasingForAPaymentTakesTheFeeFromTheVaultAsWell() {
        BigDecimal before = walletService.getById(current.getId()).getAvailableBalance();

        vaultService.releaseForPayment(userId, rentVault.getId(), xof("50750"));

        assertThat(vaultService.getVault(userId, rentVault.getId()).getBalance())
                .isEqualByComparingTo("249250");
        assertThat(walletService.getById(current.getId()).getAvailableBalance())
                .isEqualByComparingTo(before.add(xof("50750")));
    }

    /** A short vault must say which vault is short, not just "solde insuffisant". */
    @Test
    void aVaultWithTooLittleSaysWhichOne() {
        assertThatThrownBy(() ->
                vaultService.releaseForPayment(userId, rentVault.getId(), xof("999999")))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Loyer");
    }

    // --- the day of the month ----------------------------------------------

    @Test
    void aMonthlyScheduleRemembersTheDayItWasSetFor() {
        ScheduledTaskResponse task =
                scheduledTaskService.create(userId, monthlyTransfer(rentVault.getId(), 28));

        assertThat(task.dayOfMonth()).isEqualTo(28);
    }

    @Test
    void theDayIsTakenFromTheFirstRunWhenNotGiven() {
        ScheduledTaskResponse task =
                scheduledTaskService.create(userId, monthlyTransfer(rentVault.getId(), null));

        assertThat(task.dayOfMonth())
                .isEqualTo(LocalDateTime.ofInstant(Instant.parse("2026-10-05T00:00:00Z"),
                        ZoneOffset.UTC).getDayOfMonth());
    }

    // --- bills --------------------------------------------------------------

    private ScheduledTaskRequest monthlyBill(Biller biller, String identifier) {
        return new ScheduledTaskRequest(
                ScheduledTaskType.BILL_PAYMENT, ScheduleFrequency.MONTHLY,
                xof("15000"), Currency.XOF, identifier,
                Instant.parse("2026-10-05T00:00:00Z"), null, null,
                rentVault.getId(), biller, 5, null);
    }

    /** Canal+ publishes its format: 14 digits. It is the one biller we can check strictly. */
    @Test
    void aCanalPlusCardIsFourteenDigits() {
        assertThatCode(() -> scheduledTaskService.create(userId,
                monthlyBill(Biller.CANAL_PLUS, "12345678901234"))).doesNotThrowAnyException();

        assertThatThrownBy(() -> scheduledTaskService.create(userId,
                monthlyBill(Biller.CANAL_PLUS, "12345")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("14");
    }

    /** People copy a card number off a bill with spaces. Rejecting that teaches distrust of the form. */
    @Test
    void separatorsCopiedFromAPaperBillAreStripped() {
        ScheduledTaskResponse task = scheduledTaskService.create(userId,
                monthlyBill(Biller.CANAL_PLUS, "1234 5678 9012 34"));

        assertThat(task.beneficiaryReference()).isEqualTo("12345678901234");
    }

    /**
     * A consumption bill changes every month. Scheduling a fixed sum against one would silently
     * underpay or overpay for ever, so it is refused rather than allowed to look like it works.
     */
    @Test
    void aConsumptionBillCannotBeScheduledForAFixedAmount() {
        assertThatThrownBy(() -> scheduledTaskService.create(userId,
                monthlyBill(Biller.CEET, "REF12345")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("consommation");

        assertThatThrownBy(() -> scheduledTaskService.create(userId,
                monthlyBill(Biller.CASH_POWER, "12345678")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void aBillWithoutABillerIsRefused() {
        assertThatThrownBy(() -> scheduledTaskService.create(userId,
                monthlyBill(null, "12345678901234")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("service");
    }

    /** A meter number is digits. A phone number typed into that box is a mistake worth catching. */
    @Test
    void aDigitsOnlyIdentifierRejectsLetters() {
        assertThatThrownBy(() -> scheduledTaskService.create(userId,
                monthlyBill(Biller.CANAL_PLUS, "ABCD5678901234")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("chiffres");
    }
}
