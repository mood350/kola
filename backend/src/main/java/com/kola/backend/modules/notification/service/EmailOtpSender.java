package com.kola.backend.modules.notification.service;

import java.time.Duration;

/** Delivers a one-time code to an email address (KYC tier 1). */
public interface EmailOtpSender {

    void sendOtp(String email, String code, Duration ttl);
}
