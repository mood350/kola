package com.dogaa.backend.modules.qr.dto;

import com.dogaa.backend.common.enums.Currency;
import com.dogaa.backend.modules.qr.entity.QrCodeStatus;
import com.dogaa.backend.modules.qr.entity.QrCodeType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A code as its owner sees it.
 *
 * <p>{@code payload} is exactly what the image encodes, so the app can draw the QR itself and only
 * call the PNG endpoint when it needs a file to share or print.
 */
public record QrCodeResponse(UUID id,
                             String code,
                             String payload,
                             QrCodeType type,
                             QrCodeStatus status,
                             BigDecimal amount,
                             Currency currency,
                             String label,
                             Instant expiresAt,
                             Instant paidAt,
                             String transactionReference,
                             Instant createdAt) {
}
