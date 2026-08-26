package com.kola.backend.exception;

/**
 * Lancée quand un code à usage unique soumis par l'utilisateur ne correspond à
 * aucun token valide : code inexistant, déjà consommé, ou expiré.
 *
 * POURQUOI CETTE CLASSE EXISTE : ces trois cas étaient signalés par une
 * {@code RuntimeException} nue, que seul le gestionnaire de repli attrapait —
 * l'API répondait donc 500 « Une erreur interne est survenue » à quelqu'un qui
 * avait simplement recopié un chiffre de travers. Le client ne pouvait rien en
 * faire : un 500 se traite comme une panne, pas comme une saisie à corriger.
 * HTTP 400 BAD REQUEST.
 *
 * Les trois cas partagent délibérément UN SEUL message. Distinguer « code
 * inconnu » de « code expiré » renseignerait un attaquant sur la validité des
 * codes qu'il essaie ; l'utilisateur légitime, lui, a la même action à faire
 * dans les deux cas — en redemander un.
 */
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException() {
        super("Code invalide ou expiré. Demandez-en un nouveau.");
    }
}
