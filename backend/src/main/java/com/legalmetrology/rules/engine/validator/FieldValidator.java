package com.legalmetrology.rules.engine.validator;

import com.legalmetrology.common.enums.ValidationType;
import com.legalmetrology.inspection.entity.Rule;
import com.legalmetrology.rules.engine.ValidationContext;
import com.legalmetrology.rules.model.ValidationResult;

/**
 * Strategy interface for one generic validation type (Step 5). Every
 * implementation is reusable across any number of {@code Rule} rows of its
 * {@link #supportedType()} — adding a new rule that fits an existing type
 * is a pure data change (Open/Closed Principle); only a genuinely new kind
 * of check needs a new implementation of this interface.
 */
public interface FieldValidator {

    ValidationType supportedType();

    ValidationResult validate(Rule rule, ValidationContext context);
}
