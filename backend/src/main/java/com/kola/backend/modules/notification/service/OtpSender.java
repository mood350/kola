package com.kola.backend.modules.notification.service;

import java.time.Duration;

/**
 * Delivers a one-time code to a phone number. Implemented here by a development stub;
 * the Mobile Money era SMS gateway plugs in behind this interface without touching auth.
 */
public interface OtpSender {

    void sendOtp(String phone, String code, Duration ttl);
}
