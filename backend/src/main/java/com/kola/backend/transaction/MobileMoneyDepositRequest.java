package com.kola.backend.transaction;

import com.kola.backend.payment.MobileMoneyMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * Demande de rechargement depuis un compte Mobile Money.
 *
 * DIFFÉRENT DE {@code DepositRequest}, qui crédite le wallet immédiatement et
 * n'existe que pour le développement et les corrections manuelles. Ici, aucun
 * franc n'est crédité tant que l'opérateur n'a pas confirmé : l'écriture naît
 * en attente.
 *
 * Le pays n'est pas demandé — il découle de l'opérateur choisi
 * ({@link MobileMoneyMode#getCountryCode()}). Un champ de plus serait un champ
 * de plus à contredire : « MTN Côte d'Ivoire » avec un pays « TG » n'a aucun
 * sens, et rien n'empêcherait de le saisir.
 *
 * @param idempotencyKey même rôle que sur les autres opérations d'argent : un
 *        double envoi ne doit pas déclencher deux demandes de débit sur le
 *        téléphone du client.
 */
public record MobileMoneyDepositRequest(

        @NotNull(message = "Le wallet à créditer est obligatoire")
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
