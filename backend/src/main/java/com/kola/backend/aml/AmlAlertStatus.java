package com.kola.backend.aml;

/**
 * Cycle de vie d'une alerte côté analyste conformité.
 * CONFIRMED = soupçon retenu, à transmettre à la cellule de renseignement
 * financier (CENTIF dans l'espace UEMOA).
 */
public enum AmlAlertStatus {
    OPEN,
    REVIEWING,
    CLEARED,
    CONFIRMED
}
