package com.kola.backend.payment;

/**
 * Opérateurs Mobile Money encaissables sans redirection.
 *
 * Le code envoyé à FedaPay n'est PAS déduit du nom de l'opérateur : il EST le
 * chemin de l'appel ({@code POST /v1/{mode}}, à la racine de l'API) et n'a de
 * sens que tel quel. « MTN Bénin » s'appelle {@code mtn_open}, « Celtis »
 * s'appelle {@code sbin} — deviner mènerait à un 404 silencieux.
 *
 * CETTE LISTE N'EST PAS {@code MobileNetwork}. L'enum des bénéficiaires décrit
 * le réseau d'un DESTINATAIRE de virement (il inclut Western Union, MoneyGram,
 * PalmPay…) ; celle-ci décrit les canaux par lesquels FedaPay sait ENCAISSER.
 * Les fusionner ferait proposer à l'utilisateur de recharger son compte par
 * MoneyGram, ce qu'aucune API ne permet ici.
 *
 * Source : https://docs-v1.fedapay.com/payments/transactions (« paiements sans
 * redirection »). Ajouter un opérateur suppose de vérifier qu'il y figure.
 */
public enum MobileMoneyMode {

    MTN_BENIN("mtn_open", "MTN Bénin", "BJ"),
    MOOV_BENIN("moov", "Moov Bénin", "BJ"),
    CELTIS_BENIN("sbin", "Celtis Bénin", "BJ"),
    MOOV_TOGO("moov_tg", "Moov Togo", "TG"),
    TOGOCEL("togocel", "Togocel T-Money", "TG"),
    MTN_CI("mtn_ci", "MTN Côte d'Ivoire", "CI"),
    FREE_SENEGAL("free_sn", "Free Sénégal", "SN"),
    AIRTEL_NIGER("airtel_ne", "Airtel Niger", "NE"),
    MTN_GUINEE("mtn_open_gn", "MTN Guinée", "GN");

    private final String providerCode;
    private final String label;
    private final String countryCode;

    MobileMoneyMode(String providerCode, String label, String countryCode) {
        this.providerCode = providerCode;
        this.label = label;
        this.countryCode = countryCode;
    }

    /** Segment d'URL attendu par FedaPay. */
    public String getProviderCode() {
        return providerCode;
    }

    public String getLabel() {
        return label;
    }

    /** Pays ISO 3166-1 alpha-2, transmis avec le numéro de téléphone. */
    public String getCountryCode() {
        return countryCode;
    }
}
