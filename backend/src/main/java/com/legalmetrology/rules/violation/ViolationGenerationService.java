package com.legalmetrology.rules.violation;

import com.legalmetrology.inspection.entity.Violation;
import com.legalmetrology.rules.model.ValidationResult;

import java.util.List;
import java.util.UUID;

/** Step 8: turns the rule engine's failing {@link ValidationResult}s into persisted {@code Violation} rows for an inspection. */
public interface ViolationGenerationService {

    List<Violation> generate(UUID inspectionId, List<ValidationResult> evaluationResults);

    List<Violation> getViolations(UUID inspectionId);
}
