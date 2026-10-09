package com.kola.backend.modules.qr.service;

import com.kola.backend.config.QrProperties;
import com.kola.backend.exception.BadRequestException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * Turns a payload into a PNG.
 *
 * <p>Error correction is set to {@code M} rather than the default {@code L}: these codes are meant
 * to be printed on receipts and stuck to counters, where they get scratched and creased. {@code M}
 * survives about 15% damage for a modest increase in density — the right trade for something that
 * lives on paper rather than on a screen.
 */
@Component
@RequiredArgsConstructor
public class QrImageGenerator {

    private final QrProperties properties;

    public byte[] pngFor(String payload, Integer requestedSize) {
        int size = resolveSize(requestedSize);

        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.MARGIN, 2);

        try {
            BitMatrix matrix = new QRCodeWriter()
                    .encode(payload, BarcodeFormat.QR_CODE, size, size, hints);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException ex) {
            throw new IllegalStateException("Impossible de générer le QR code", ex);
        }
    }

    /**
     * A caller-supplied size is capped: rendering is CPU and memory on the server, and an
     * unbounded pixel count on an authenticated endpoint is a cheap way to hurt it.
     */
    private int resolveSize(Integer requested) {
        if (requested == null) {
            return properties.getDefaultImageSize();
        }
        if (requested < 64) {
            throw new BadRequestException("La taille minimale est de 64 pixels");
        }
        if (requested > properties.getMaxImageSize()) {
            throw new BadRequestException(
                    "La taille maximale est de " + properties.getMaxImageSize() + " pixels");
        }
        return requested;
    }
}
