package com.dogaa.backend.modules.transaction;

import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.enums.TransactionType;
import com.dogaa.backend.exception.InsufficientFundsException;
import com.dogaa.backend.modules.transaction.dto.BillPaymentRequest;
import com.dogaa.backend.modules.transaction.entity.Transaction;
import com.dogaa.backend.modules.transaction.service.TransactionService;
import com.dogaa.backend.modules.user.entity.User;
import com.dogaa.backend.modules.user.service.UserService;
import com.dogaa.backend.modules.wallet.entity.Wallet;
import com.dogaa.backend.modules.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * dogaa-transaction had no BILL_PAYMENT support (DOGAA.md 4.6.1), which left the scheduler
 * unable to execute a "paiement de facture programme". This drives the new
 * {@code TransactionService.payBill} the same way {@code VaultFlowIntegrationTest} drives
 * vault deposits: a real funded wallet, a real debit, a real trace.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BillPaymentTest {

    @Autowired
    private UserService userService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private TransactionService transactionService;

    private UUID ownerId;

    @BeforeEach
    void createFundedWallet() {
        User user = userService.save(User.builder()
                .firstName("Kwame").lastName("Boateng")
                .phone("+22890" + (100000 + (int) (Math.random() * 899999)))
                .pinHash("x")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .privacyPolicyAcceptedAt(Instant.now())
                .build());
        ownerId = user.getId();
        Wallet wallet = walletService.createWallet(ownerId, Currency.XOF);
        // Kept well under the TIER_0 daily send cap (50 000 XOF, DOGAA.md 4.4) so these tests
        // exercise the wallet balance check, not the KYC limit.
        walletService.credit(wallet.getId(), new BigDecimal("5000"));
    }

    @Test
    void payBillDebitsTheAmountPlusFeeAndLeavesACompletedTrace() {
        Transaction tx = transactionService.payBill(ownerId,
                new BillPaymentRequest(Currency.XOF, new BigDecimal("2000"), "CEET-00234891", "Electricite"));

        assertThat(tx.getType()).isEqualTo(TransactionType.BILL_PAYMENT);
        assertThat(tx.getAmount()).isEqualByComparingTo("2000");
        assertThat(tx.getFee()).isEqualByComparingTo("20"); // 1% default rate
        assertThat(tx.getCounterparty()).isEqualTo("CEET-00234891");
        assertThat(tx.getSenderId()).isEqualTo(ownerId);

        Wallet wallet = walletService.getWallet(ownerId, Currency.XOF);
        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("2980"); // 5000 - 2000 - 20
    }

    @Test
    void payBillRefusesWhenTheAvailableBalanceIsTooLow() {
        assertThatThrownBy(() -> transactionService.payBill(ownerId,
                new BillPaymentRequest(Currency.XOF, new BigDecimal("10000"), "CEET-00234891", null)))
                .isInstanceOf(InsufficientFundsException.class);
    }
}
