package com.kola.backend.modules.qr.mapper;

import com.kola.backend.config.QrProperties;
import com.kola.backend.modules.qr.dto.QrCodeResponse;
import com.kola.backend.modules.qr.entity.QrCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QrCodeMapper {

    private final QrProperties properties;

    public QrCodeResponse toResponse(QrCode qr) {
        return new QrCodeResponse(qr.getId(), qr.getCode(), payload(qr.getCode()), qr.getType(),
                qr.getStatus(), qr.getAmount(), qr.getCurrency(), qr.getLabel(), qr.getExpiresAt(),
                qr.getPaidAt(), qr.getTransactionReference(), qr.getCreatedAt());
    }

    /** What the image encodes. Built here so the format lives in exactly one place. */
    public String payload(String code) {
        return properties.getPayloadBaseUrl() + code;
    }
}
