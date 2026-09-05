package com.dogaa.backend.modules.qr.entity;

public enum QrCodeStatus {

    ACTIVE,

    /** A payment request that has been paid. Terminal. */
    USED,

    /** Cancelled by its owner — a code on a printed receipt they no longer stand behind. */
    REVOKED
}
