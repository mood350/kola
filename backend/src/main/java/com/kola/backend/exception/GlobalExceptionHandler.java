package com.kola.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║              GlobalExceptionHandler.java                    ║
 * ║       Gestion centralisée de toutes les exceptions          ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * @RestControllerAdvice intercepte toutes les exceptions lancées
 * par n'importe quel Controller et retourne une réponse JSON propre.
 *
 * SANS ce handler :
 *  → Spring retourne une page HTML d'erreur ou un JSON générique
 *  → Le frontend ne sait pas quoi afficher
 *
 * Avec handler :
 *  → Chaque erreur retourne un JSON structuré et cohérent ✅
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * ┌─────────────────────────────────────────────────────────┐
     * │  DTO — Format de réponse d'erreur                       │
     * └─────────────────────────────────────────────────────────┘
     *
     * C'est ce que le frontend reçoit pour CHAQUE erreur :
     * {
     *   "code"      : "BAD_CREDENTIALS",
     *   "message"   : "Email ou mot de passe incorrect",
     *   "details"   : {},
     *   "path"      : "/api/auth/login",
     *   "timestamp" : "2024-01-15T10:30:00"
     * }
     */
    public record ErrorResponse(
            String code,
            String message,
            Map<String, String> details,
            String path,
            LocalDateTime timestamp
    ) {
        // Constructeur simplifié sans details (pour les erreurs simples)
        public ErrorResponse(String code, String message, String path) {
            this(code, message, new HashMap<>(), path, LocalDateTime.now());
        }

        // Constructeur complet avec details (pour les erreurs de validation)
        public ErrorResponse(String code, String message,
                             Map<String, String> details, String path) {
            this(code, message, details, path, LocalDateTime.now());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  SÉCURITÉ
    // ═══════════════════════════════════════════════════════════════

    /**
     * 401 UNAUTHORIZED — Mauvais email ou mot de passe
     *
     * Déclenché par : authenticationManager.authenticate() dans AuthenticationService
     * quand les credentials sont incorrects.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request) {
        log.error("ERREUR INTERNE : {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(
                        "BAD_CREDENTIALS",
                        "Email ou mot de passe incorrect",
                        request.getRequestURI()
                ));
    }

    /**
     * 404 NOT_FOUND — Utilisateur introuvable
     *
     * Déclenché par : UserDetailsServiceImpl.loadUserByUsername()
     * quand l'email n'existe pas en BDD.
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(
            UsernameNotFoundException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        "USER_NOT_FOUND",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * 403 FORBIDDEN — Accès refusé (pas les droits)
     *
     * Déclenché par : @PreAuthorize("hasRole('ADMIN')") quand
     * l'utilisateur n'a pas le rôle requis.
     *
     * ⚠️ Pour que ce handler soit appelé au lieu de Spring Security,
     * il faut configurer un AccessDeniedHandler dans SecurityConfig.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                        "ACCESS_DENIED",
                        "Vous n'avez pas les droits pour accéder à cette ressource",
                        request.getRequestURI()
                ));
    }

    // ═══════════════════════════════════════════════════════════════
    //  VALIDATION
    // ═══════════════════════════════════════════════════════════════

    /**
     * 400 BAD_REQUEST — Erreurs de validation (@NotBlank, @Email, @Size...)
     *
     * Déclenché par : @Valid dans AuthController quand les données
     * envoyées ne respectent pas les contraintes du DTO.
     *
     * Retourne le détail de CHAQUE champ invalide :
     * {
     *   "details": {
     *     "email"    : "Email invalide",
     *     "password" : "Le mot de passe doit contenir au moins 8 caractères"
     *   }
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        // On collecte tous les champs invalides et leurs messages
        Map<String, String> details = new HashMap<>();
        ex.getBindingResult()
                .getAllErrors()
                .forEach(error -> {
                    String fieldName = ((FieldError) error).getField();
                    String errorMessage = error.getDefaultMessage();
                    details.put(fieldName, errorMessage);
                });

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        "VALIDATION_ERROR",
                        "Les données envoyées sont invalides",
                        details,
                        request.getRequestURI()
                ));
    }

    // ═══════════════════════════════════════════════════════════════
    //  RESSOURCES
    // ═══════════════════════════════════════════════════════════════

    /**
     * 404 NOT_FOUND — Ressource introuvable
     *
     * Déclenché par : repository.findById().orElseThrow(EntityNotFoundException::new)
     * dans n'importe quel Service quand une entité n'existe pas en BDD.
     *
     * Exemple : GET /api/books/999 → livre introuvable
     */
    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(
            jakarta.persistence.EntityNotFoundException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        "ENTITY_NOT_FOUND",
                        ex.getMessage() != null ? ex.getMessage() : "Ressource introuvable",
                        request.getRequestURI()
                ));
    }

    // ═══════════════════════════════════════════════════════════════
    //  FALLBACK — Toutes les autres exceptions
    // ═══════════════════════════════════════════════════════════════

    /**
     * 500 INTERNAL_SERVER_ERROR — Toute exception non gérée
     *
     * C'est le filet de sécurité — si aucun handler ci-dessus
     * ne correspond, on retourne une erreur 500 générique.
     *
     * ⚠️ On ne retourne JAMAIS les détails techniques en production
     *    (stack trace, message d'erreur interne) pour des raisons
     *    de sécurité.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        // Log l'erreur en interne (important pour le debugging)
        ex.printStackTrace();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        "INTERNAL_ERROR",
                        "Une erreur interne est survenue",
                        request.getRequestURI()
                ));
    }
}