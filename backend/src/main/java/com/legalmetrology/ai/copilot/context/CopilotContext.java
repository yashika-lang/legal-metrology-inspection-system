package com.legalmetrology.ai.copilot.context;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Everything the Copilot is allowed to know about one inspection — a
 * read-only, provider-agnostic snapshot assembled once per request by
 * {@link CopilotContextAssembler}. This is the entire surface area the LLM
 * ever sees: no raw OCR text, no Vision AI provider details, no rule
 * evaluation internals — only already-decided, persisted facts. The LLM
 * cannot "decide" compliance because it is never given anything to decide
 * with; every violation/score/declaration here was produced by the Rule
 * Engine and Declaration Fusion before the Copilot ever runs.
 */
public record CopilotContext(
        UUID inspectionId,
        String productName,
        String manufacturerName,
        String categoryName,
        String inspectionStatus,
        BigDecimal complianceScore,
        String fraudRisk,
        List<ViolationSummary> violations,
        List<DeclarationSummary> declarations,
        List<ImageQualitySummary> images
) {
    public record ViolationSummary(
            String ruleCode, String ruleTitle, String severity, String field, String description,
            String actualValue, String expectedValue, BigDecimal confidence, String suggestedFix, String legalReference
    ) {
    }

    public record DeclarationSummary(
            String type, boolean present, String value, BigDecimal confidence,
            BigDecimal readabilityScore, String labelSection
    ) {
    }

    public record ImageQualitySummary(
            String imageType, BigDecimal qualityScore, String qualityWarnings, String recommendedAction
    ) {
    }
}
