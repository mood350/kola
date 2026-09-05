package com.dogaa.backend.modules.admin.dto;

/**
 * One slice of where the platform's money sits (BACKEND.md 8).
 *
 * @param pct share of the total, 0-100; the buckets add up to 100
 */
public record LiquidityBucketResponse(String label, String value, String note, int pct) {
}
