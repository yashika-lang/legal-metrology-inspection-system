package com.legalmetrology.analytics.insight.generator;

import com.legalmetrology.analytics.explainability.DataPeriod;
import com.legalmetrology.analytics.explainability.Explainability;
import com.legalmetrology.analytics.insight.Insight;
import com.legalmetrology.analytics.insight.InsightGenerator;
import com.legalmetrology.analytics.insight.InsightType;
import com.legalmetrology.analytics.model.DimensionMetric;
import com.legalmetrology.analytics.service.AnalyticsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * "Manufacturer ABC has repeated violations" — flags manufacturers whose
 * violation count is a statistical outlier (more than one standard
 * deviation above the mean across all manufacturers with any violations),
 * so the bar scales with this system's actual data rather than a fixed
 * magic number.
 */
@Component
@RequiredArgsConstructor
public class RepeatOffenderInsightGenerator implements InsightGenerator {

    private static final long MINIMUM_SAMPLE_SIZE = 3;
    private static final String METHODOLOGY = "One-Standard-Deviation Outlier Detection";

    private final AnalyticsQueryService analyticsQueryService;

    @Override
    public List<Insight> generate() {
        List<DimensionMetric> byManufacturer = analyticsQueryService.violationCountsByManufacturer();
        if (byManufacturer.size() < 2) {
            return List.of();
        }

        double mean = byManufacturer.stream().mapToLong(DimensionMetric::count).average().orElse(0);
        double variance = byManufacturer.stream().mapToDouble(m -> Math.pow(m.count() - mean, 2)).average().orElse(0);
        double stdDev = Math.sqrt(variance);
        double outlierThreshold = mean + stdDev;

        long totalManufacturers = byManufacturer.size();
        long totalViolations = byManufacturer.stream().mapToLong(DimensionMetric::count).sum();

        return byManufacturer.stream()
                .filter(m -> m.count() >= MINIMUM_SAMPLE_SIZE && m.count() > outlierThreshold)
                .map(m -> buildInsight(m, mean, stdDev, totalManufacturers, totalViolations))
                .toList();
    }

    private Insight buildInsight(DimensionMetric manufacturer, double mean, double stdDev, long totalManufacturers, long totalViolations) {
        double deviationsAboveMean = stdDev > 0 ? (manufacturer.count() - mean) / stdDev : 0;
        double confidence = Math.min(0.95, 0.6 + deviationsAboveMean * 0.1);

        String message = "Manufacturer " + manufacturer.label() + " has repeated violations (" + manufacturer.count()
                + " total, vs an average of " + Math.round(mean) + " across all manufacturers).";

        DataPeriod period = DataPeriod.allTime(totalViolations);

        Explainability explainability = Explainability.of(confidence, METHODOLOGY, period,
                List.of(manufacturer.count() + " total violations for " + manufacturer.label(),
                        "Average across " + totalManufacturers + " manufacturers: " + Math.round(mean),
                        String.format("%.1f standard deviations above the mean", deviationsAboveMean)),
                Map.of("manufacturerId", manufacturer.entityId(), "violationCount", manufacturer.count(),
                        "averageAcrossManufacturers", Math.round(mean * 100) / 100.0, "standardDeviation", Math.round(stdDev * 100) / 100.0),
                List.of("Flagged only when the count exceeds mean + 1 standard deviation and the manufacturer has at least "
                        + MINIMUM_SAMPLE_SIZE + " violations",
                        "Does not adjust for how many inspections this manufacturer has had relative to others (see the Risk Index for a rate-based view)"));

        return Insight.of(InsightType.REPEAT_OFFENDER, manufacturer.label() + " — repeated violations", message, explainability);
    }
}
