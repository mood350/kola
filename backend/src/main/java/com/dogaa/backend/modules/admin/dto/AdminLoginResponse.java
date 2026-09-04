package com.dogaa.backend.modules.admin.dto;

public record AdminLoginResponse(String token, AdminIdentity user) {
}
