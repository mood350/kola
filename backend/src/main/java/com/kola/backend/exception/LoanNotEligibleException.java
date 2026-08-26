package com.kola.backend.exception;

/**
 * Une condition d'accès au crédit n'est pas remplie.
 *
 * ═══ POURQUOI DES CONDITIONS SÉPARÉES DU SCORE ═══
 *
 * Parce qu'un score composite se compense toujours. Avec un seuil unique
 * (« score ≥ 40 »), d'excellents résultats sur six règles rachètent l'échec
 * complet des deux autres — et l'on prête à un compte ouvert la semaine
 * dernière, non vérifié, parce qu'il a beaucoup transféré d'argent.
 *
 * Ces conditions-ci ne se compensent pas : elles bloquent, quel que soit le
 * score. C'est ce qui rend le système sélectif là où un barème pondéré ne peut
 * pas l'être.
 *
 * HTTP 422 UNPROCESSABLE ENTITY : la demande est bien formée et l'utilisateur
 * authentifié — c'est sa situation qui ne permet pas encore l'opération. Le
 * message dit toujours QUELLE condition manque, sans quoi l'emprunteur n'a
 * aucun moyen de savoir quoi corriger.
 */
public class LoanNotEligibleException extends RuntimeException {

    public LoanNotEligibleException(String message) {
        super(message);
    }
}
