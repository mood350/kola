package com.kola.backend.modules.transaction;

import com.kola.backend.common.enums.Currency;
import com.kola.backend.common.enums.TransactionType;
import com.kola.backend.modules.transaction.dto.BillPaymentRequest;
import com.kola.backend.modules.transaction.entity.Transaction;
import com.kola.backend.modules.transaction.service.TransactionService;
import com.kola.backend.modules.user.entity.User;
import com.kola.backend.modules.user.service.UserService;
import com.kola.backend.modules.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TransactionHistoryFilterTest {

    @Autowired
    private UserService userService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private TransactionService transactionService;

    private UUID ownerId;

    @BeforeEach
    void createFundedUser() {
        User user = userService.save(User.builder()
                .firstName("Nadia").lastName("Toure")
                .phone("+22890" + (100000 + (int) (Math.random() * 899999)))
                .pinHash("x")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .privacyPolicyAcceptedAt(Instant.now())
                .build());
        ownerId = user.getId();
        walletService.createWallet(ownerId, Currency.XOF);
        transactionService.cashIn(ownerId, Currency.XOF, new BigDecimal("50000"));
        transactionService.payBill(ownerId,
                new BillPaymentRequest(Currency.XOF, new BigDecimal("2000"), "CEET-1", null));
    }

    @Test
    void filtersHistoryByType() {
        Page<Transaction> cashIns = transactionService.history(
                ownerId, TransactionType.CASH_IN, null, null, null, PageRequest.of(0, 10));
        assertThat(cashIns.getContent()).hasSize(1);
        assertThat(cashIns.getContent().get(0).getType()).isEqualTo(TransactionType.CASH_IN);

        Page<Transaction> bills = transactionService.history(
                ownerId, TransactionType.BILL_PAYMENT, null, null, null, PageRequest.of(0, 10));
        assertThat(bills.getContent()).hasSize(1);
    }

    @Test
    void unfilteredHistoryReturnsEverything() {
        Page<Transaction> all = transactionService.history(ownerId, PageRequest.of(0, 10));
        assertThat(all.getContent()).hasSize(2);
    }

    @Test
    void filtersHistoryByDateWindow() {
        Instant future = Instant.now().plusSeconds(3600);
        Page<Transaction> nothingYet = transactionService.history(
                ownerId, null, null, future, null, PageRequest.of(0, 10));
        assertThat(nothingYet.getContent()).isEmpty();
    }
}
