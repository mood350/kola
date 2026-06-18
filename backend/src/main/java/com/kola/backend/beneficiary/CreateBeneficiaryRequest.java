package com.kola.backend.beneficiary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateBeneficiaryRequest(
        @NotBlank(message = "L'alias est obligatoire")
        String alias,

        @NotBlank(message = "Le numéro de téléphone est obligatoire")
        @Pattern(
                regexp = "^\\+[1-9]\\d{6,14}$",
                message = "Le numéro de téléphone doit être au format international, ex: +221770000000"
        )
        String phoneNumber,

        @NotBlank(message = "Le code pays est obligatoire")
        @Pattern(regexp = "^[A-Z]{2}$", message = "Le code pays doit être au format ISO 3166-1 alpha-2, ex: SN")
        String countryCode,

        @NotNull(message = "Le réseau Mobile Money est obligatoire")
        MobileNetwork network
) {
}
