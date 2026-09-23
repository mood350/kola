package com.kola.backend.modules.dispute.dto;

import com.kola.backend.modules.dispute.entity.DisputeTag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** What a customer sends to contest one of their own transactions. */
public record OpenDisputeRequest(@NotBlank String transactionReference,
                                 @NotNull DisputeTag tag,
                                 @NotBlank @Size(max = 160) String title) {
}
