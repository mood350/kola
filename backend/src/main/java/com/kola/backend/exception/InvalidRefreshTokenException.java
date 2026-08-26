package com.kola.backend.exception;

/**
 * Lancée quand un refresh token présenté à {@code POST /api/auth/refresh-token}
 * n'authentifie plus personne : jeton illisible, expiré, de mauvais type, ou
 * rattaché à un compte qui n'existe plus.
 *
 * HTTP 401 UNAUTHORIZED — et non 400. La distinction n'est pas cosmétique :
 * 401 est le signal qu'un client attend pour conclure « cette session est
 * finie, il faut se reconnecter ». Ces cas passaient auparavant par une
 * {@code RuntimeException} nue, donc par le gestionnaire de repli, et
 * répondaient 500 : les clients traitaient une session normalement expirée
 * comme une panne du serveur.
 *
 * Un seul message pour tous les cas, comme pour {@link InvalidTokenException} :
 * préciser lequel des quatre s'applique renseignerait sur la validité des
 * jetons essayés, sans rien apporter à l'utilisateur légitime — qui n'a de
 * toute façon qu'une seule chose à faire, se reconnecter.
 */
public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException(String message) {
        super(message);
    }

    public InvalidRefreshTokenException() {
        super("Session expirée. Veuillez vous reconnecter.");
    }
}
