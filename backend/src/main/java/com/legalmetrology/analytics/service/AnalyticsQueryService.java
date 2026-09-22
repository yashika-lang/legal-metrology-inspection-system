package com.legalmetrology.analytics.service;

import com.legalmetrology.analytics.model.DimensionMetric;
import com.legalmetrology.analytics.model.FieldCategoryCount;
import com.legalmetrology.analytics.model.KpiSnapshot;
import com.legalmetrology.analytics.model.MonthlyMetric;
import com.legalmetrology.analytics.model.RegionSeverityCount;
import com.legalmetrology.analytics.model.RiskFactors;
import com.legalmetrology.analytics.model.RuleSummary;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The "Analytics" layer of Analytics → Insight Engine → Prediction Engine →
 * Dashboard: pure historical aggregation, no interpretation. Every class in
 * {@code insight}/{@code prediction}/{@code risk} depends on this interface
 * (dependency inversion), not on {@code AnalyticsRepository} directly, and
 * consumes only structured, persisted inspection/violation/rule data — never
 * OCR text, Vision AI output, or the rule engine's live evaluation logic.
 */
public interface AnalyticsQueryService {

    KpiSnapshot getKpiSnapshot();

    List<MonthlyMetric> monthlyInspectionCounts(int monthsBack);

    List<MonthlyMetric> monthlyViolationCounts(int monthsBack);

    List<MonthlyMetric> monthlyViolationCountsForRule(String ruleCode, int monthsBack);

    List<MonthlyMetric> monthlyViolationCountsForManufacturer(UUID manufacturerId, int monthsBack);

    List<MonthlyMetric> monthlyAverageComplianceScore(int monthsBack);

    List<DimensionMetric> mostViolatedRules(int limit);

    List<DimensionMetric> severityDistribution();

    List<DimensionMetric> violationCountsByManufacturer();

    List<DimensionMetric> violationCountsByRegion();

    List<DimensionMetric> violationCountsByCategory();

    List<FieldCategoryCount> violationFieldCountsByCategory();

    List<DimensionMetric> complianceScoreByManufacturer();

    List<DimensionMetric> complianceScoreByRegion();

    List<DimensionMetric> complianceScoreByCategory();

    List<DimensionMetric> inspectorActivity();

    BigDecimal averageComplianceScoreInWindow(Instant start, Instant end);

    List<LocalDate> ruleVersionEffectiveDates(String ruleCode);

    List<RuleSummary> activeRuleSummaries();

    List<RuleSummary> activeRuleSummariesWithMultipleVersions();

    List<RegionSeverityCount> severityCountsByRegion();

    List<RiskFactors> riskFactorsByManufacturer();

    List<RiskFactors> riskFactorsByCategory();

    List<RiskFactors> riskFactorsByRegion();

    List<RiskFactors> riskFactorsByInspector();
}
