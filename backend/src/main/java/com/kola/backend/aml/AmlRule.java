package com.kola.backend.aml;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Typologies de blanchiment détectées par le moteur LAB-FT.
 * Chaque règle porte un poids de risque : la somme des règles déclenchées
 * donne le score de risque de l'alerte (plafonné à 100).
 *
 * Même philosophie que ScoringRule côté crédit : chaque déclenchement est
 * explicable en clair, ce qui est indispensable pour qu'un analyste
 * conformité puisse justifier (ou écarter) une déclaration de soupçon.
 */
@Getter
@RequiredArgsConstructor
public enum AmlRule {

    STRUCTURING(
            "Structuration (smurfing)",
            "Fractionnement de montants juste sous le seuil de déclaration.",
            30
    ),
    AMOUNT_ANOMALY(
            "Montant atypique",
            "Montant très supérieur à la moyenne historique du client.",
            20
    ),
    VELOCITY_SPIKE(
            "Pic de vélocité",
            "Nombre de transactions anormalement élevé sur une courte période.",
            20
    ),
    RAPID_PASSTHROUGH(
            "Flux traversant",
            "Fonds reçus puis ressortis presque immédiatement (compte tunnel).",
            25
    ),
    UNUSUAL_HOUR(
            "Horaire atypique",
            "Opération effectuée en pleine nuit, hors des habitudes du client.",
            10
    ),
    HIGH_RISK_COUNTRY(
            "Destination à risque",
            "Transfert vers une juridiction classée à risque élevé.",
            25
    ),
    NEW_BENEFICIARY_BURST(
            "Rafale de nouveaux bénéficiaires",
            "Plusieurs bénéficiaires inédits contactés en très peu de temps.",
            15
    ),
    DORMANT_REACTIVATION(
            "Réactivation de compte dormant",
            "Compte inactif de longue date subitement très actif.",
            20
    ),
    THRESHOLD_BREACH(
            "Franchissement de seuil",
            "Opération unitaire au-dessus du seuil déclaratif réglementaire.",
            15
    );

    private final String label;
    private final String description;
    private final int riskWeight;
}
