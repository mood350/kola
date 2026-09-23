package com.kola.backend.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * Demande de retrait vers un compte Mobile Money.
 *
 * DIFFÉRENTE DE {@code WithdrawalRequest}, qui débite le portefeuille et
 * s'arrête là — ce retrait de test n'envoie l'argent nulle part et n'existe que
 * pour le développement. Ici, un versement réel part chez l'opérateur.
 *
 * Le pays découle de l'opérateur ({@link MobileMoneyMode#getCountryCode()}) :
 * un champ de plus serait un champ de plus à contredire.
 */
public record MobileMoneyWithdrawalRequest(

        @NotNull(message = "Le wallet à débiter est obligatoire")
        Long walletId,

        @NotNull(message = "Le montant est obligatoire")
        @DecimalMin(value = "1", message = "Le montant doit être supérieur à 0")
        BigDecimal amount,

        @NotNull(message = "L'opérateur Mobile Money est obligatoire")
        MobileMoneyMode mode,

        @NotBlank(message = "Le numéro de téléphone est obligatoire")
        @Pattern(
                regexp = "^\\+[1-9]\\d{6,14}$",
                message = "Le numéro de téléphone doit être au format international, ex: +22890000000"
        )
        String phoneNumber,

        String idempotencyKey
) {
}
