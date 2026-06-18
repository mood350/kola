package com.kola.backend.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─── DTO réponse d'erreur ─────────────────────────────────────────────────

    public record ErrorResponse(
            String code,
            String message,
            Map<String, String> details,
            String path,
            LocalDateTime timestamp
    ) {
        public ErrorResponse(String code, String message, String path) {
            this(code, message, new HashMap<>(), path, LocalDateTime.now());
        }

        public ErrorResponse(String code, String message,
                             Map<String, String> details, String path) {
            this(code, message, details, path, LocalDateTime.now());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  SÉCURITÉ
    // ═══════════════════════════════════════════════════════════════

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Tentative de connexion échouée - {}", request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(
                        "BAD_CREDENTIALS",
                        "Email ou mot de passe incorrect",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(
            UsernameNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        "USER_NOT_FOUND",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                        "ACCESS_DENIED",
                        "Vous n'avez pas les droits pour accéder à cette ressource",
                        request.getRequestURI()
                ));
    }

    /**
     * Compte désactivé (email non confirmé).
     * Déclenché par Spring Security avant même d'arriver dans AuthController.
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(
            DisabledException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                        "ACCOUNT_DISABLED",
                        "Votre compte n'est pas encore activé. Vérifiez vos emails.",
                        request.getRequestURI()
                ));
    }

    /**
     * Compte verrouillé (trop de tentatives ou gel admin).
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLocked(
            LockedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.LOCKED)
                .body(new ErrorResponse(
                        "ACCOUNT_LOCKED",
                        "Votre compte est temporairement verrouillé. Contactez le support.",
                        request.getRequestURI()
                ));
    }

    // ═══════════════════════════════════════════════════════════════
    //  JWT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Token JWT expiré → 401.
     * Déclenché par JwtAuthFilter quand le token dépasse sa date d'expiration.
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwt(
            ExpiredJwtException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(
                        "TOKEN_EXPIRED",
                        "Votre session a expiré. Veuillez vous reconnecter.",
                        request.getRequestURI()
                ));
    }

    /**
     * Token JWT malformé (corrompu ou falsifié) → 401.
     */
    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJwt(
            MalformedJwtException ex, HttpServletRequest request) {
        log.warn("Token JWT malformé reçu depuis {}", request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(
                        "TOKEN_INVALID",
                        "Token d'authentification invalide.",
                        request.getRequestURI()
                ));
    }

    /**
     * Signature JWT incorrecte (clé secrète différente) → 401.
     */
    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ErrorResponse> handleJwtSignature(
            SignatureException ex, HttpServletRequest request) {
        log.warn("Signature JWT invalide depuis {}", request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(
                        "TOKEN_SIGNATURE_INVALID",
                        "Token d'authentification invalide.",
                        request.getRequestURI()
                ));
    }

    // ═══════════════════════════════════════════════════════════════
    //  VALIDATION
    // ═══════════════════════════════════════════════════════════════

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> details = new HashMap<>();
        ex.getBindingResult()
                .getAllErrors()
                .forEach(error -> {
                    String fieldName = ((FieldError) error).getField();
                    String errorMessage = error.getDefaultMessage();
                    details.put(fieldName, errorMessage);
                });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        "VALIDATION_ERROR",
                        "Les données envoyées sont invalides",
                        details,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        "INVALID_ARGUMENT",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        "INVALID_STATE",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    // ═══════════════════════════════════════════════════════════════
    //  RESSOURCES
    // ═══════════════════════════════════════════════════════════════

    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
            jakarta.persistence.EntityNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        "ENTITY_NOT_FOUND",
                        ex.getMessage() != null ? ex.getMessage() : "Ressource introuvable",
                        request.getRequestURI()
                ));
    }

    /**
     * Email ou numéro de téléphone déjà utilisé lors de l'inscription → 409.
     * Déclenché par la contrainte @Column(unique = true) en BDD.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violation de contrainte BDD : {}", ex.getMostSpecificCause().getMessage());

        String message = "Cette valeur est déjà utilisée.";

        // On affine le message selon la contrainte violée
        String cause = ex.getMostSpecificCause().getMessage().toLowerCase();
        if (cause.contains("email")) {
            message = "Cette adresse email est déjà associée à un compte.";
        } else if (cause.contains("phone") || cause.contains("phone_number")) {
            message = "Ce numéro de téléphone est déjà associé à un compte.";
        }

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        "DUPLICATE_ENTRY",
                        message,
                        request.getRequestURI()
                ));
    }

    // ═══════════════════════════════════════════════════════════════
    //  MÉTIER FINANCIER
    // ═══════════════════════════════════════════════════════════════

    /**
     * Solde insuffisant pour effectuer un transfert → 422.
     */
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(
            InsufficientFundsException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse(
                        "INSUFFICIENT_FUNDS",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Coffre-fort verrouillé → 423.
     */
    @ExceptionHandler(VaultLockedException.class)
    public ResponseEntity<ErrorResponse> handleVaultLocked(
            VaultLockedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.LOCKED)
                .body(new ErrorResponse(
                        "VAULT_LOCKED",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Wallet suspendu par l'admin → 403.
     */
    @ExceptionHandler(WalletInactiveException.class)
    public ResponseEntity<ErrorResponse> handleWalletInactive(
            WalletInactiveException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                        "WALLET_INACTIVE",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Devise non supportée par la plateforme → 400.
     */
    @ExceptionHandler(UnsupportedCurrencyException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedCurrency(
            UnsupportedCurrencyException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        "UNSUPPORTED_CURRENCY",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Limite KYC dépassée → 403 (l'utilisateur doit upgrader son niveau de vérification).
     */
    @ExceptionHandler(KycLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleKycLimitExceeded(
            KycLimitExceededException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                        "KYC_LIMIT_EXCEEDED",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    // ═══════════════════════════════════════════════════════════════
    //  RATE LIMITING
    // ═══════════════════════════════════════════════════════════════

    /**
     * Trop de requêtes → 429.
     * Déclenché par RateLimitingService.
     */
    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequests(
            TooManyRequestsException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ErrorResponse(
                        "TOO_MANY_REQUESTS",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    // ═══════════════════════════════════════════════════════════════
    //  HTTP
    // ═══════════════════════════════════════════════════════════════

    /**
     * Mauvaise méthode HTTP (ex : GET sur un endpoint POST) → 405.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorResponse(
                        "METHOD_NOT_ALLOWED",
                        "Méthode HTTP non autorisée : " + ex.getMethod(),
                        request.getRequestURI()
                ));
    }

    /**
     * Échec d'envoi d'email → 503.
     */
    @ExceptionHandler(MessagingException.class)
    public ResponseEntity<ErrorResponse> handleMessaging(
            MessagingException ex, HttpServletRequest request) {
        log.error("Échec d'envoi d'email : {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(
                        "EMAIL_SERVICE_ERROR",
                        "Le service d'email est temporairement indisponible. Réessayez plus tard.",
                        request.getRequestURI()
                ));
    }

    // ═══════════════════════════════════════════════════════════════
    //  CRÉDIT & PRÊTS
    // ═══════════════════════════════════════════════════════════════

    @ExceptionHandler(InsufficientCreditScoreException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientCreditScore(
            InsufficientCreditScoreException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("INSUFFICIENT_CREDIT_SCORE", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(ActiveLoanExistsException.class)
    public ResponseEntity<ErrorResponse> handleActiveLoanExists(
            ActiveLoanExistsException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("ACTIVE_LOAN_EXISTS", ex.getMessage(), request.getRequestURI()));
    }

    // ═══════════════════════════════════════════════════════════════
    //  FALLBACK
    // ═══════════════════════════════════════════════════════════════

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {
        // log.error() au lieu de printStackTrace() — bonne pratique prod
        log.error("ERREUR INTERNE non gérée : {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        "INTERNAL_ERROR",
                        "Une erreur interne est survenue. Veuillez réessayer.",
                        request.getRequestURI()
                ));
    }
}