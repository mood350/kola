package com.kola.backend.modules.dispute.dto;

import com.kola.backend.modules.dispute.entity.DisputeStatus;
import com.kola.backend.modules.dispute.entity.DisputeTag;

/**
 * A dispute as the list screen shows it (BACKEND.md 9).
 *
 * @param meta free text already assembled: "Ouvert il y a 2 h · TIER_2 · Lomé"
 */
public record DisputeResponse(String ref,
                              DisputeTag tag,
                              String tagLabel,
                              String amount,
                              String title,
                              String meta,
                              DisputeStatus status) {
}
