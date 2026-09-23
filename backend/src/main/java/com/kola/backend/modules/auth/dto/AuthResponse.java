package com.kola.backend.modules.auth.dto;

import com.kola.backend.modules.user.dto.UserResponse;

public record AuthResponse(String accessToken,
                           String refreshToken,
                           String tokenType,
                           long expiresIn,
                           UserResponse user) {

    public static AuthResponse of(String accessToken, String refreshToken, long expiresIn, UserResponse user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
