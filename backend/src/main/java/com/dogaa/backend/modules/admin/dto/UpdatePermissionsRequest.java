package com.dogaa.backend.modules.admin.dto;

import com.dogaa.backend.modules.admin.entity.AdminRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePermissionsRequest(@NotNull AdminRole role,
                                       @Size(max = 200) String scope) {
}
