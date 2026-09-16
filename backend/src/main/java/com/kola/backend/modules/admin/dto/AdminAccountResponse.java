package com.kola.backend.modules.admin.dto;

import com.kola.backend.modules.admin.entity.AdminRole;

/** The roles screen lists staff with their initials (BACKEND.md 12). */
public record AdminAccountResponse(String id,
                                   String initials,
                                   String name,
                                   AdminRole role,
                                   String scope) {
}
