package com.kola.backend.admin;

import com.kola.backend.credit.CreditTier;
import com.kola.backend.credit.LoanStatus;
import com.kola.backend.user.KycLevel;
import com.kola.backend.user.User;
import com.kola.backend.vault.VaultStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Vues administrateur d'un utilisateur.
 *
 * POURQUOI DES DTO DÉDIÉS PLUTÔT QUE `UserResponse`. Le DTO client exclut
 * délibérément tout ce qui relève de l'administration — rôles, état du verrou,
 * compteur d'échecs de connexion (cf. le commentaire de `UserResponse`).
 * L'administrateur a précisément besoin de ces champs pour faire son travail :
 * il lui faut une vue distincte, pas un assouplissement de la vue client, qui
 * exposerait ces informations à tout le monde.
 *
 * Ce qui reste exclu ici, et doit le rester : le mot de passe, même haché. Un
 * hachage n'est pas un secret partageable — il se soumet à une attaque hors
 * ligne, et aucune tâche d'administration n'en a l'usage.
 */
public final class AdminUserDtos {

    private AdminUserDtos() {}

    /** Ligne de tableau : ce qui suffit à décider s'il faut ouvrir la fiche. */
    public record AdminUserSummary(
            Long id,
            String firstName,
            String lastName,
            String email,
            String phoneNumber,
            String countryCode,
            KycLevel kycLevel,
            boolean enabled,
            boolean accountLocked,
            int failedLoginAttempts,
            LocalDateTime lockedAt,
            List<String> roles,
            LocalDateTime createdAt
    ) {
        public static AdminUserSummary fromEntity(User user) {
            return new AdminUserSummary(
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.getPhoneNumber(),
                    user.getCountryCode(),
                    user.getKycLevel(),
                    user.isEnabled(),
                    user.isAccountLocked(),
                    user.getFailedLoginAttempts(),
                    user.getLockedAt(),
                    user.getRoles() == null
                            ? List.of()
                            : user.getRoles().stream().map(role -> role.getRoleName()).toList(),
                    user.getCreatedAt()
            );
        }
    }

    public record WalletSummary(
            Long id,
            String currency,
            BigDecimal balance,
            BigDecimal lockedBalance,
            boolean active
    ) {}

    public record VaultSummary(
            Long id,
            String name,
            String currency,
            BigDecimal currentAmount,
            BigDecimal targetAmount,
            LocalDate unlockDate,
            VaultStatus status
    ) {}

    public record CreditSummary(
            int score,
            CreditTier tier,
            BigDecimal maxLoanAmount,
            BigDecimal monthlyRate,
            LocalDateTime expiresAt,
            LocalDateTime computedAt
    ) {}

    public record LoanSummary(
            Long id,
            BigDecimal requestedAmount,
            BigDecimal totalRepayment,
            int durationMonths,
            LoanStatus status,
            LocalDate dueDate,
            LocalDateTime defaultedAt,
            LocalDateTime createdAt
    ) {}

    /**
     * Fiche complète.
     *
     * `credit` est nul tant qu'aucun score n'a été calculé pour ce compte — un
     * utilisateur fraîchement inscrit n'en a pas. Le front doit traiter ce cas,
     * il n'est ni exceptionnel ni transitoire.
     */
    public record AdminUserDetail(
            AdminUserSummary identity,
            String avatar,
            String lastKnownIp,
            String lastKnownUserAgent,
            List<WalletSummary> wallets,
            List<VaultSummary> vaults,
            CreditSummary credit,
            List<LoanSummary> loans
    ) {}

    /* ---------------------------------------------------------------------
       Requêtes de modification
       ------------------------------------------------------------------ */

    /**
     * Changement de niveau de vérification.
     *
     * Le motif est OBLIGATOIRE et contraint en longueur : relever un palier KYC
     * élargit mécaniquement les plafonds d'opération de l'utilisateur et pèse
     * 15 points sur son score de crédit. Une telle décision ne doit jamais
     * pouvoir être prise sans que son auteur ait à l'expliciter — c'est la
     * seule trace exploitable en cas de contrôle, faute d'un journal d'audit
     * persistant (cf. AdminUserService).
     */
    public record UpdateKycRequest(
            @NotNull(message = "Le niveau KYC est obligatoire")
            KycLevel kycLevel,

            @NotNull(message = "Le motif est obligatoire")
            @Size(min = 10, max = 500, message = "Le motif doit faire entre 10 et 500 caractères")
            String reason
    ) {}

    /** Verrouillage manuel d'un compte. Même exigence de motif. */
    public record LockAccountRequest(
            @NotNull(message = "Le motif est obligatoire")
            @Size(min = 10, max = 500, message = "Le motif doit faire entre 10 et 500 caractères")
            String reason
    ) {}
}
