package com.dogaa.backend.modules.admin.dto;

/**
 * A claim as the queue shows it (BACKEND.md 9).
 *
 * @param ref      the disputed transaction's public reference
 * @param tag      wire value the console styles on: {@code fraud}, {@code double_debit}, {@code p2p}
 * @param tagLabel French label printed next to it
 * @param amount   pre-formatted, e.g. "450 000 XOF"
 * @param title    one line describing the claim
 * @param meta     pre-formatted context, e.g. "Ouvert il y a 2 h · TIER_2 · Lomé"
 * @param status   {@code open}, {@code chargeback_pending}, {@code resolved} or {@code rejected}
 */
public record DisputeResponse(
        String ref,
        String tag,
        String tagLabel,
        String amount,
        String title,
        String meta,
        String status) {
}
