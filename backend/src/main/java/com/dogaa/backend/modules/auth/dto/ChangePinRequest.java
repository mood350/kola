package com.dogaa.backend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePinRequest(
        @NotBlank String currentPin,
        @NotBlank @Pattern(regexp = "[0-9]{4,6}", message = "PIN must be 4 to 6 digits") String newPin,
        @NotBlank String confirmPin) {
}
