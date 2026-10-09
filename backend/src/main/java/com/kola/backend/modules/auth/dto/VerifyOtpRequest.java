package com.kola.backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Step 2: the code received by SMS. */
public record VerifyOtpRequest(
        @NotBlank @Size(max = 20) String phone,
        @NotBlank @Pattern(regexp = "[0-9]{4,8}", message = "The code is numeric") String code) {
}
