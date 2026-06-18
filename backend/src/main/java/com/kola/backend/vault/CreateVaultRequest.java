package com.kola.backend.vault;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateVaultRequest(
        @NotNull(message = "Le wallet source est obligatoire")
        Long walletId,

        @NotBlank(message = "Le nom du coffre est obligatoire")
        String name,

        String purpose,

        BigDecimal targetAmount,

        @NotNull(message = "Le montant initial est obligatoire")
        @DecimalMin(value = "0", message = "Le montant initial ne peut pas être négatif")
        BigDecimal initialAmount,

        @Future(message = "La date de déblocage doit être dans le futur")
        LocalDate unlockDate
) {
}
