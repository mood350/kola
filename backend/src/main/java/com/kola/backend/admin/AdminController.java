package com.kola.backend.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kola.backend.user.User;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminAnalyticsService analyticsService;

    @GetMapping("/overview")
    public AdminOverviewResponse overview(@AuthenticationPrincipal User currentUser) {
        // SÉCURITÉ : On vérifie explicitement que l'utilisateur a le rôle ADMIN
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ADMIN")); // Assure-toi que le roleName dans la BDD est bien "ADMIN"

        if (!isAdmin) {
            throw new AccessDeniedException("Accès refusé : réservé aux administrateurs");
        }

        return analyticsService.overview();
    }
}