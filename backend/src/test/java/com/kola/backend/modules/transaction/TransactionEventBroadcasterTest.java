package com.kola.backend.modules.transaction;

import com.kola.backend.common.enums.Currency;
import com.kola.backend.common.enums.TransactionStatus;
import com.kola.backend.common.enums.TransactionType;
import com.kola.backend.modules.transaction.entity.Transaction;
import com.kola.backend.modules.transaction.mapper.TransactionMapper;
import com.kola.backend.modules.transaction.service.TransactionEventBroadcaster;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class TransactionEventBroadcasterTest {

    private final TransactionEventBroadcaster broadcaster = new TransactionEventBroadcaster(new TransactionMapper());

    @Test
    void subscribeReturnsAUsableEmitter() {
        SseEmitter emitter = broadcaster.subscribe(UUID.randomUUID());
        assertThat(emitter).isNotNull();
    }

    @Test
    void publishingWithNoLiveSubscribersIsANoOp() {
        Transaction tx = new Transaction();
        tx.setReference("TXN-TEST");
        tx.setType(TransactionType.CASH_IN);
        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setCurrency(Currency.XOF);
        tx.setAmount(BigDecimal.TEN);
        tx.setFee(BigDecimal.ZERO);
        tx.setRecipientId(UUID.randomUUID());

        assertThatCode(() -> broadcaster.publish(tx)).doesNotThrowAnyException();
    }
}
