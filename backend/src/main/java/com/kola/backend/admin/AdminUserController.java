package com.kola.backend.admin;

import com.kola.backend.admin.AdminUserDtos.AdminUserDetail;
import com.kola.backend.admin.AdminUserDtos.AdminUserSummary;
import com.kola.backend.admin.AdminUserDtos.LockAccountRequest;
import com.kola.backend.admin.AdminUserDtos.UpdateKycRequest;
import com.kola.backend.user.KycLevel;
import com.kola.backend.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Console d'administration des comptes.
 *
 * SÉCURITÉ : monté sous `/api/admin/**`, où `SecurityConfig` exige déjà
 * l'autorité ADMIN pour toute requête. Aucune vérification de rôle n'est donc
 * répétée dans les méthodes — le faire donnerait l'impression que la règle de
 * chemin est facultative, et le jour où quelqu'un ajoute un endpoint sans la
 * recopier, il le croirait protégé par une convention qui n'existe pas. La
 * règle est au même endroit pour tout le monde : `SecurityConfig`.
 *
 * `@AuthenticationPrincipal User` identifie l'ADMINISTRATEUR qui agit, jamais
 * l'utilisateur administré — celui-ci est désigné par l'identifiant dans
 * l'URL. La distinction est ce qui rend les journaux d'action exploitables.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * Liste paginée et filtrable.
     *
     * Tri par défaut sur la date de création décroissante : la question la plus
     * fréquente devant une console de support est « qui vient d'arriver », pas
     * « qui est arrivé en premier ».
     */
    @GetMapping
    public ResponseEntity<Page<AdminUserSummary>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) KycLevel kycLevel,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Boolean locked,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(adminUserService.search(q, kycLevel, enabled, locked, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserDetail> detail(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.detail(id));
    }

    /**
     * `PATCH` et non `PUT` : on modifie un seul attribut d'une ressource dont
     * le reste est laissé intact. Un `PUT` engagerait à remplacer la
     * représentation entière.
     */
    @PatchMapping("/{id}/kyc")
    public ResponseEntity<AdminUserSummary> updateKyc(
            @AuthenticationPrincipal User actor,
            @PathVariable Long id,
            @RequestBody @Valid UpdateKycRequest request
    ) {
        return ResponseEntity.ok(
                adminUserService.updateKycLevel(id, request.kycLevel(), request.reason(), actor));
    }

    /** Verrouillage administratif : il ne s'auto-lève pas (cf. AdminUserService). */
    @PostMapping("/{id}/lock")
    public ResponseEntity<AdminUserSummary> lock(
            @AuthenticationPrincipal User actor,
            @PathVariable Long id,
            @RequestBody @Valid LockAccountRequest request
    ) {
        return ResponseEntity.ok(adminUserService.lock(id, request.reason(), actor));
    }

    /**
     * Le déverrouillage n'exige pas de motif : c'est l'action qui REND l'accès,
     * elle ne restreint rien. Imposer une justification pour rétablir un
     * service ajouterait une friction là où l'erreur coûteuse est de ne pas
     * agir assez vite.
     */
    @PostMapping("/{id}/unlock")
    public ResponseEntity<AdminUserSummary> unlock(
            @AuthenticationPrincipal User actor,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(adminUserService.unlock(id, actor));
    }
}
