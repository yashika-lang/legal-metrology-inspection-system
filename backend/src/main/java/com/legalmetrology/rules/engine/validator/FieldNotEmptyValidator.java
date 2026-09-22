package com.legalmetrology.rules.engine.validator;

import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.common.enums.ValidationType;
import com.legalmetrology.inspection.entity.Rule;
import com.legalmetrology.rules.engine.RuleExpressionParser;
import com.legalmetrology.rules.engine.ValidationContext;
import com.legalmetrology.rules.model.DeclarationSnapshot;
import com.legalmetrology.rules.model.ValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@code {"field":"LICENSE_NUMBER"}} — stricter than {@link FieldExistsValidator}:
 * a detection can be marked present by a provider that spotted the label
 * region but couldn't read legible text out of it, leaving an empty value.
 * This type catches that case explicitly.
 */
@Component
@RequiredArgsConstructor
public class FieldNotEmptyValidator implements FieldValidator {

    private final RuleExpressionParser expressionParser;

    @Override
    public ValidationType supportedType() {
        return ValidationType.FIELD_NOT_EMPTY;
    }

    @Override
    public ValidationResult validate(Rule rule, ValidationContext context) {
        DeclarationType field = expressionParser.getField(rule);
        DeclarationSnapshot snapshot = context.get(field);
        boolean hasContent = snapshot.present() && snapshot.value() != null && !snapshot.value().isBlank();

        if (hasContent) {
            return ValidationResult.pass(rule, field, snapshot.value(), snapshot.confidence(), snapshot.boundingBox(),
                    field + " was detected with a legible value (\"" + snapshot.value() + "\").");
        }
        return ValidationResult.fail(rule, field, snapshot.value(), "non-empty value", snapshot.confidence(), snapshot.boundingBox(),
                snapshot.present()
                        ? field + " was detected but no legible value could be read."
                        : field + " was not detected on any of the inspection's images.");
    }
}
