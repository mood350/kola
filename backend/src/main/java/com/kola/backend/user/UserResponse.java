package com.kola.backend.user;

import java.time.LocalDateTime;

/**
 * DTO de sortie pour le profil utilisateur.
 * On n'exposera JAMAIS l'entité JPA User directement dans l'API :
 * pas de mot de passe, pas de rôles/authorities, pas de relations
 * wallets/vaults/beneficiaries (déjà exposées via leurs propres endpoints).
 */
public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String countryCode,
        KycLevel kycLevel,
        String avatar,
        String lastKnownIp,
        String lastKnownUserAgent,
        LocalDateTime createdAt
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getCountryCode(),
                user.getKycLevel(),
                user.getAvatar(),
                user.getLastKnownIp(),
                user.getLastKnownUserAgent(),
                user.getCreatedAt()
        );
    }
}
