package com.kola.backend.email;

import lombok.Getter;

/**
 * Enum des templates d'emails.
 * Chaque valeur correspond à un fichier HTML
 * dans src/main/resources/templates/
 */
@Getter
public enum EmailTemplateName {

    ACTIVATE_ACCOUNT("activate_account"),
    WELCOME("welcome"),
    RESET_PASSWORD("reset_password"),
    NEW_DEVICE_LOGIN("new_device_login");

    private final String name;

    EmailTemplateName(String name) {
        this.name = name;
    }
}