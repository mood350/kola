package com.kola.backend.auth;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void register(@RequestBody @Valid RegistrationRequest request, HttpServletRequest httpRequest)
            throws MessagingException {
        authService.register(request, clientIp(httpRequest));
    }

    @PostMapping("/confirm")
    public ResponseEntity<String> confirmAccount(
            @RequestBody @Valid ConfirmAccountRequest request,
            HttpServletRequest httpRequest
    ) throws MessagingException {
        authService.confirmAccount(request.token(), clientIp(httpRequest));
        return ResponseEntity.ok("Compte activé avec succès !");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @RequestBody @Valid AuthenticationRequest request,
            HttpServletRequest httpRequest
    ) throws MessagingException {
        String ip = clientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(authService.authenticate(request, ip, userAgent));
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@RequestBody @Valid ForgotPasswordRequest request)
            throws MessagingException {
        authService.requestPasswordReset(request.email());
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @RequestBody @Valid ResetPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        authService.resetPassword(request.token(), request.newPassword(), clientIp(httpRequest));
        return ResponseEntity.ok("Mot de passe réinitialisé avec succès !");
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthenticationResponse> refreshToken(
            HttpServletRequest request
    ) {
        // Extrait le refresh token du header
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        final String refreshToken = authHeader.substring(7);
        return ResponseEntity.ok(authService.refreshToken(refreshToken, clientIp(request)));
    }

    /**
     * POST /api/auth/logout
     *
     * Déconnecte l'utilisateur en effaçant le SecurityContext.
     * Le frontend doit supprimer les tokens de son côté (localStorage).
     *
     * Header : Authorization: Bearer <access_token>
     *
     * LIMITE CONNUE (non résolue dans cette passe) : le token reste
     * valide côté serveur jusqu'à expiration naturelle, faute de
     * blacklist (Redis ou BDD). Cf. commentaire dans AuthenticationService.logout().
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204 — pas de body
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        authService.logout(request, response);
    }

    /**
     * Extrait l'IP réelle du client, en tenant compte d'un éventuel
     * reverse proxy (header X-Forwarded-For). Sans ça, derrière un load
     * balancer/proxy, getRemoteAddr() renverrait toujours l'IP du proxy
     * et le rate limiting par IP serait inefficace (tout le trafic
     * partagerait la même clé).
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Le header peut contenir une liste "client, proxy1, proxy2" — on garde le premier
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}