package com.legalmetrology.analytics.risk.impl;

import com.legalmetrology.analytics.explainability.DataPeriod;
import com.legalmetrology.analytics.explainability.Explainability;
import com.legalmetrology.analytics.model.RiskFactors;
import com.legalmetrology.analytics.risk.EntityRiskScore;
import com.legalmetrology.analytics.risk.RiskIndexService;
import com.legalmetrology.analytics.service.AnalyticsQueryService;
import com.legalmetrology.common.settings.repository.SettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * riskScore = 100 × (w1·violationRatê + w2·criticalRatio + w3·complianceGap) / (w1+w2+w3)
 * <ul>
 *   <li>violationRatê = min(1, violations/inspection ÷ referenceMaxRate) — saturating, so one extreme
 *       manufacturer can't blow the scale for everyone else.</li>
 *   <li>criticalRatio = critical violations ÷ total violations for that entity.</li>
 *   <li>complianceGap = (100 − average compliance score) ÷ 100.</li>
 * </ul>
 * All four numbers (three weights + the reference rate) are configurable
 * via {@code settings} (keys {@code risk.*}) — never hardcoded, same
 * pattern as {@code rules.scoring.ComplianceScoreService}. Every score
 * carries full {@link Explainability}: the reasoning bullets mirror the
 * spec's example ("5 critical violations in last 30 days", "Average
 * compliance score 41%", "Trend increasing by 12%").
 */
@Service
@RequiredArgsConstructor
public class RiskIndexServiceImpl implements RiskIndexService {

    private static final Map<String, BigDecimal> DEFAULTS = Map.of(
            "risk.weight.violationRate", BigDecimal.valueOf(0.4),
            "risk.weight.criticalRatio", BigDecimal.valueOf(0.3),
            "risk.weight.complianceGap", BigDecimal.valueOf(0.3),
            "risk.referenceMaxViolationRate", BigDecimal.valueOf(3.0)
    );
    private static final String METHODOLOGY = "Weighted Composite Risk Model";

    private final AnalyticsQueryService analyticsQueryService;
    private final SettingsRepository settingsRepository;

    @Override
    public List<EntityRiskScore> manufacturerRiskIndex() {
        return score("MANUFACTURER", analyticsQueryService.riskFactorsByManufacturer());
    }

    @Override
    public List<EntityRiskScore> categoryRiskIndex() {
        return score("CATEGORY", analyticsQueryService.riskFactorsByCategory());
    }

    @Override
    public List<EntityRiskScore> regionRiskIndex() {
        return score("REGION", analyticsQueryService.riskFactorsByRegion());
    }

    @Override
    public List<EntityRiskScore> inspectorRiskIndex() {
        return score("INSPECTOR", analyticsQueryService.riskFactorsByInspector());
    }

    private List<EntityRiskScore> score(String entityType, List<RiskFactors> factorsList) {
        BigDecimal w1 = weight("risk.weight.violationRate");
        BigDecimal w2 = weight("risk.weight.criticalRatio");
        BigDecimal w3 = weight("risk.weight.complianceGap");
        BigDecimal referenceMaxRate = weight("risk.referenceMaxViolationRate");
        BigDecimal weightSum = w1.add(w2).add(w3);

        return factorsList.stream()
                .filter(f -> f.inspectionCount() > 0)
                .map(f -> toRiskScore(entityType, f, w1, w2, w3, weightSum, referenceMaxRate))
                .sorted(Comparator.comparing(EntityRiskScore::riskScore).reversed())
                .toList();
    }

    private EntityRiskScore toRiskScore(String entityType, RiskFactors f, BigDecimal w1, BigDecimal w2, BigDecimal w3,
                                         BigDecimal weightSum, BigDecimal referenceMaxRate) {
        BigDecimal violationRate = BigDecimal.valueOf(f.violationCount())
                .divide(BigDecimal.valueOf(f.inspectionCount()), 6, RoundingMode.HALF_UP);
        BigDecimal violationRateNormalized = violationRate.divide(referenceMaxRate, 6, RoundingMode.HALF_UP).min(BigDecimal.ONE);

        BigDecimal criticalRatio = f.violationCount() == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(f.criticalViolationCount()).divide(BigDecimal.valueOf(f.violationCount()), 6, RoundingMode.HALF_UP);

        BigDecimal avgScore = f.averageComplianceScore() != null ? f.averageComplianceScore() : BigDecimal.valueOf(100);
        BigDecimal complianceGap = BigDecimal.valueOf(100).subtract(avgScore).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);

