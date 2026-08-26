package com.kola.backend.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import com.kola.backend.payment.PaymentProviderException;
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

    // Cette méthode unique gère désormais les deux cas de figure de manière identique
    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleAuthenticationExceptions(
            Exception ex, HttpServletRequest request) {

        log.warn("Tentative de connexion échouée ({}) - {}", ex.getClass().getSimpleName(), request.getRemoteAddr());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(
                        "BAD_CREDENTIALS",
                        "Email ou mot de passe incorrect",
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

    /**
     * Corps de requête absent, tronqué ou non parsable.
     *
     * On ne renvoie NI ne logge le message de l'exception : Jackson y recopie
     * un extrait du payload fautif. Sur /api/auth/reset-password, ce serait
     * réintroduire dans les logs le mot de passe en clair qu'on vient d'en
     * sortir. Sans ce handler, l'exception finissait dans handleGeneric, qui
     * logge ex.getMessage() en ERROR — et répondait 500 pour une erreur
     * strictement côté client.
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(
            org.springframework.http.converter.HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        log.warn("Corps de requête illisible sur {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        "MALFORMED_BODY",
                        "Le corps de la requête est absent ou mal formé.",
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
     * Écriture concurrente détectée par le verrouillage optimiste
     * (Wallet.version / Vault.version).
     *
     * 409 et non 500 : l'opération n'a rien cassé, elle a perdu la course. Le
     * client peut simplement rejouer. Sans ce handler, le cas tombait dans
     * handleGeneric et ressortait en « erreur interne », ce qui poussait à
     * chercher une panne serveur là où il n'y a qu'une collision.
     */
    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            org.springframework.orm.ObjectOptimisticLockingFailureException ex,
            HttpServletRequest request) {
        log.warn("Conflit d'écriture concurrente sur {} : {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        "CONCURRENT_MODIFICATION",
                        "Une autre opération a modifié ces données en même temps. Veuillez réessayer.",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violation de contrainte BDD : {}", ex.getMostSpecificCause().getMessage());

        String code = "DUPLICATE_ENTRY";
        String message = "Cette valeur est déjà utilisée.";

        String cause = ex.getMostSpecificCause().getMessage().toLowerCase();
        if (cause.contains("email")) {
            message = "Cette adresse email est déjà associée à un compte.";
        } else if (cause.contains("phone") || cause.contains("phone_number")) {
            message = "Ce numéro de téléphone est déjà associé à un compte.";
        } else if (cause.contains("idempotency")) {
            // Deux requêtes simultanées portant la même clé : la seconde a
            // perdu la course à l'insertion. Sans ce cas explicite, le client
            // recevait « Cette valeur est déjà utilisée », message qui laissait
            // croire à un doublon d'email ou de téléphone.
            code = "IDEMPOTENCY_CONFLICT";
            message = "Une opération portant la même clé d'idempotence est en cours. Réessayez.";
        }

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        code,
                        message,
                        request.getRequestURI()
                ));
    }

    // ═══════════════════════════════════════════════════════════════
    //  MÉTIER FINANCIER
    // ═══════════════════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════════════════
    //  CRÉDIT & PRÊTS
    // ═══════════════════════════════════════════════════════════════

    @ExceptionHandler(InsufficientCreditScoreException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientCreditScore(
            InsufficientCreditScoreException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("INSUFFICIENT_CREDIT_SCORE", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(LoanNotEligibleException.class)
    public ResponseEntity<ErrorResponse> handleLoanNotEligible(
            LoanNotEligibleException ex, HttpServletRequest request) {
        // 422 : la requete est valide, l'utilisateur authentifie — c'est sa
        // situation qui bloque. Le message nomme toujours la condition
        // manquante, sans quoi l'emprunteur ne sait pas quoi corriger.
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("LOAN_NOT_ELIGIBLE", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(ActiveLoanExistsException.class)
    public ResponseEntity<ErrorResponse> handleActiveLoanExists(
            ActiveLoanExistsException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("ACTIVE_LOAN_EXISTS", ex.getMessage(), request.getRequestURI()));
    }

    // ═══════════════════════════════════════════════════════════════
    //  CODES À USAGE UNIQUE (activation, réinitialisation)
    // ═══════════════════════════════════════════════════════════════

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(
            InvalidTokenException ex, HttpServletRequest request) {
        // 400 et non 500 : le code soumis est faux, expiré ou déjà utilisé —
        // rien n'a dysfonctionné côté serveur. Le client peut donc l'afficher
        // sous le champ concerné au lieu d'annoncer une panne.
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_TOKEN", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(
            InvalidRefreshTokenException ex, HttpServletRequest request) {
        // 401 : c'est le statut auquel un client réagit en purgeant sa session
        // et en renvoyant vers la connexion. Un 500 lui ferait croire à une
        // panne passagère, et réessayer avec le même jeton mort.
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("INVALID_REFRESH_TOKEN", ex.getMessage(), request.getRequestURI()));
    }

    // ═══════════════════════════════════════════════════════════════
    //  PRESTATAIRE DE PAIEMENT
    // ═══════════════════════════════════════════════════════════════

    @ExceptionHandler(PaymentProviderException.class)
    public ResponseEntity<ErrorResponse> handlePaymentProvider(
            PaymentProviderException ex, HttpServletRequest request) {
        // 502 : la panne vient d'un système tiers. Un 500 accuserait Kola, un
        // 400 accuserait l'utilisateur — ni l'un ni l'autre n'aiderait le
        // support à savoir où chercher. Le message est celui du prestataire,
        // déjà rédigé pour l'utilisateur final.
        log.error("Prestataire de paiement en échec : {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("PAYMENT_PROVIDER_ERROR", ex.getMessage(), request.getRequestURI()));
    }


    // ═══════════════════════════════════════════════════════════════
    //  FALLBACK
    // ═══════════════════════════════════════════════════════════════

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {
        log.error("ERREUR INTERNE non gérée : {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        "INTERNAL_ERROR",
                        "Une erreur interne est survenue. Veuillez réessayer.",
                        request.getRequestURI()
                ));
    }
}