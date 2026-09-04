package com.dogaa.backend.modules.admin.dto;

import com.dogaa.backend.modules.admin.entity.AdminRole;

/** Shape consumed as-is by the back-office (BACKEND.md 4.1). `role` serialises to its label. */
public record AdminIdentity(String id,
                            String name,
                            String email,
                            AdminRole role,
                            String scope) {
}
