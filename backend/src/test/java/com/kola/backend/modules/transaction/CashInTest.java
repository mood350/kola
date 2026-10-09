package com.kola.backend.modules.transaction;

import com.kola.backend.common.enums.Currency;
import com.kola.backend.common.enums.TransactionType;
import com.kola.backend.modules.transaction.entity.Transaction;
import com.kola.backend.modules.transaction.service.TransactionService;
import com.kola.backend.modules.user.entity.User;
import com.kola.backend.modules.user.service.UserService;
import com.kola.backend.modules.wallet.entity.Wallet;
import com.kola.backend.modules.wallet.service.WalletService;
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

/**
 * WalletService.deposit() alone never left a trace, so a real cash-in was invisible in a
 * user's transaction history. TransactionService.cashIn wraps it so the deposit and its
 * CASH_IN trace happen together (KOLA.md 5.3.A).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CashInTest {

    @Autowired
    private UserService userService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private TransactionService transactionService;

    private UUID ownerId;

    @BeforeEach
    void createUser() {
        User user = userService.save(User.builder()
                .firstName("Efua").lastName("Asante")
                .phone("+22890" + (100000 + (int) (Math.random() * 899999)))
                .pinHash("x")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .privacyPolicyAcceptedAt(Instant.now())
                .build());
        ownerId = user.getId();
        walletService.createWallet(ownerId, Currency.XOF);
    }

    @Test
    void cashInCreditsTheWalletAndLeavesAFreeCompletedTrace() {
        Transaction tx = transactionService.cashIn(ownerId, Currency.XOF, new BigDecimal("30000"));

        assertThat(tx.getType()).isEqualTo(TransactionType.CASH_IN);
        assertThat(tx.getFee()).isEqualByComparingTo("0");
        assertThat(tx.getSenderId()).isNull(); // external funding source, not a Kola user
        assertThat(tx.getRecipientId()).isEqualTo(ownerId);

        Wallet wallet = walletService.getWallet(ownerId, Currency.XOF);
        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("30000");
    }
}
