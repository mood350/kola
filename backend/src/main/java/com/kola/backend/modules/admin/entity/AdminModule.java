package com.kola.backend.modules.admin.entity;

/** The back-office sections access is granted on (BACKEND.md 4.3). */
public enum AdminModule {
    DASHBOARD,
    USERS,
    CREDIT,
    FINANCE,
    DISPUTES,
    CONFIG,
    AUDIT,
    ROLES,
    SUPPORT,
    /** Own account and password. Open to every role, always. */
    PROFILE;

    /** Authority carried by an administrator's token, e.g. {@code MODULE_USERS}. */
    public String authority() {
        return "MODULE_" + name();
    }
}
