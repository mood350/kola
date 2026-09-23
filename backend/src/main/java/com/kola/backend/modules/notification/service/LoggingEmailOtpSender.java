package com.kola.backend.modules.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.time.Duration;

/** Development fallback, like {@link LoggingOtpSender}: logs the code instead of sending mail. */
@Slf4j
@Service
@ConditionalOnMissingBean(ignored = LoggingEmailOtpSender.class, value = EmailOtpSender.class)
public class LoggingEmailOtpSender implements EmailOtpSender {

    @Override
    public void sendOtp(String email, String code, Duration ttl) {
        log.warn("[DEV OTP] {} -> code {} (valid {} minutes). Configure a real mail sender.",
                email, code, ttl.toMinutes());
    }
}
