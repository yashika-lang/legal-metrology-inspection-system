package com.legalmetrology.analytics.risk;

import com.legalmetrology.analytics.explainability.Explainability;

import java.math.BigDecimal;
import java.util.UUID;

public record EntityRiskScore(
        String entityType,
        UUID entityId,
        String label,
        BigDecimal riskScore,
        RiskBand riskBand,
        Explainability explainability
) {
    public enum RiskBand { LOW, MEDIUM, HIGH }
}
