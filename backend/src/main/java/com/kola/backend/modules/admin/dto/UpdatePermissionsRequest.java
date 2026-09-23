package com.kola.backend.modules.admin.dto;

import com.kola.backend.modules.admin.entity.AdminRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePermissionsRequest(@NotNull AdminRole role,
                                       @Size(max = 200) String scope) {
}
