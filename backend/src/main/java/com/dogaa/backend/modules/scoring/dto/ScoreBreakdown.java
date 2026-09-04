package com.dogaa.backend.modules.scoring.dto;

/**
 * The five axes and their total (DOGAA.md 3.2). Returned to the user so a refusal can be
 * explained: "your savings discipline is 8/30" is actionable, "score 47" is not.
 */
public record ScoreBreakdown(int savingsDiscipline,
                             int financialStability,
                             int inflowRegularity,
                             int usageIntensity,
                             int creditHistory,
                             int total) {
}
