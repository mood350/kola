package com.kola.backend.modules.notification.service;

import com.kola.backend.common.util.PhoneNumbers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Development fallback: writes the code to the log instead of sending an SMS.
 *
 * <p>Backed off as soon as a real sender bean exists. It logs the code in clear, which is exactly
 * what must never happen in production — wire an SMS gateway before going live.
 */
@Slf4j
@Service
@ConditionalOnMissingBean(ignored = LoggingOtpSender.class, value = OtpSender.class)
public class LoggingOtpSender implements OtpSender {

    @Override
    public void sendOtp(String phone, String code, Duration ttl) {
        log.warn("[DEV OTP] {} -> code {} (valid {} minutes). Configure a real SMS sender.",
                PhoneNumbers.mask(phone), code, ttl.toMinutes());
    }
}
