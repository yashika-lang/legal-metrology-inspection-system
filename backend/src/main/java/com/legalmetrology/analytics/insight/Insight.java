package com.legalmetrology.analytics.insight;

import com.legalmetrology.analytics.explainability.Explainability;

import java.util.UUID;

/**
 * One generated, actionable observation — deliberately template-based and
 * deterministic rather than free-text LLM output, so every number in
 * {@code message} is traceable to {@code explainability.supportingMetrics()}
 * and every insight in this compliance system is auditable, not a black box.
 */
public record Insight(UUID id, InsightType type, String title, String message, Explainability explainability) {

    public static Insight of(InsightType type, String title, String message, Explainability explainability) {
        return new Insight(UUID.randomUUID(), type, title, message, explainability);
    }
}
