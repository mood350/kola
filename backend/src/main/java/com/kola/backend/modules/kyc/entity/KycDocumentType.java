package com.kola.backend.modules.kyc.entity;

/** Pieces of evidence a user can submit. Which ones are required is decided by {@code KycTierRules}. */
public enum KycDocumentType {

    NATIONAL_ID("Carte d'identité"),
    PASSPORT("Passeport"),
    DRIVING_LICENCE("Permis de conduire"),
    /** Voter card: in the UEMOA zone this is often the only ID an informal trader holds. */
    VOTER_CARD("Carte d'électeur"),
    /** A photo of the holder, matched against the identity document. */
    SELFIE("Selfie"),
    PROOF_OF_ADDRESS("Justificatif de domicile");

    private final String label;

    KycDocumentType(String label) {
        this.label = label;
    }

    /** What the reviewer reads in the queue, and what a rejection notice names. */
    public String label() {
        return label;
    }

    /** True for the documents that establish who someone is, as opposed to corroborating it. */
    public boolean isIdentityDocument() {
        return this == NATIONAL_ID || this == PASSPORT
                || this == DRIVING_LICENCE || this == VOTER_CARD;
    }
}
