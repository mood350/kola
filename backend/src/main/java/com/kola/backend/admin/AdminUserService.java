package com.kola.backend.admin;

import com.kola.backend.admin.AdminUserDtos.AdminUserDetail;
import com.kola.backend.admin.AdminUserDtos.AdminUserSummary;
import com.kola.backend.admin.AdminUserDtos.CreditSummary;
import com.kola.backend.admin.AdminUserDtos.LoanSummary;
import com.kola.backend.admin.AdminUserDtos.VaultSummary;
import com.kola.backend.admin.AdminUserDtos.WalletSummary;
import com.kola.backend.credit.CreditScore;
import com.kola.backend.credit.CreditScoreRepository;
import com.kola.backend.credit.LoanRequestRepository;
import com.kola.backend.user.KycLevel;
import com.kola.backend.user.User;
import com.kola.backend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Administration des comptes utilisateurs.
 *
 * TRAÇABILITÉ — LIMITE CONNUE ET ASSUMÉE DE CETTE PASSE.
 *
 * Les deux actions de ce service (changement de palier KYC, verrouillage
 * manuel) sont des décisions à effet réglementaire : le palier KYC commande
 * les plafonds d'opération, et le verrou coupe l'accès aux fonds. Elles
 * devraient être consignées dans une table d'audit — qui a fait quoi, quand,
 * pour quel motif — consultable et non modifiable.
 *
 * Cette table n'existe pas ici : le schéma appartient aux migrations Flyway
 * (`ddl-auto=validate`), et l'ajouter dépasse le périmètre de cette passe. En
 * attendant, chaque action est journalisée au niveau WARN avec l'e-mail de
 * l'administrateur agissant et le motif qu'il a été obligé de fournir. C'est
 * une trace réelle, mais elle vit dans les journaux applicatifs : ni requêtable
 * depuis l'interface, ni protégée contre la rotation des logs. C'est le premier
 * chantier à ouvrir après celui-ci.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final CreditScoreRepository creditScoreRepository;
    private final LoanRequestRepository loanRequestRepository;

    /* ---------------------------------------------------------------------
       Lecture
       ------------------------------------------------------------------ */

    /**
     * Recherche filtrée et paginée.
     *
     * Chaque filtre absent n'ajoute AUCUN prédicat : la Specification n'est
     * composée que des critères réellement fournis. C'est ce qui évite de lier
     * un paramètre nul, que PostgreSQL refuse de typer (cf. le commentaire de
     * `UserRepository`).
     *
     * La recherche textuelle porte sur les quatre champs par lesquels un
     * administrateur identifie quelqu'un en pratique : prénom, nom, e-mail,
     * téléphone. Elle est insensible à la casse et cherche en sous-chaîne — un
     * support qui n'a qu'un fragment de numéro doit pouvoir retrouver le
     * compte.
     */
    @Transactional(readOnly = true)
    public Page<AdminUserSummary> search(
            String query,
            KycLevel kycLevel,
            Boolean enabled,
            Boolean locked,
            Pageable pageable
    ) {
        Specification<User> spec = (root, criteriaQuery, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("phoneNumber")), pattern)
                ));
            }
            if (kycLevel != null) {
                predicates.add(cb.equal(root.get("kycLevel"), kycLevel));
            }
            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }
            if (locked != null) {
                predicates.add(cb.equal(root.get("accountLocked"), locked));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return userRepository.findAll(spec, pageable).map(AdminUserSummary::fromEntity);
    }

    /**
     * Fiche complète d'un compte.
     *
     * `@Transactional` n'est pas décoratif ici : `open-in-view` est désactivé,
     * et les collections `wallets` / `vaults` sont LAZY. Hors transaction, leur
     * lecture lèverait une LazyInitializationException — la projection en DTO
     * doit donc se faire À L'INTÉRIEUR de cette méthode, jamais après son
     * retour.
     */
    @Transactional(readOnly = true)
    public AdminUserDetail detail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable : " + userId));

        List<WalletSummary> wallets = user.getWallets() == null ? List.of() : user.getWallets().stream()
                .map(wallet -> new WalletSummary(
                        wallet.getId(),
                        wallet.getCurrency(),
                        wallet.getBalance(),
                        wallet.getLockedBalance(),
                        wallet.isActive()))
                .toList();

        List<VaultSummary> vaults = user.getVaults() == null ? List.of() : user.getVaults().stream()
                .map(vault -> new VaultSummary(
                        vault.getId(),
                        vault.getName(),
                        vault.getCurrency(),
                        vault.getCurrentAmount(),
                        vault.getTargetAmount(),
                        vault.getUnlockDate(),
                        vault.getStatus()))
                .toList();

        CreditSummary credit = creditScoreRepository.findByUserIdAndLatestTrue(userId)
                .map(AdminUserService::toCreditSummary)
                .orElse(null);

        List<LoanSummary> loans = loanRequestRepository
                .findByBorrowerIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(loan -> new LoanSummary(
                        loan.getId(),
                        loan.getRequestedAmount(),
                        loan.getTotalRepayment(),
                        loan.getDurationMonths(),
                        loan.getStatus(),
                        loan.getDueDate(),
                        loan.getDefaultedAt(),
                        loan.getCreatedAt()))
                .toList();

        return new AdminUserDetail(
                AdminUserSummary.fromEntity(user),
                user.getAvatar(),
                user.getLastKnownIp(),
                user.getLastKnownUserAgent(),
                wallets,
                vaults,
                credit,
                loans
        );
    }

    private static CreditSummary toCreditSummary(CreditScore score) {
        return new CreditSummary(
                score.getScore(),
                score.getTier(),
                score.getMaxLoanAmount(),
                score.getMonthlyRate(),
                score.getExpiresAt(),
                score.getCreatedAt()
        );
    }

    /* ---------------------------------------------------------------------
       Écriture
       ------------------------------------------------------------------ */

    /**
     * Change le palier de vérification d'un compte.
     *
     * Conséquences immédiates, à avoir en tête avant d'appeler : les plafonds
     * d'opération de l'utilisateur bougent, et son score de crédit changera au
     * prochain recalcul — le niveau de vérification y pèse 15 points sur 100.
     * Le score déjà calculé n'est PAS invalidé ici : il expire naturellement au
     * bout de trente jours, ou l'utilisateur force un recalcul. Purger le cache
     * de score depuis l'administration reviendrait à modifier silencieusement
     * la capacité d'emprunt de quelqu'un sans qu'il l'ait demandé.
     */
    @Transactional
    public AdminUserSummary updateKycLevel(Long userId, KycLevel newLevel, String reason, User actor) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable : " + userId));

        KycLevel previous = user.getKycLevel();
        if (previous == newLevel) {
            throw new IllegalStateException("Le compte est déjà au niveau " + newLevel + ".");
        }

        user.setKycLevel(newLevel);
        userRepository.save(user);

        log.warn("[ADMIN] KYC modifié — utilisateur={} ({}), {} -> {}, par={}, motif=\"{}\"",
                user.getId(), user.getEmail(), previous, newLevel, actor.getEmail(), reason);

        return AdminUserSummary.fromEntity(user);
    }

    /**
     * Verrouille un compte de façon administrative.
     *
     * POINT SUBTIL, ET C'EST LE CŒUR DE LA MÉTHODE : `lockedAt` est laissé à
     * NUL, délibérément.
     *
     * `AuthenticationService.checkAndAutoUnlockIfExpired()` déverrouille tout
     * compte dont le `lockedAt` remonte à plus de trente minutes. C'est le
     * comportement voulu pour le verrou AUTOMATIQUE, celui qui sanctionne cinq
     * mots de passe erronés : il doit s'effacer tout seul.
     *
     * Un verrou décidé par un administrateur n'a pas cette nature. Horodaté, il
     * s'ouvrirait de lui-même une demi-heure plus tard — un compte suspendu
     * pour soupçon de fraude redeviendrait accessible sans que personne ne
     * l'ait décidé. En laissant `lockedAt` nul, la condition `lockedAt != null`
     * de l'auto-déverrouillage n'est jamais vraie : le verrou tient jusqu'à ce
     * qu'un administrateur le lève. Aucune modification d'AuthenticationService
     * n'est nécessaire.
     */
    @Transactional
    public AdminUserSummary lock(Long userId, String reason, User actor) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable : " + userId));

        /* Se verrouiller soi-même expulserait l'administrateur de sa propre
           console, sans personne pour l'en sortir si c'est le seul compte
           ADMIN. Le cas est trop facile à déclencher par erreur pour être
           laissé ouvert. */
        if (user.getId().equals(actor.getId())) {
            throw new IllegalStateException("Vous ne pouvez pas verrouiller votre propre compte.");
        }

        if (user.isAccountLocked()) {
            throw new IllegalStateException("Ce compte est déjà verrouillé.");
        }

        user.setAccountLocked(true);
        user.setLockedAt(null);
        userRepository.save(user);

        log.warn("[ADMIN] Compte verrouillé — utilisateur={} ({}), par={}, motif=\"{}\"",
                user.getId(), user.getEmail(), actor.getEmail(), reason);

        return AdminUserSummary.fromEntity(user);
    }

    /**
     * Lève le verrou, qu'il soit administratif ou automatique.
     *
     * Le compteur d'échecs est remis à zéro dans la foulée : le laisser à cinq
     * ferait re-verrouiller le compte au premier mot de passe erroné suivant,
     * et l'utilisateur qu'on vient de débloquer se retrouverait bloqué sur une
     * simple faute de frappe.
     */
    @Transactional
    public AdminUserSummary unlock(Long userId, User actor) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable : " + userId));

        if (!user.isAccountLocked()) {
            throw new IllegalStateException("Ce compte n'est pas verrouillé.");
        }

        user.setAccountLocked(false);
        user.setLockedAt(null);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        log.warn("[ADMIN] Compte déverrouillé — utilisateur={} ({}), par={}, à={}",
                user.getId(), user.getEmail(), actor.getEmail(), LocalDateTime.now());

        return AdminUserSummary.fromEntity(user);
    }
}
