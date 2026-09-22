package com.legalmetrology.analytics.explainability;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The mandatory transparency contract every prediction, anomaly, trend
 * insight, and risk score in this system carries — "do not expose
 * black-box predictions; every prediction must be explainable."
 * <ul>
 *   <li>{@code confidence} — 0-1, how much this specific output should be trusted.</li>
 *   <li>{@code methodology} — the named technique, e.g. "Weighted Composite Risk Model",
 *       "Ordinary Least-Squares Linear Regression", "Z-Score Anomaly Detection".</li>
 *   <li>{@code inputDataPeriod} — exactly what historical window/sample size fed this output.</li>
 *   <li>{@code reasoning} — human-readable bullet points a report can print verbatim
 *       (the "Reason:" list in the spec's example).</li>
 *   <li>{@code supportingMetrics} — the same facts as {@code reasoning}, machine-readable.</li>
 *   <li>{@code assumptions} — what the method assumes that, if false, would undermine it
 *       (e.g. "assumes residuals are approximately normally distributed").</li>
 *   <li>{@code generatedAt} — when this specific computation ran.</li>
 * </ul>
 */
public record Explainability(
        double confidence,
        String methodology,
        DataPeriod inputDataPeriod,
        List<String> reasoning,
        Map<String, Object> supportingMetrics,
        List<String> assumptions,
        Instant generatedAt
) {
    public static Explainability of(double confidence, String methodology, DataPeriod inputDataPeriod,
                                     List<String> reasoning, Map<String, Object> supportingMetrics, List<String> assumptions) {
        return new Explainability(confidence, methodology, inputDataPeriod, reasoning, supportingMetrics, assumptions, Instant.now());
    }
}
