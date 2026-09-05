package com.dogaa.backend.modules.admin.dto;

import java.util.List;

/** Revenue breakdown and its total (BACKEND.md 8). */
public record RevenueResponse(List<RevenueLineResponse> lines, String total) {

    /**
     * @param pct share of the total, 0-100
     */
    public record RevenueLineResponse(String label, String value, int pct) {
    }
}
