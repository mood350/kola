package com.dogaa.backend.modules.admin.service;

import com.dogaa.backend.config.AuthProperties;
import com.dogaa.backend.common.util.RelativeTime;
import com.dogaa.backend.exception.AccountLockedException;
import com.dogaa.backend.exception.BadRequestException;
import com.dogaa.backend.exception.ResourceNotFoundException;
import com.dogaa.backend.exception.UnauthorizedException;
import com.dogaa.backend.modules.admin.dto.AdminAccountResponse;
import com.dogaa.backend.modules.admin.dto.AdminIdentity;
import com.dogaa.backend.modules.admin.dto.AdminLoginRequest;
import com.dogaa.backend.modules.admin.dto.AdminLoginResponse;
import com.dogaa.backend.modules.admin.dto.ChangePasswordRequest;
import com.dogaa.backend.modules.admin.dto.UpdatePermissionsRequest;
import com.dogaa.backend.modules.admin.entity.AdminAccount;
import com.dogaa.backend.modules.admin.entity.AdminRole;
import com.dogaa.backend.modules.admin.repository.AdminAccountRepository;
import com.dogaa.backend.modules.admin.security.CurrentAdmin;
import com.dogaa.backend.modules.audit.service.AuditService;
import com.dogaa.backend.modules.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Back-office sign-in and staff accounts (BACKEND.md 4 and 12).
 *
 * <p>Separate from customer authentication on purpose: an administrator signs in with an email and
 * a password, holds no wallet, and gets a session long enough to review a queue without being
 * logged out mid-decision.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAccountService {

    private static final String MODULE = "roles";

    private final AdminAccountRepository adminAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final AuthProperties authProperties;

    // --- sign-in ----------------------------------------------------------

    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request) {
        AdminAccount admin = adminAccountRepository.findByEmailIgnoreCase(request.email().trim())
                .orElse(null);

        // Hash even when the account does not exist, so a missing email and a wrong password take
        // the same time to answer. Otherwise the endpoint tells anyone which addresses are staff.
        boolean matches = admin != null
                && passwordEncoder.matches(request.password(), admin.getPasswordHash());
        if (admin == null) {
            burnEqualTime(request.password());
        }

        if (admin == null || !matches) {
            throw new UnauthorizedException("INVALID_CREDENTIALS", "Email ou mot de passe incorrect");
        }
        if (!admin.isEnabled()) {
            throw new AccountLockedException(Instant.now().plus(authProperties.getLockDuration()));
        }

        admin.setLastLoginAt(Instant.now());
        adminAccountRepository.save(admin);

        CurrentAdmin principal = toPrincipal(admin);
        auditService.record(principal, "auth", "Connexion au back-office", null,
                "AdminAccount", admin.getId().toString());

        log.info("Admin {} signed in ({})", admin.getEmail(), admin.getRole());
        return new AdminLoginResponse(jwtService.generateAdminToken(admin), toIdentity(admin));
    }

    @Transactional(readOnly = true)
    public AdminIdentity me(UUID adminId) {
        return toIdentity(getById(adminId));
    }

    @Transactional
    public void logout(CurrentAdmin admin) {
        // Admin tokens are stateless and short enough to expire on their own; the front-end drops
        // its copy. Recorded anyway, because a session ending is part of the trail.
        auditService.record(admin, "auth", "Déconnexion du back-office", null,
                "AdminAccount", admin.id().toString());
    }

    // --- password ---------------------------------------------------------

    @Transactional
    public void changePassword(CurrentAdmin currentAdmin, ChangePasswordRequest request) {
        AdminAccount admin = getById(currentAdmin.id());

        // Deliberately not a 401: the session is valid, only the typed field is wrong. The console
        // signs the user out on any 401 (BACKEND.md 3), so answering 401 here would throw the
        // administrator back to the login screen instead of showing them their typo.
        if (!passwordEncoder.matches(request.currentPassword(), admin.getPasswordHash())) {
            throw new BadRequestException("Mot de passe actuel incorrect");
        }
        if (request.newPassword().equals(request.currentPassword())) {
            throw new BadRequestException("Le nouveau mot de passe doit différer de l'actuel");
        }

        admin.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        adminAccountRepository.save(admin);

        auditService.record(currentAdmin, "profile", "Changement de mot de passe", null,
                "AdminAccount", admin.getId().toString());
        log.info("Admin {} changed their password", admin.getEmail());
    }

    /**
     * Always succeeds, whether or not the address belongs to anyone: answering differently would
     * turn this endpoint into a way to enumerate staff emails.
     */
    @Transactional(readOnly = true)
    public void requestPasswordReset(String email) {
        adminAccountRepository.findByEmailIgnoreCase(email.trim()).ifPresentOrElse(
                admin -> log.warn("Password reset requested for {} — no mail sender is wired, "
                        + "reset it manually", admin.getEmail()),
                () -> log.info("Password reset requested for an unknown address"));
    }

    // --- staff management (BACKEND.md 12) ---------------------------------

    @Transactional(readOnly = true)
    public List<AdminAccountResponse> listAdmins() {
        return adminAccountRepository.findAllByOrderByNameAsc().stream()
                .map(admin -> new AdminAccountResponse(
                        admin.getId().toString(), admin.getInitials(),
                        admin.getName(), admin.getRole(), admin.getScope()))
                .toList();
    }

    @Transactional
    public AdminAccountResponse updatePermissions(CurrentAdmin actor, UUID targetId,
                                                  UpdatePermissionsRequest request) {
        AdminAccount target = getById(targetId);
        AdminRole previousRole = target.getRole();

        if (actor.id().equals(targetId) && request.role() != previousRole) {
            // Otherwise the last super-admin can demote themselves and lock everyone out.
            throw new BadRequestException("Vous ne pouvez pas modifier votre propre rôle");
        }

        target.setRole(request.role());
        if (request.scope() != null) {
            target.setScope(request.scope());
        }
        adminAccountRepository.save(target);

        auditService.record(actor, MODULE,
                "Modification des permissions de " + target.getName(),
                AuditService.diff(previousRole.getLabel(), request.role().getLabel()),
                "AdminAccount", targetId.toString());

        return new AdminAccountResponse(target.getId().toString(), target.getInitials(),
                target.getName(), target.getRole(), target.getScope());
    }

    // --- helpers ----------------------------------------------------------

    private AdminAccount getById(UUID adminId) {
        return adminAccountRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte admin introuvable"));
    }

    private static AdminIdentity toIdentity(AdminAccount admin) {
        return new AdminIdentity(admin.getId().toString(), admin.getName(),
                admin.getEmail(), admin.getRole(), admin.getScope(),
                admin.getLastLoginAt() == null ? null : RelativeTime.since(admin.getLastLoginAt()));
    }

    private static CurrentAdmin toPrincipal(AdminAccount admin) {
        return new CurrentAdmin(admin.getId(), admin.getEmail(), admin.getName(), admin.getRole());
    }

    /**
     * Spends the same time hashing when the account does not exist. Named rather than inlined so
     * it does not look like a stray encode() someone could helpfully delete.
     */
    private void burnEqualTime(String password) {
        passwordEncoder.encode(password);
    }
}
