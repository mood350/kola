package com.dogaa.backend.modules.admin.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.EnumSet;
import java.util.Set;

/**
 * Back-office roles and what each may reach (BACKEND.md 4.3).
 *
 * <p>The front-end hides navigation links from roles that lack access, which is a convenience and
 * nothing more: hiding a link does not stop anyone from calling the endpoint. The same matrix is
 * therefore enforced here, server-side, and it is this copy that actually protects anything.
 *
 * <p>The labels are the exact strings the admin UI expects on the wire — they are part of the
 * contract, not decoration, so they are serialised rather than the enum names.
 */
public enum AdminRole {

    SUPER_ADMIN("Super-admin", EnumSet.allOf(AdminModule.class)),

    COMPLIANCE_OFFICER("Agent conformité", EnumSet.of(
            AdminModule.DASHBOARD, AdminModule.USERS, AdminModule.DISPUTES,
            AdminModule.AUDIT, AdminModule.SUPPORT, AdminModule.PROFILE)),

    CREDIT_ANALYST("Analyste crédit", EnumSet.of(
            AdminModule.DASHBOARD, AdminModule.CREDIT, AdminModule.FINANCE, AdminModule.PROFILE)),

    SUPPORT("Support", EnumSet.of(
            AdminModule.DASHBOARD, AdminModule.USERS, AdminModule.SUPPORT, AdminModule.PROFILE));

    private final String label;
    private final Set<AdminModule> modules;

    AdminRole(String label, Set<AdminModule> modules) {
        this.label = label;
        this.modules = modules;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    public Set<AdminModule> modules() {
        return modules;
    }

    public boolean canAccess(AdminModule module) {
        return modules.contains(module);
    }

    /** Accepts both the wire label ("Agent conformité") and the enum name, so either form works. */
    @JsonCreator
    public static AdminRole fromLabel(String value) {
        for (AdminRole role : values()) {
            if (role.label.equalsIgnoreCase(value) || role.name().equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown admin role: " + value);
    }
}
