package com.legalmetrology.analytics.model;

import java.math.BigDecimal;

public record KpiSnapshot(
        long totalInspections,
        long completedInspections,
        long totalViolations,
        long criticalViolations,
        BigDecimal averageComplianceScore,
        BigDecimal complianceRatePercent,
        long activeInspectors,
        long activeRules
) {
}
