package com.dogaa.backend.modules.dispute.dto;

/**
 * The validation panel (BACKEND.md 9).
 *
 * @param validationsDone   how many distinct administrators have signed off
 * @param lastValidationNote names who validated and what remains, shown verbatim
 */
public record DisputeDetailResponse(String ref,
                                    String debitedAccount,
                                    String creditedAccount,
                                    String amount,
                                    int validationsRequired,
                                    int validationsDone,
                                    String lastValidationNote) {
}
