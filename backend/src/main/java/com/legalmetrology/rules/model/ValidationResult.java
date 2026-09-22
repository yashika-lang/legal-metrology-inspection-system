package com.legalmetrology.rules.model;

import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.common.enums.Severity;
import com.legalmetrology.inspection.entity.Rule;
import com.legalmetrology.vision.provider.VisionBoundingBox;

/**
 * Explainability contract: the output of every single rule evaluation,
 * pass or fail — which rule ran, what data it looked at, what it expected,
 * how confident the underlying data was, and a plain-language reason.
 * {@code rules.violation.ViolationGenerationService} turns the failing
 * ones into persisted {@code Violation} rows; the full list (including
 * passes) is what the explainability endpoint returns.
 */
public record ValidationResult(
        Rule rule,
        boolean passed,
        DeclarationType field,
        String actualValue,
        String expectedValue,
        double confidence,
        VisionBoundingBox boundingBox,
        String explanation
) {
    public static ValidationResult pass(Rule rule, DeclarationType field, String actualValue, double confidence,
                                         VisionBoundingBox boundingBox, String explanation) {
        return new ValidationResult(rule, true, field, actualValue, null, confidence, boundingBox, explanation);
    }

    public static ValidationResult fail(Rule rule, DeclarationType field, String actualValue, String expectedValue,
                                         double confidence, VisionBoundingBox boundingBox, String explanation) {
        return new ValidationResult(rule, false, field, actualValue, expectedValue, confidence, boundingBox, explanation);
    }

    public Severity severity() {
        return rule.getSeverity();
    }
}
