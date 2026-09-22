package com.legalmetrology.analytics.dto;

import com.legalmetrology.analytics.model.MonthlyMetric;
import com.legalmetrology.analytics.risk.EntityRiskScore;

import java.util.List;
import java.util.UUID;

public record ManufacturerDrillDownResponse(
        UUID manufacturerId,
        EntityRiskScore riskScore,
        List<MonthlyMetric> monthlyViolationTrend
) {
}
