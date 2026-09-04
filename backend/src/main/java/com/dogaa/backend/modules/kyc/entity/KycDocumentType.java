package com.dogaa.backend.modules.kyc.entity;

/** Pieces of evidence a user can submit. Which ones are required is decided by {@code KycTierRules}. */
public enum KycDocumentType {

    NATIONAL_ID,
    PASSPORT,
    DRIVING_LICENCE,
    /** Voter card: in the UEMOA zone this is often the only ID an informal trader holds. */
    VOTER_CARD,
    /** A photo of the holder, matched against the identity document. */
    SELFIE,
    PROOF_OF_ADDRESS;

    /** True for the documents that establish who someone is, as opposed to corroborating it. */
    public boolean isIdentityDocument() {
        return this == NATIONAL_ID || this == PASSPORT
                || this == DRIVING_LICENCE || this == VOTER_CARD;
    }
}