        BigDecimal weightedSum = w1.multiply(violationRateNormalized)
                .add(w2.multiply(criticalRatio))
                .add(w3.multiply(complianceGap));

        BigDecimal riskScore = weightSum.signum() == 0 ? BigDecimal.ZERO
                : weightedSum.divide(weightSum, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP).max(BigDecimal.ZERO).min(BigDecimal.valueOf(100));

        Explainability explainability = buildExplainability(f, violationRate, criticalRatio, avgScore, w1, w2, w3, referenceMaxRate, riskScore);

        return new EntityRiskScore(entityType, f.entityId(), f.label(), riskScore, band(riskScore), explainability);
    }

    private Explainability buildExplainability(RiskFactors f, BigDecimal violationRate, BigDecimal criticalRatio, BigDecimal avgScore,
                                                BigDecimal w1, BigDecimal w2, BigDecimal w3, BigDecimal referenceMaxRate, BigDecimal riskScore) {
        List<String> reasoning = new ArrayList<>();
        reasoning.add(f.criticalViolationCount() + " critical violations overall, " + f.recentCriticalViolationCount() + " in the last 30 days");
        reasoning.add("Average compliance score " + avgScore.setScale(0, RoundingMode.HALF_UP) + "%");
        reasoning.add("Violation rate " + violationRate.setScale(2, RoundingMode.HALF_UP) + " per inspection (reference max "
                + referenceMaxRate.stripTrailingZeros().toPlainString() + ")");

        Double trendPercent = null;
        if (f.priorViolationCount() > 0) {
            trendPercent = (f.recentViolationCount() - f.priorViolationCount()) * 100.0 / f.priorViolationCount();
            String direction = trendPercent >= 0 ? "increasing" : "decreasing";
            reasoning.add("Violations " + direction + " by " + Math.round(Math.abs(trendPercent)) + "% vs. the prior 30 days ("
                    + f.recentViolationCount() + " vs " + f.priorViolationCount() + ")");
        } else if (f.recentViolationCount() > 0) {
            reasoning.add(f.recentViolationCount() + " violations in the last 30 days (no prior 30-day period to compare against)");
        }

        DataPeriod period = DataPeriod.allTime(f.inspectionCount());

        // Confidence in a risk score itself scales with how much evidence (inspections) it's built on —
        // a manufacturer with 2 inspections gets a less trustworthy score than one with 50.
        double sampleConfidence = Math.min(0.95, 0.4 + Math.min(f.inspectionCount(), 20) / 25.0);

        Map<String, Object> metrics = new java.util.HashMap<>(Map.of(
                "inspectionCount", f.inspectionCount(),
                "violationCount", f.violationCount(),
                "criticalViolationCount", f.criticalViolationCount(),
                "recentCriticalViolationCount", f.recentCriticalViolationCount(),
                "averageComplianceScore", avgScore,
                "violationRate", violationRate.setScale(2, RoundingMode.HALF_UP),
                "criticalRatio", criticalRatio.setScale(2, RoundingMode.HALF_UP)
        ));
        if (trendPercent != null) {
            metrics.put("trendPercent", Math.round(trendPercent * 100) / 100.0);
        }

        List<String> assumptions = List.of(
                "Weights: violationRate=" + w1 + ", criticalRatio=" + w2 + ", complianceGap=" + w3 + " (configurable via settings)",
                "Violation rate is capped at the reference max (" + referenceMaxRate.stripTrailingZeros().toPlainString()
                        + " per inspection) so one extreme entity cannot dominate the scale",
                "Based on all-time aggregate data except the explicit 30-day recent/prior comparison noted above",
                "Confidence reflects sample size (" + f.inspectionCount() + " inspections), not the risk score's magnitude"
        );

        return Explainability.of(sampleConfidence, METHODOLOGY, period, reasoning, metrics, assumptions);
    }

    private EntityRiskScore.RiskBand band(BigDecimal riskScore) {
        double value = riskScore.doubleValue();
        if (value >= 70) return EntityRiskScore.RiskBand.HIGH;
        if (value >= 40) return EntityRiskScore.RiskBand.MEDIUM;
        return EntityRiskScore.RiskBand.LOW;
    }

    private BigDecimal weight(String key) {
        return settingsRepository.findByKey(key)
                .map(setting -> new BigDecimal(setting.getValue().trim()))
                .orElseGet(() -> DEFAULTS.get(key));
    }
}
