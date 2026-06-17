package com.kola.backend.beneficiary;

/**
 * Réseau de Mobile Money supporté par Kola.
 * Utilisé pour identifier le réseau du bénéficiaire lors d'un transfert.
 */
public enum MobileNetwork {
    // Togo
    MIXX_BY_YAS,
    MOOV_TOGO,

    // Sénégal, Côte d'Ivoire, Mali, Burkina Faso
    WAVE,
    ORANGE_MONEY,
    FREE_MONEY,

    // Ghana
    MTN_MOMO,
    VODAFONE_CASH,
    AIRTELTIGO,

    // Nigeria
    OPAY,
    PALMPAY,

    // International
    WESTERN_UNION,
    MONEYGRAM
}
