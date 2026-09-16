package com.kola.backend.modules.qr.controller;

import com.kola.backend.common.dto.ApiResponse;
import com.kola.backend.modules.auth.security.CurrentUser;
import com.kola.backend.modules.qr.dto.CreatePaymentRequestRequest;
import com.kola.backend.modules.qr.dto.QrCodeResponse;
import com.kola.backend.modules.qr.dto.QrPaymentRequest;
import com.kola.backend.modules.qr.dto.ScannedQrResponse;
import com.kola.backend.modules.qr.entity.QrCode;
import com.kola.backend.modules.qr.mapper.QrCodeMapper;
import com.kola.backend.modules.qr.service.QrCodeService;
import com.kola.backend.modules.qr.service.QrImageGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * QR codes for receiving money (KOLA.md 4.6 — payer sans saisir de numéro).
 *
 * <p>The routes split along one line: {@code /me/**} is about the codes the caller owns, and
 * {@code /{code}/**} is about a code they have just scanned. The beneficiary of a code is always
 * its creator, so no route accepts a beneficiary.
 */
@RestController
@RequestMapping("/api/v1/qr")
@RequiredArgsConstructor
@Tag(name = "QR codes", description = "Encaisser et payer sans saisir de numéro")
@SecurityRequirement(name = "bearerAuth")
public class QrCodeController {

    private final QrCodeService qrCodeService;
    private final QrImageGenerator imageGenerator;
    private final QrCodeMapper mapper;

    // --- my codes ---------------------------------------------------------

    @GetMapping("/me")
    @Operation(summary = "Son QR code permanent — créé à la première demande")
    public ResponseEntity<ApiResponse<QrCodeResponse>> myCode(
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(qrCodeService.myStaticCode(currentUser.id())));
    }

    @PostMapping("/me/rotate")
    @Operation(summary = "Régénérer son QR code permanent — l'ancien cesse de fonctionner")
    public ResponseEntity<ApiResponse<QrCodeResponse>> rotate(
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(ApiResponse.ok("Nouveau QR code généré",
                qrCodeService.rotateStaticCode(currentUser.id())));
    }

    @PostMapping("/me/requests")
    @Operation(summary = "Créer une demande de paiement pour un montant précis")
    public ResponseEntity<ApiResponse<QrCodeResponse>> createRequest(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody CreatePaymentRequestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Demande de paiement créée",
                        qrCodeService.createPaymentRequest(currentUser.id(), request)));
    }

    @GetMapping("/me/requests")
    @Operation(summary = "Ses demandes de paiement, la plus récente en premier")
    public ResponseEntity<ApiResponse<List<QrCodeResponse>>> myRequests(
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(qrCodeService.myPaymentRequests(currentUser.id())));
    }

    @DeleteMapping("/{code}")
    @Operation(summary = "Annuler un de ses QR codes")
    public ResponseEntity<ApiResponse<QrCodeResponse>> revoke(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.ok("QR code annulé",
                qrCodeService.revoke(currentUser.id(), code)));
    }

    /**
     * The PNG, for sharing or printing.
     *
     * <p>Owner-only, and marked private: a payer already has the payload from
     * {@code GET /qr/{code}} and can draw the code themselves, so nothing is lost by not serving
     * strangers an image — and a cached QR in a shared proxy is a payment instrument left lying
     * around.
     */
    @GetMapping(value = "/{code}/image", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Image PNG d'un de ses QR codes")
    public ResponseEntity<byte[]> image(@AuthenticationPrincipal CurrentUser currentUser,
                                        @PathVariable String code,
                                        @RequestParam(required = false) Integer size) {
        QrCode qr = qrCodeService.requireOwned(currentUser.id(), code);
        byte[] png = imageGenerator.pngFor(mapper.payload(qr.getCode()), size);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(png);
    }

    // --- scanned codes ----------------------------------------------------

    /**
     * Resolves a scanned code into a confirmation screen.
     *
     * <p>Answers 200 with {@code payable=false} and a reason for an expired, revoked or already
     * paid code, rather than an error: the user is standing in front of a merchant and needs to be
     * told which of those it is.
     */
    @GetMapping("/{code}")
    @Operation(summary = "Lire un QR code scanné, avant de payer")
    public ResponseEntity<ApiResponse<ScannedQrResponse>> scan(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.ok(qrCodeService.scan(currentUser.id(), code)));
    }

    @PostMapping("/{code}/pay")
    @Operation(summary = "Payer un QR code scanné")
    public ResponseEntity<ApiResponse<QrCodeResponse>> pay(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable String code,
            @Valid @RequestBody QrPaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Paiement effectué",
                qrCodeService.pay(currentUser.id(), code, request)));
    }
}
