package com.kola.backend.modules.qr.dto;

import com.kola.backend.common.enums.Currency;
import com.kola.backend.modules.qr.entity.QrCodeType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A scanned code as the <em>payer</em> sees it — the confirmation screen before any money moves.
 *
 * <p>The beneficiary is named but their phone number is masked, and no account id is returned. The
 * payer needs enough to recognise who they are paying; a resolve endpoint that handed back full
 * numbers would turn a shared QR into a way to harvest them.
 *
 * @param amountFixed true when the amount is the code's and cannot be changed
 * @param payable     false when the code is expired, revoked or already paid — {@code reason} says
 *                    which, so the app shows a real message instead of a generic failure
 */
public record ScannedQrResponse(String code,
                                QrCodeType type,
                                boolean payable,
                                String reason,
                                String recipientName,
                                String recipientPhoneMasked,
                                boolean amountFixed,
                                BigDecimal amount,
                                Currency currency,
                                String label,
                                Instant expiresAt) {
}
