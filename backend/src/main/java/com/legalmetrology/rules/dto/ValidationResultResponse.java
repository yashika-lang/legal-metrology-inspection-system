package com.legalmetrology.rules.dto;

import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.common.enums.Severity;
import com.legalmetrology.vision.provider.VisionBoundingBox;

import java.util.UUID;

/**
 * Explainability: one rule's full evaluation outcome, pass or fail — every
 * rule that ran, whether or not it failed. Shares most fields with {@link
 * ViolationResponse} by necessity (both describe a rule outcome against a
 * declaration), but is a genuinely distinct concept, not a near-duplicate
 * to collapse: this is a transient, non-persisted explain-only view (see
 * {@code InspectionEvaluationService#explain}), where {@code
 * ViolationResponse} is the view of a row actually persisted to the
 * {@code violations} table for a rule that failed.
 */
public record ValidationResultResponse(
        UUID ruleId,
        String ruleCode,
        String ruleTitle,
        Severity severity,
        boolean passed,
        DeclarationType field,
        String actualValue,
        String expectedValue,
        double confidence,
        VisionBoundingBox boundingBox,
        String explanation
) {
}
