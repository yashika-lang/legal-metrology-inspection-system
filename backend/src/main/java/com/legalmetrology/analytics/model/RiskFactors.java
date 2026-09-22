package com.legalmetrology.analytics.model;

import java.math.BigDecimal;
import java.util.UUID;

/** The raw ingredients {@code risk.RiskIndexService} combines into one composite score for a manufacturer/category/region/inspector. */
public record RiskFactors(
        UUID entityId,
        String label,
        long inspectionCount,
        long violationCount,
        long criticalViolationCount,
        BigDecimal averageComplianceScore,
        long recentCriticalViolationCount,
        long recentViolationCount,
        long priorViolationCount
) {
}
