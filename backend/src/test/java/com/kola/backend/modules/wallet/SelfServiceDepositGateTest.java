package com.kola.backend.modules.wallet;

import com.kola.backend.common.enums.Currency;
import com.kola.backend.exception.ServiceUnavailableException;
import com.kola.backend.modules.transaction.service.TransactionService;
import com.kola.backend.modules.wallet.controller.WalletController;
import com.kola.backend.modules.wallet.dto.DepositRequest;
import com.kola.backend.modules.wallet.mapper.WalletMapper;
import com.kola.backend.modules.wallet.service.WalletService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * The cash-in route credits any amount with no payment provider behind it, so it must stay closed
 * unless a developer opens it on purpose.
 */
class SelfServiceDepositGateTest {

    @Test
    void depositIsRefusedByDefaultAndNothingIsCredited() {
        TransactionService transactionService = mock(TransactionService.class);
        WalletController controller = new WalletController(
                mock(WalletService.class), mock(WalletMapper.class), transactionService);

        assertThatThrownBy(() -> controller.deposit(
                null, Currency.XOF, new DepositRequest(new BigDecimal("500000"))))
                .isInstanceOf(ServiceUnavailableException.class);

        verifyNoInteractions(transactionService);
    }
}
