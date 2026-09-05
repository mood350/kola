package com.dogaa.backend.modules.admin.dto;

/**
 * A customer as the back-office table shows them (BACKEND.md 6).
 *
 * <p>Everything is pre-formatted server-side — {@code age} as "14 mois", {@code loan} as
 * "100 000 XOF" — because the front-end renders these strings verbatim and does no transformation.
 *
 * <p>⚠️ {@code id} is a UUID string, while the contract types it {@code number}. Our users have
 * never had numeric identifiers and inventing a second one purely for display would be worse. It
 * works unchanged for React keys and URL building; only arithmetic on the id would break.
 */
public record ClientUserResponse(String id,
                                 String initials,
                                 String name,
                                 String phone,
                                 String tier,
                                 int score,
                                 String age,
                                 String state,
                                 long vaults,
                                 String loan) {
}
