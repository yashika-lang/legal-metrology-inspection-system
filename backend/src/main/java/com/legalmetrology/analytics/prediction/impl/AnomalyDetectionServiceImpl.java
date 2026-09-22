package com.legalmetrology.analytics.prediction.impl;

import com.legalmetrology.analytics.explainability.DataPeriod;
import com.legalmetrology.analytics.explainability.Explainability;
import com.legalmetrology.analytics.model.DimensionMetric;
import com.legalmetrology.analytics.model.MonthlyMetric;
import com.legalmetrology.analytics.prediction.Anomaly;
import com.legalmetrology.analytics.prediction.AnomalyDetectionService;
import com.legalmetrology.analytics.service.AnalyticsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnomalyDetectionServiceImpl implements AnomalyDetectionService {

    private static final double Z_SCORE_THRESHOLD = 2.0;
    private static final int HISTORY_MONTHS = 6;
    private static final long MINIMUM_REGION_SAMPLE = 3;
    private static final String METHODOLOGY = "Z-Score Statistical Anomaly Detection";

    private final AnalyticsQueryService analyticsQueryService;

    @Override
    public List<Anomaly> detectViolationSpikes() {
        List<MonthlyMetric> history = analyticsQueryService.monthlyViolationCounts(HISTORY_MONTHS);
        if (history.size() < 3) {
            return List.of();
        }

        // Baseline excludes the most recent month — we're asking "is this month unusual vs. its predecessors".
        List<MonthlyMetric> baseline = history.subList(0, history.size() - 1);
        MonthlyMetric latest = history.get(history.size() - 1);

        double mean = baseline.stream().mapToLong(MonthlyMetric::count).average().orElse(0);
        double stdDev = stdDev(baseline.stream().mapToDouble(MonthlyMetric::count).toArray(), mean);
        if (stdDev == 0) {
            return List.of();
        }

        double zScore = (latest.count() - mean) / stdDev;
        if (Math.abs(zScore) < Z_SCORE_THRESHOLD) {
            return List.of();
        }

        String direction = zScore > 0 ? "spike" : "drop";
        String reason = "Sudden " + direction + " in violations: " + latest.month() + " recorded " + latest.count()
                + " violations vs. a " + baseline.size() + "-month baseline average of " + Math.round(mean) + ".";

        DataPeriod period = DataPeriod.ofMonths(baseline.get(0).month().atDay(1), latest.month().atEndOfMonth(), history.size(), HISTORY_MONTHS);

        Explainability explainability = Explainability.of(
                confidenceFromZScore(zScore),
                METHODOLOGY,
                period,
                List.of(reason, "Baseline mean = " + Math.round(mean) + ", standard deviation = " + String.format("%.2f", stdDev),
                        "z-score = " + String.format("%.2f", zScore) + " (threshold = " + Z_SCORE_THRESHOLD + ")"),
                Map.of("baselineMean", Math.round(mean * 100) / 100.0, "baselineStdDev", Math.round(stdDev * 100) / 100.0,
                        "baselineMonths", baseline.size(), "actual", latest.count()),
                List.of("Assumes monthly violation counts are approximately normally distributed",
                        "A short baseline (" + baseline.size() + " months) makes the mean/stddev estimate noisier"));

        return List.of(new Anomaly(
                "MONTHLY_VIOLATIONS", latest.month().toString(),
                BigDecimal.valueOf(mean).setScale(1, RoundingMode.HALF_UP), BigDecimal.valueOf(latest.count()),
                Math.round(zScore * 100) / 100.0, Z_SCORE_THRESHOLD, reason, explainability
        ));
    }

    @Override
    public List<Anomaly> detectRegionalAnomalies() {
        List<DimensionMetric> byRegion = analyticsQueryService.complianceScoreByRegion().stream()
                .filter(r -> r.count() >= MINIMUM_REGION_SAMPLE && r.averageScore() != null)
                .toList();

        if (byRegion.size() < 3) {
            return List.of();
        }

        DataPeriod period = DataPeriod.allTime(byRegion.stream().mapToLong(DimensionMetric::count).sum());

        List<Anomaly> anomalies = new ArrayList<>();
        for (DimensionMetric region : byRegion) {
            // Leave-one-out baseline: compare this region against the *other* regions' mean/stddev,
            // not a population that includes itself. Including the point being tested in its own
            // baseline creates a self-referential ceiling — for n=3 groups, a single outlier's
            // internally-computed z-score can never exceed ~1.41 no matter how extreme the true gap
            // is, because the outlier drags its own mean and standard deviation along with it. This
            // mirrors detectViolationSpikes()'s baseline-excludes-the-tested-month approach.
            double[] othersScores = byRegion.stream()
                    .filter(r -> !r.label().equals(region.label()))
                    .mapToDouble(r -> r.averageScore().doubleValue())
                    .toArray();
            if (othersScores.length < 2) {
                continue;
            }
            double baselineMean = java.util.Arrays.stream(othersScores).average().orElse(0);
            double baselineStdDev = stdDev(othersScores, baselineMean);
            if (baselineStdDev == 0) {
                continue;
            }

            double zScore = (region.averageScore().doubleValue() - baselineMean) / baselineStdDev;
            if (Math.abs(zScore) < Z_SCORE_THRESHOLD) {
                continue;
            }

            String direction = zScore > 0 ? "notably better" : "notably worse";
            String reason = region.label() + " is behaving differently: average compliance score " + region.averageScore()
                    + " is " + direction + " than the other regions' average of " + String.format("%.1f", baselineMean) + ".";

            Explainability explainability = Explainability.of(
                    confidenceFromZScore(zScore),
                    METHODOLOGY,
                    period,
                    List.of(reason, "Other regions' mean = " + String.format("%.1f", baselineMean) + ", standard deviation = " + String.format("%.2f", baselineStdDev),
                            "z-score = " + String.format("%.2f", zScore) + " (threshold = " + Z_SCORE_THRESHOLD + ")",
                            region.label() + " sample size: " + region.count() + " inspections"),
                    Map.of("baselineMean", Math.round(baselineMean * 100) / 100.0, "baselineStdDev", Math.round(baselineStdDev * 100) / 100.0,
                            "regionCount", byRegion.size(), "regionSampleSize", region.count()),
                    List.of("Assumes average compliance scores across regions are approximately normally distributed",
                            "Regions with fewer than " + MINIMUM_REGION_SAMPLE + " inspections are excluded as too noisy to compare",
                            "Baseline excludes the region being tested (leave-one-out), so its own extremity cannot dilute the baseline it's measured against"));

            anomalies.add(new Anomaly(
                    "REGION_COMPLIANCE", region.label(),
                    BigDecimal.valueOf(baselineMean).setScale(1, RoundingMode.HALF_UP), region.averageScore().setScale(1, RoundingMode.HALF_UP),
                    Math.round(zScore * 100) / 100.0, Z_SCORE_THRESHOLD, reason, explainability
            ));
        }
        return anomalies;
    }

    /** Maps |z| in [threshold, threshold+2] to a confidence in [0.7, 0.98] — further from the threshold, more confident this is a real anomaly. */
    private double confidenceFromZScore(double zScore) {
        double magnitude = Math.abs(zScore);
        double normalized = Math.min(1.0, (magnitude - Z_SCORE_THRESHOLD) / 2.0);
        return Math.round((0.7 + normalized * 0.28) * 100) / 100.0;
    }

    private double stdDev(double[] values, double mean) {
        if (values.length == 0) {
            return 0;
        }
        double variance = 0;
        for (double v : values) {
            variance += Math.pow(v - mean, 2);
        }
        return Math.sqrt(variance / values.length);
    }
}
