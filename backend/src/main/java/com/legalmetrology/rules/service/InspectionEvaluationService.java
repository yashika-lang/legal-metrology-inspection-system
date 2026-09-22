package com.legalmetrology.rules.service;

import com.legalmetrology.rules.dto.EvaluationResponse;
import com.legalmetrology.rules.model.ValidationResult;

import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the rule-engine pipeline stage for one inspection: evaluate
 * → generate violations → compute compliance score. Kept thin and separate
 * from {@code RuleEngineService} itself so each stage stays independently
 * testable/callable (per the "every step should be independently
 * testable" requirement) while still offering one convenient entry point
 * for the common "run everything" case.
 */
public interface InspectionEvaluationService {

    EvaluationResponse evaluateAndScore(UUID inspectionId);

    /** Explainability preview — evaluates every rule without persisting violations or the score. */
    List<ValidationResult> explain(UUID inspectionId);
}
