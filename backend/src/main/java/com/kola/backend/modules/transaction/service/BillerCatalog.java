package com.kola.backend.modules.transaction.service;

import com.kola.backend.common.enums.Biller;
import com.kola.backend.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Checks a subscriber identifier against the biller it is meant for, and answers what the app
 * should ask the user for.
 *
 * <p>The validation is intentionally shallow — see {@link Biller} for why. It catches the mistakes
 * that are certainly mistakes (an empty box, a phone number typed into a meter-number field, a
 * Canal+ card that is not 14 digits) and lets everything else through to the biller, which is the
 * only party that actually knows whether an account exists.
 */
@Component
public class BillerCatalog {

    /** Everything payable, for the biller picker. */
    public List<Biller> all() {
        return Arrays.asList(Biller.values());
    }

    /** The subset a user may put on a monthly schedule: those whose amount does not change. */
    public List<Biller> schedulable() {
        return Arrays.stream(Biller.values()).filter(Biller::isFixedAmount).toList();
    }

    /**
     * Validates and normalises an identifier.
     *
     * <p>Separators are stripped from digit-only references before checking the length: people copy
     * a meter number off a bill as "1234 5678 9012", and rejecting that teaches them to distrust
     * the form rather than to fix anything.
     *
     * @return the identifier as it should be stored and sent to the biller
     */
    public String normaliseIdentifier(Biller biller, String rawIdentifier) {
        if (rawIdentifier == null || rawIdentifier.isBlank()) {
            throw new BadRequestException(biller.getIdentifierLabel() + " est obligatoire");
        }

        String identifier = rawIdentifier.strip();
        if (biller.getIdentifierKind() == Biller.IdentifierKind.DIGITS) {
            identifier = identifier.replaceAll("[\\s.-]", "");
        }

        if (!biller.getIdentifierKind().matches(identifier)) {
            throw new BadRequestException(
                    biller.getDisplayName() + " : " + biller.getIdentifierKind().message());
        }

        if (identifier.length() < biller.getMinLength() || identifier.length() > biller.getMaxLength()) {
            throw new BadRequestException(expectedLengthMessage(biller));
        }
        return identifier;
    }

    /** Refuses to schedule a bill whose amount is not the same every month. */
    public void requireSchedulable(Biller biller) {
        if (!biller.isFixedAmount()) {
            throw new BadRequestException(biller.getDisplayName()
                    + " se facture à la consommation : le montant change chaque mois, il ne peut"
                    + " pas être programmé pour un montant fixe.");
        }
    }

    private static String expectedLengthMessage(Biller biller) {
        String label = biller.getDisplayName() + " : " + biller.getIdentifierLabel().toLowerCase();
        if (biller.getMinLength() == biller.getMaxLength()) {
            return label + " compte " + biller.getMinLength() + " chiffres";
        }
        return label + " doit compter entre " + biller.getMinLength()
                + " et " + biller.getMaxLength() + " caractères";
    }
}
