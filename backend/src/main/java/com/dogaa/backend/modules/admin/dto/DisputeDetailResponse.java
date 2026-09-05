package com.dogaa.backend.modules.admin.dto;

/**
 * The chargeback panel of one claim (BACKEND.md 9).
 *
 * @param validationsRequired how many distinct admins must sign off
 * @param validationsDone     how many already have
 * @param lastValidationNote  what happened last, named — "1re validation : Sena A. — en attente
 *                            d'un 2e admin conformité"
 */
public record DisputeDetailResponse(
        String ref,
        String debitedAccount,
        String creditedAccount,
        String amount,
        int validationsRequired,
        int validationsDone,
        String lastValidationNote) {
}
