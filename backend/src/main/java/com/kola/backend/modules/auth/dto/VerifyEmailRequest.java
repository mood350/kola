package com.kola.backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailRequest(
        @NotBlank @Pattern(regexp = "[0-9]{4,8}", message = "The code is numeric") String code) {
}
