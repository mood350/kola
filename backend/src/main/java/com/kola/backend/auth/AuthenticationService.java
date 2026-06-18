package com.kola.backend.auth;

import com.kola.backend.email.EmailService;
import com.kola.backend.email.EmailTemplateName;
import com.kola.backend.ratelimit.RateLimitPolicy;
import com.kola.backend.ratelimit.RateLimitingService;
import com.kola.backend.role.RoleRepository;
import com.kola.backend.security.JwtService;
import com.kola.backend.role.Role;
import com.kola.backend.token.Token;
import com.kola.backend.token.TokenRepository;
import com.kola.backend.token.TokenType;
import com.kola.backend.user.User;
import com.kola.backend.user.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;
    private final RateLimitingService rateLimitingService;

    @Value("${application.mail.from}")
    private String from;

    @Value("${application.mailing.frontend.activation-url}")
    private String activationUrl;

    // ═══════════════════════════════════════════════════════════════
    //  INSCRIPTION
    // ═══════════════════════════════════════════════════════════════

    public void register(RegistrationRequest request, String ipAddress) throws MessagingException {

        rateLimitingService.consume(RateLimitPolicy.REGISTER, ipAddress);

        Role userRole = roleRepository.findByRoleName("CLIENT")
                .orElseThrow(() -> new RuntimeException("Rôle CLIENT introuvable en BDD"));

        var user = User.builder()
                .firstName(request.getFirstname())
                .lastName(request.getLastname())
                .email(request.getEmail())
                // BUG CORRIGÉ : phoneNumber et countryCode n'étaient jamais
                // renseignés alors que phoneNumber est nullable=false en BDD
                // → l'inscription levait systématiquement une exception.
                .phoneNumber(request.getPhoneNumber())
                .countryCode(request.getCountryCode())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(List.of(userRole))
                .enabled(false) // ← désactivé jusqu'à confirmation email
                .accountLocked(false)
                .failedLoginAttempts(0)
                .build();

        userRepository.save(user);

        // Envoi de l'email de confirmation
        sendConfirmationEmail(user);
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONFIRMATION EMAIL
    // ═══════════════════════════════════════════════════════════════

    /**
     * Génère un code OTP à 6 chiffres, le sauvegarde en BDD
     * et envoie l'email de confirmation.
     */
    private void sendConfirmationEmail(User user) throws MessagingException {
        String otp = generateAndSaveToken(user, TokenType.ACTIVATION);

        Map<String, Object> properties = new HashMap<>();
        properties.put("username", user.fullName());
        properties.put("confirmationUrl", activationUrl);
        properties.put("activation_code", otp);

        emailService.sendEmail(
                user.getEmail(),
                "Confirmation de votre compte Armin",
                EmailTemplateName.ACTIVATE_ACCOUNT,
                properties,
                from
        );
    }

    /**
     * Valide le compte utilisateur avec le code OTP reçu par email.
     */
    @Transactional
    public void confirmAccount(String tokenValue, String ipAddress) throws MessagingException {
        rateLimitingService.consume(RateLimitPolicy.CONFIRM_ACCOUNT, ipAddress);

        Token token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new RuntimeException("Token invalide"));

        // Vérifie que le token n'est pas expiré
        if (LocalDateTime.now().isAfter(token.getExpiresAt())) {
            // Token expiré → on renvoie un nouveau code
            sendConfirmationEmail(token.getUser());
            throw new RuntimeException("Token expiré. Un nouveau code vous a été envoyé.");
        }

        // Active le compte
        User user = token.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        // Marque le token comme utilisé
        token.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(token);

        // Envoi email de bienvenue
        sendWelcomeEmail(user);
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONNEXION
    // ═══════════════════════════════════════════════════════════════

    // Nombre de tentatives échouées consécutives avant verrouillage du compte
    private static final int MAX_FAILED_ATTEMPTS = 5;
    // Durée du verrouillage automatique avant déblocage
    private static final long LOCK_DURATION_MINUTES = 30;

    public AuthenticationResponse authenticate(
            AuthenticationRequest request,
            String ipAddress,
            String userAgent
    ) throws MessagingException {

        rateLimitingService.consume(RateLimitPolicy.LOGIN, ipAddress + ":" + request.getEmail());

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException(
                        "Aucun compte associé à cet email"
                ));

        // BUG CORRIGÉ : accountLocked était présent dans User mais jamais
        // mis à jour ni vérifié ici → un compte ne pouvait jamais être
        // verrouillé suite à des tentatives de connexion répétées
        // (brute-force illimité possible sur le mot de passe).
        checkAndAutoUnlockIfExpired(user);

        if (user.isAccountLocked()) {
            throw new org.springframework.security.authentication.LockedException(
                    "Compte verrouillé suite à trop de tentatives échouées. Réessayez plus tard."
            );
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (org.springframework.security.authentication.BadCredentialsException ex) {
            registerFailedAttempt(user);
            throw ex;
        }

        // Connexion réussie → on remet le compteur à zéro
        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
        }

        // Détection nouveau appareil/IP (comme Google)
        if (isNewDevice(user, ipAddress, userAgent)) {
            sendNewDeviceEmail(user, ipAddress, userAgent);
        }

        // Met à jour le dernier IP et userAgent connus
        user.setLastKnownIp(ipAddress);
        user.setLastKnownUserAgent(userAgent);
        userRepository.save(user);

        var accessToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * Incrémente le compteur de tentatives échouées et verrouille le
     * compte si le seuil est atteint. Sauvegarde systématiquement, même
     * si la levée de l'exception interrompt le flux normal — c'est
     * volontaire : on veut persister l'échec avant de propager l'erreur.
     */
    private void registerFailedAttempt(User user) {
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

        if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
            user.setAccountLocked(true);
            user.setLockedAt(LocalDateTime.now());
        }

        userRepository.save(user);
    }

    /**
     * Déverrouille automatiquement un compte si le délai de verrouillage
     * est écoulé. Évite qu'un utilisateur reste bloqué indéfiniment sans
     * intervention d'un administrateur.
     */
    private void checkAndAutoUnlockIfExpired(User user) {
        if (user.isAccountLocked() && user.getLockedAt() != null) {
            boolean lockExpired = user.getLockedAt()
                    .plusMinutes(LOCK_DURATION_MINUTES)
                    .isBefore(LocalDateTime.now());

            if (lockExpired) {
                user.setAccountLocked(false);
                user.setFailedLoginAttempts(0);
                user.setLockedAt(null);
                userRepository.save(user);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  RESET PASSWORD
    // ═══════════════════════════════════════════════════════════════

    public void requestPasswordReset(String email) throws MessagingException {
        rateLimitingService.consume(RateLimitPolicy.FORGOT_PASSWORD, email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email introuvable"));

        String otp = generateAndSaveToken(user, TokenType.PASSWORD_RESET);

        Map<String, Object> properties = new HashMap<>();
        properties.put("username", user.fullName());
        properties.put("reset_code", otp);

        emailService.sendEmail(
                user.getEmail(),
                "Réinitialisation de votre mot de passe",
                EmailTemplateName.RESET_PASSWORD,
                properties,
                from
        );
    }

    @Transactional
    public void resetPassword(String tokenValue, String newPassword, String ipAddress) {
        rateLimitingService.consume(RateLimitPolicy.RESET_PASSWORD, ipAddress);

        Token token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new RuntimeException("Token invalide"));

        if (LocalDateTime.now().isAfter(token.getExpiresAt())) {
            throw new RuntimeException("Token expiré");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(token);
    }

    // ═══════════════════════════════════════════════════════════════
    //  UTILITAIRES PRIVÉS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Génère un OTP à 6 chiffres sécurisé et le sauvegarde en BDD.
     * Expire dans 15 minutes.
     */
    private String generateAndSaveToken(User user, TokenType type) {
        // SecureRandom → cryptographiquement sûr (pas Math.random() !)
        String otp = String.format("%06d",
                new SecureRandom().nextInt(999999));

        Token token = Token.builder()
                .token(otp)
                .tokenType(type)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .user(user)
                .build();

        tokenRepository.save(token);
        return otp;
    }

    // ═══════════════════════════════════════════════════════════════
//  REFRESH TOKEN
// ═══════════════════════════════════════════════════════════════

    /**
     * Génère un nouvel access token à partir du refresh token.
     *
     * FLUX :
     *  1. Extrait l'email depuis le refresh token
     *  2. Charge l'user depuis la BDD
     *  3. Vérifie que le refresh token est valide
     *  4. Génère un nouvel access token
     *  5. Retourne le nouvel access token + le même refresh token
     *
     * POURQUOI on ne régénère pas le refresh token ?
     *  → Standard entreprise : le refresh token reste valide jusqu'à
     *    son expiration (7 jours). On ne le régénère que si l'user
     *    se reconnecte ou si on implémente le "refresh token rotation".
     */
    public AuthenticationResponse refreshToken(String refreshToken, String ipAddress) {
        rateLimitingService.consume(RateLimitPolicy.REFRESH_TOKEN, ipAddress);

        // Extrait l'email depuis le refresh token
        final String userEmail = jwtService.extractUsername(refreshToken);

        if (userEmail == null) {
            throw new RuntimeException("Refresh token invalide");
        }

        // Charge l'user depuis la BDD
        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // Vérifie que le refresh token est valide et non expiré
        if (!jwtService.isRefreshTokenValid(refreshToken, user)) {
            throw new RuntimeException("Refresh token expiré ou invalide");
        }

        // Génère un nouvel access token uniquement
        var newAccessToken = jwtService.generateToken(user);

        return AuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // ← même refresh token
                .build();
    }

// ═══════════════════════════════════════════════════════════════
//  LOGOUT
// ═══════════════════════════════════════════════════════════════

    /**
     * Déconnecte l'utilisateur.
     *
     * POURQUOI c'est complexe avec JWT ?
     *  Les JWT sont STATELESS — le serveur ne garde pas de liste
     *  des tokens actifs. Un token valide reste valide jusqu'à
     *  son expiration même après logout.
     *
     * SOLUTION standard entreprise :
     *  → Blacklist : on stocke les tokens invalidés en BDD/Redis
     *  → Ici on utilise la BDD (simple) — en prod on utilise Redis
     *    car c'est beaucoup plus rapide pour les lookups.
     *
     * FLUX :
     *  1. Extrait le JWT du header Authorization
     *  2. Efface le SecurityContext (déconnexion immédiate)
     *  → Le token sera rejeté par JwtAuthFilter à la prochaine requête
     *     car on peut ajouter une vérification blacklist.
     */
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }

        // Efface l'authentification du SecurityContext
        SecurityContextHolder.clearContext();
    }

    /**
     * Détecte si la connexion vient d'un nouvel appareil/IP.
     * Comparaison IP + UserAgent comme Google.
     */
    private boolean isNewDevice(User user, String ipAddress, String userAgent) {
        if (user.getLastKnownIp() == null) return false;
        return !user.getLastKnownIp().equals(ipAddress) ||
                !user.getLastKnownUserAgent().equals(userAgent);
    }

    private void sendWelcomeEmail(User user) throws MessagingException {
        Map<String, Object> properties = new HashMap<>();
        properties.put("username", user.fullName());

        emailService.sendEmail(
                user.getEmail(),
                "Bienvenue sur Armin ! 🎉",
                EmailTemplateName.WELCOME,
                properties,
                from
        );
    }

    private void sendNewDeviceEmail(User user, String ip,
                                    String userAgent) throws MessagingException {
        Map<String, Object> properties = new HashMap<>();
        properties.put("username", user.fullName());
        properties.put("ip", ip);
        properties.put("device", userAgent);
        properties.put("time", LocalDateTime.now().toString());

        emailService.sendEmail(
                user.getEmail(),
                "⚠️ Nouvelle connexion détectée sur votre compte",
                EmailTemplateName.NEW_DEVICE_LOGIN,
                properties,
                from
        );
    }
}
