package com.legalmetrology.rules.engine;

import com.legalmetrology.rules.model.ValidationResult;

import java.util.List;
import java.util.UUID;

/**
 * Step 5 facade: evaluates every active cached rule against an inspection's
 * currently-fused declarations. Returns every result, pass and fail alike —
 * explainability (Step: "why it passed / why it failed") for the whole
 * rule set, not just the violations. Callers that only want the failures
 * filter this list themselves ({@code rules.violation.ViolationGenerationService} does).
 */
public interface RuleEngineService {

    List<ValidationResult> evaluate(UUID inspectionId);
}
