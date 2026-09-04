package com.dogaa.backend.common.enums;

public enum Role {
    USER,
    MERCHANT,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
