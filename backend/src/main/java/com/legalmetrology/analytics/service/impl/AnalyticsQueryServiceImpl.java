package com.legalmetrology.analytics.service.impl;

import com.legalmetrology.analytics.model.DimensionMetric;
import com.legalmetrology.analytics.model.FieldCategoryCount;
import com.legalmetrology.analytics.model.KpiSnapshot;
import com.legalmetrology.analytics.model.MonthlyMetric;
import com.legalmetrology.analytics.model.RegionSeverityCount;
import com.legalmetrology.analytics.model.RiskFactors;
import com.legalmetrology.analytics.model.RuleSummary;
import com.legalmetrology.analytics.repository.AnalyticsRepository;
import com.legalmetrology.analytics.service.AnalyticsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsQueryServiceImpl implements AnalyticsQueryService {

    private final AnalyticsRepository analyticsRepository;

    @Override
    public KpiSnapshot getKpiSnapshot() {
        return analyticsRepository.getKpiSnapshot();
    }

    @Override
    public List<MonthlyMetric> monthlyInspectionCounts(int monthsBack) {
        return analyticsRepository.monthlyInspectionCounts(monthsBack);
    }

    @Override
    public List<MonthlyMetric> monthlyViolationCounts(int monthsBack) {
        return analyticsRepository.monthlyViolationCounts(monthsBack);
    }

    @Override
    public List<MonthlyMetric> monthlyViolationCountsForRule(String ruleCode, int monthsBack) {
        return analyticsRepository.monthlyViolationCountsForRule(ruleCode, monthsBack);
    }

    @Override
    public List<MonthlyMetric> monthlyViolationCountsForManufacturer(java.util.UUID manufacturerId, int monthsBack) {
        return analyticsRepository.monthlyViolationCountsForManufacturer(manufacturerId, monthsBack);
    }

    @Override
    public List<MonthlyMetric> monthlyAverageComplianceScore(int monthsBack) {
        return analyticsRepository.monthlyAverageComplianceScore(monthsBack);
    }

    @Override
    public List<DimensionMetric> mostViolatedRules(int limit) {
        return analyticsRepository.mostViolatedRules(limit);
    }

    @Override
    public List<DimensionMetric> severityDistribution() {
        return analyticsRepository.severityDistribution();
    }

    @Override
    public List<DimensionMetric> violationCountsByManufacturer() {
        return analyticsRepository.violationCountsByManufacturer();
    }

    @Override
    public List<DimensionMetric> violationCountsByRegion() {
        return analyticsRepository.violationCountsByRegion();
    }

    @Override
    public List<DimensionMetric> violationCountsByCategory() {
        return analyticsRepository.violationCountsByCategory();
    }

    @Override
    public List<FieldCategoryCount> violationFieldCountsByCategory() {
        return analyticsRepository.violationFieldCountsByCategory();
    }

    @Override
    public List<DimensionMetric> complianceScoreByManufacturer() {
        return analyticsRepository.complianceScoreByManufacturer();
    }

    @Override
    public List<DimensionMetric> complianceScoreByRegion() {
        return analyticsRepository.complianceScoreByRegion();
    }

    @Override
    public List<DimensionMetric> complianceScoreByCategory() {
        return analyticsRepository.complianceScoreByCategory();
    }

    @Override
    public List<DimensionMetric> inspectorActivity() {
        return analyticsRepository.inspectorActivity();
    }

    @Override
    public BigDecimal averageComplianceScoreInWindow(Instant start, Instant end) {
        return analyticsRepository.averageComplianceScoreInWindow(start, end);
    }

    @Override
    public List<LocalDate> ruleVersionEffectiveDates(String ruleCode) {
        return analyticsRepository.ruleVersionEffectiveDates(ruleCode);
    }

    @Override
    public List<RuleSummary> activeRuleSummaries() {
        return analyticsRepository.activeRuleSummaries();
    }

    @Override
    public List<RuleSummary> activeRuleSummariesWithMultipleVersions() {
        return analyticsRepository.activeRuleSummariesWithMultipleVersions();
    }

    @Override
    public List<RegionSeverityCount> severityCountsByRegion() {
        return analyticsRepository.severityCountsByRegion();
    }

    @Override
    public List<RiskFactors> riskFactorsByManufacturer() {
        return analyticsRepository.riskFactorsByManufacturer();
    }

    @Override
    public List<RiskFactors> riskFactorsByCategory() {
        return analyticsRepository.riskFactorsByCategory();
    }

    @Override
    public List<RiskFactors> riskFactorsByRegion() {
        return analyticsRepository.riskFactorsByRegion();
    }

    @Override
    public List<RiskFactors> riskFactorsByInspector() {
        return analyticsRepository.riskFactorsByInspector();
    }
}
