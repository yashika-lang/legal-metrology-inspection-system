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

/** {@code {"field":"MRP"}} — passes when the declaration was detected as present on the label, regardless of what its value looks like. */
@Component
@RequiredArgsConstructor
public class FieldExistsValidator implements FieldValidator {

    private final RuleExpressionParser expressionParser;

    @Override
    public ValidationType supportedType() {
        return ValidationType.FIELD_EXISTS;
    }

    @Override
    public ValidationResult validate(Rule rule, ValidationContext context) {
        DeclarationType field = expressionParser.getField(rule);
        DeclarationSnapshot snapshot = context.get(field);

        if (snapshot.present()) {
            return ValidationResult.pass(rule, field, snapshot.value(), snapshot.confidence(), snapshot.boundingBox(),
                    field + " was detected on the label" + (snapshot.value() != null ? " (\"" + snapshot.value() + "\")" : "") + ".");
        }
        return ValidationResult.fail(rule, field, null, "present", snapshot.confidence(), snapshot.boundingBox(),
                field + " was not detected on any of the inspection's images.");
    }
}
