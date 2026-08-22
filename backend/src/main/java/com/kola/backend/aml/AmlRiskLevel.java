package com.kola.backend.aml;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AmlRiskLevel {

    LOW(0, 24),
    MEDIUM(25, 49),
    HIGH(50, 74),
    CRITICAL(75, 100);

    private final int minScore;
    private final int maxScore;

    public static AmlRiskLevel fromScore(int score) {
        for (AmlRiskLevel level : values()) {
            if (score >= level.minScore && score <= level.maxScore) {
                return level;
            }
        }
        return CRITICAL;
    }
}
