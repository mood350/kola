package com.kola.backend.modules.admin.dto;

public record AdminLoginResponse(String token, AdminIdentity user) {
}
