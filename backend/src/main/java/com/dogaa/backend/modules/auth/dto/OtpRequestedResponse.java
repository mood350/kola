package com.dogaa.backend.modules.auth.dto;

import java.time.Instant;

/** Never carries the code itself — it only travels by SMS. */
public record OtpRequestedResponse(String phone,
                                   long codeExpiresInSeconds,
                                   Instant resendAvailableAt) {
}
