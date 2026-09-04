package com.dogaa.backend.modules.transaction.service;

import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.common.util.PhoneNumbers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Development stub: logs the payout and returns as if the operator accepted it. Swap for a
 * real {@link ExternalTransferGateway} in production.
 */
@Slf4j
@Component
public class LoggingExternalTransferGateway implements ExternalTransferGateway {

    @Override
    public void payout(String phone, BigDecimal amount, Currency currency, String reference) {
        log.info("External payout {} {} to {} (ref {})",
                amount.toPlainString(), currency, PhoneNumbers.mask(phone), reference);
    }
}
