package com.legalmetrology.rules.engine.custom;

import com.legalmetrology.inspection.entity.Rule;
import com.legalmetrology.rules.engine.ValidationContext;
import com.legalmetrology.rules.model.ValidationResult;

/**
 * The escape hatch for validation logic that genuinely can't be expressed
 * by the 11 generic types (cross-field conditionals, multi-field
 * consistency checks, etc.) — used only when a rule's
 * {@code validation_type} is {@code CUSTOM}. Every other implementation
 * detail of the rule (metadata, severity, active/version state) stays
 * database-driven exactly like every other rule; only the boolean-pass/fail
 * logic itself needs code, registered here by a stable {@link #key()} that
 * the rule's {@code validation_expression} (e.g. {@code {"strategy":"..."}})
 * references.
 */
public interface CustomRuleStrategy {

    String key();

    ValidationResult evaluate(Rule rule, ValidationContext context);
}
