package com.legalmetrology.analytics.prediction;

import com.legalmetrology.analytics.explainability.Explainability;

import java.math.BigDecimal;

public record Anomaly(
        String dimension,
        String dimensionValue,
        BigDecimal expectedValue,
        BigDecimal actualValue,
        double zScore,
        double threshold,
        String reason,
        Explainability explainability
) {
}
