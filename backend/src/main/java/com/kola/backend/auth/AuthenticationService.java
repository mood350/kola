package com.kola.backend.auth;

import com.kola.backend.email.EmailService;
import com.kola.backend.email.EmailTemplateName;
import com.kola.backend.notification.NotificationService;
import com.kola.backend.notification.NotificationType;
import com.kola.backend.exception.InvalidRefreshTokenException;
import com.kola.backend.exception.InvalidTokenException;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
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
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;
    private final RateLimitingService rateLimitingService;
    private final NotificationService notificationService;

    @Value("${application.mail.from}")
    private String from;

    @Value("${application.mailing.frontend.activation-url}")
    private String activationUrl;

    // Nombre de tentatives échouées consécutives avant verrouillage du compte
    private static final int MAX_FAILED_ATTEMPTS = 5;
    // Durée du verrouillage automatique avant déblocage
    private static final long LOCK_DURATION_MINUTES = 30;

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
                "Confirmation de votre compte Kola",
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

        // Un code recopie de travers est une SAISIE A CORRIGER, pas une panne :
        // une RuntimeException nue tombait dans le gestionnaire de repli et
        // renvoyait 500, que le client ne peut traiter que comme une erreur
        // serveur (cf. InvalidTokenException).
        Token token = tokenRepository.findValidToken(tokenValue, TokenType.ACTIVATION, LocalDateTime.now())
                .orElseThrow(InvalidTokenException::new);

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

    // dontRollbackOn est INDISPENSABLE ici, pas une optimisation.
    //
    // Les échecs d'authentification de Spring Security (BadCredentialsException,
    // LockedException, DisabledException...) héritent tous de RuntimeException :
    // sans cette clause, les lever déclenchait le rollback de TOUTE la
    // transaction — y compris le `userRepository.save(user)` de
    // registerFailedAttempt(). Le compteur failedLoginAttempts revenait donc
    // systématiquement à sa valeur d'avant la tentative et n'atteignait jamais
    // MAX_FAILED_ATTEMPTS : le verrouillage automatique du compte ne s'est
    // jamais déclenché, quel que soit le nombre de mots de passe essayés.
    // Le déverrouillage automatique de checkAndAutoUnlockIfExpired() était
    // perdu de la même façon quand le compte s'avérait ensuite désactivé.
    //
    // Les seules écritures du chemin d'échec sont ce compteur et le
    // verrou/déverrou du compte : les committer est exactement l'effet voulu.
    @Transactional(dontRollbackOn = AuthenticationException.class)
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

        // Vérifie si le compte était verrouillé et si le délai est écoulé
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
            notificationService.notify(
                    user,
                    "Nouvelle connexion détectée",
                    "Une connexion depuis un nouvel appareil ou une nouvelle adresse IP a été détectée.",
                    NotificationType.SECURITY
            );
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
     * compte si le seuil est atteint.
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
     * est écoulé.
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

        // ═══ ADRESSE INCONNUE : ON RÉPOND COMME SI DE RIEN N'ÉTAIT ═══
        //
        // Cette recherche levait une RuntimeException, donc un 500, là où une
        // adresse connue renvoyait 202. L'écart de statut suffisait à savoir
        // qui est client de Kola : il suffisait d'essayer des adresses. Sur un
        // service financier, cette liste a de la valeur — pour du hameçonnage
        // ciblé, notamment.
        //
        // On sort donc silencieusement, avec le MÊME statut et le même délai
        // apparent (l'envoi d'email est de toute façon asynchrone). Le message
        // affiché côté client — « si un compte existe pour cette adresse, un
        // code vient d'y être envoyé » — devient enfin exact.
        //
        // Le quota par adresse est consommé AVANT cette sortie, volontairement :
        // sans quoi une adresse inconnue serait un test gratuit et illimité.
        var maybeUser = userRepository.findByEmail(email);
        if (maybeUser.isEmpty()) {
            // Sans l'adresse : ces journaux sont conservés, et la liste des
            // adresses sondées est précisément ce qu'on refuse de constituer.
            log.info("Demande de réinitialisation pour une adresse inconnue — ignorée en silence");
            return;
        }

        User user = maybeUser.get();

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

        Token token = tokenRepository.findValidToken(tokenValue, TokenType.PASSWORD_RESET, LocalDateTime.now())
                .orElseThrow(InvalidTokenException::new);

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(token);
    }

    // ═══════════════════════════════════════════════════════════════
    //  REFRESH TOKEN
    // ═══════════════════════════════════════════════════════════════

    /**
     * Génère un nouvel access token à partir du refresh token.
     */
    public AuthenticationResponse refreshToken(String refreshToken, String ipAddress) {
        rateLimitingService.consume(RateLimitPolicy.REFRESH_TOKEN, ipAddress);

        final String userEmail = jwtService.extractUsername(refreshToken);

        // Les trois refus ci-dessous disent la même chose au client — « cette
        // session ne vaut plus rien » — et méritent donc le même 401. En
        // RuntimeException nue, ils tombaient dans le gestionnaire de repli et
        // répondaient 500 : une session expirée, cas parfaitement ordinaire,
        // était présentée comme une panne serveur (cf. InvalidRefreshTokenException).
        if (userEmail == null) {
            throw new InvalidRefreshTokenException();
        }

        // Un jeton lisible dont le porteur n'existe plus n'authentifie personne.
        // Répondre « utilisateur introuvable » confirmerait en prime la
        // suppression du compte à qui détient le jeton.
        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (!jwtService.isRefreshTokenValid(refreshToken, user)) {
            throw new InvalidRefreshTokenException();
        }

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
     * (Stateless : le token reste valide côté serveur jusqu'à expiration.
     *  La vraie déconnexion se fait via le nettoyage du SecurityContext).
     */
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }

        SecurityContextHolder.clearContext();
    }

    // ═══════════════════════════════════════════════════════════════
    //  UTILITAIRES PRIVÉS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Génère un OTP à 6 chiffres sécurisé et le sauvegarde en BDD.
     */
    private String generateAndSaveToken(User user, TokenType type) {
        String otp = String.format("%06d", new SecureRandom().nextInt(999999));

        Token token = Token.builder()
                .token(otp)
                .tokenType(type)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .user(user)
                .build();

        tokenRepository.save(token);
        return otp;
    }

    /**
     * Détecte si la connexion vient d'un nouvel appareil/IP.
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
                "Bienvenue sur Kola ! 🎉",
                EmailTemplateName.WELCOME,
                properties,
                from
        );
    }

    private void sendNewDeviceEmail(User user, String ip, String userAgent) throws MessagingException {
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