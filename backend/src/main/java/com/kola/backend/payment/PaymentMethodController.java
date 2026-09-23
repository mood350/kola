package com.kola.backend.payment;

import com.kola.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * Opérateurs Mobile Money disponibles.
 *
 * ═══ POURQUOI CETTE ROUTE EXISTE ═══
 *
 * Pour que la liste ne soit écrite qu'une fois. Sans elle, chaque client
 * recopierait les neuf opérateurs et leurs codes — `mtn_open` pour MTN Bénin,
 * `sbin` pour Celtis — qui ne se devinent pas et que FedaPay peut faire
 * évoluer. Trois copies, trois occasions de diverger, et un dépôt qui échoue en
 * 404 sur un seul des clients.
 *
 * ═══ LE FILTRE PAR PAYS EST UN DÉFAUT, PAS UNE CONTRAINTE ═══
 *
 * Sans paramètre, la liste est celle du pays de l'utilisateur : c'est de son
 * propre compte Mobile Money qu'il recharge, et lui proposer MTN Guinée depuis
 * Lomé n'a aucun sens. Mais le paramètre `country` reste ouvert, et une liste
 * vide retombe sur l'ensemble : quelqu'un dont le pays n'est pas couvert doit
 * voir quelque chose plutôt qu'un écran vide.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final FedaPayProperties properties;

    @GetMapping("/methods")
    public ResponseEntity<PaymentMethodsResponse> methods(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) String country
    ) {
        String wanted = country != null && !country.isBlank()
                ? country.toUpperCase()
                : currentUser.getCountryCode();

        List<PaymentMethod> forCountry = Arrays.stream(MobileMoneyMode.values())
                .filter(mode -> mode.getCountryCode().equalsIgnoreCase(wanted))
                .map(PaymentMethod::from)
                .toList();

        List<PaymentMethod> all = Arrays.stream(MobileMoneyMode.values())
                .map(PaymentMethod::from)
                .toList();

        return ResponseEntity.ok(new PaymentMethodsResponse(
                forCountry.isEmpty() ? all : forCountry,
                all,
                wanted,
                /* L'interface a besoin de le savoir AVANT d'afficher un choix
                   d'opérateurs : sans configuration, seul le dépôt de test est
                   possible, et proposer MTN pour le refuser ensuite serait une
                   promesse en l'air. */
                properties.isUsable(),
                /* Où le client validera : sur son téléphone, ou sur une page de
                   paiement. L'écran l'annonce AVANT la saisie — dire « validez
                   sur votre téléphone » puis ouvrir un onglet est le genre
                   d'incohérence qui fait abandonner un dépôt en cours. */
                properties.isDirectCharge(),
                /* Les retraits s'ouvrent séparément des encaissements chez
                   FedaPay. L'écran de retrait doit pouvoir le dire au lieu de
                   laisser remplir un formulaire qui finira en 503. */
                properties.isPayoutUsable()
        ));
    }

    /** Un opérateur, tel que l'affiche un client. */
    public record PaymentMethod(String code, String label, String countryCode) {
        static PaymentMethod from(MobileMoneyMode mode) {
            return new PaymentMethod(mode.name(), mode.getLabel(), mode.getCountryCode());
        }
    }

    /**
     * @param available    opérateurs du pays retenu — ce que l'écran propose
     * @param all          tous les opérateurs, pour un changement de pays
     * @param countryCode  pays effectivement appliqué
     * @param providerEnabled vrai si un vrai paiement est possible
     * @param directCharge    vrai si la demande de débit part sur le téléphone ;
     *                        faux si le client règle sur une page hébergée
     * @param withdrawalEnabled vrai si un retrait vers Mobile Money peut aboutir
     */
    public record PaymentMethodsResponse(
            List<PaymentMethod> available,
            List<PaymentMethod> all,
            String countryCode,
            boolean providerEnabled,
            boolean directCharge,
            boolean withdrawalEnabled
    ) {}
}
