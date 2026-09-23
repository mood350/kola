package com.kola.backend.modules.kyc.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Back-office decision. A rejection must say why, so the user can fix it. */
public record ReviewDocumentRequest(@NotNull Boolean approved,
                                    @Size(max = 500) String rejectionReason) {
}
