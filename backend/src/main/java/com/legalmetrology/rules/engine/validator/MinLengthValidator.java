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

/** {@code {"field":"ADDRESS","minLength":10}} */
@Component
@RequiredArgsConstructor
public class MinLengthValidator implements FieldValidator {

    private final RuleExpressionParser expressionParser;

    @Override
    public ValidationType supportedType() {
        return ValidationType.MIN_LENGTH;
    }

    @Override
    public ValidationResult validate(Rule rule, ValidationContext context) {
        var params = expressionParser.parse(rule);
        DeclarationType field = expressionParser.getField(rule);
        int minLength = params.path("minLength").asInt(1);

        DeclarationSnapshot snapshot = context.get(field);
        if (!snapshot.present() || snapshot.value() == null) {
            return ValidationResult.pass(rule, field, null, snapshot.confidence(), snapshot.boundingBox(),
                    field + " was not detected, so its length could not be checked (see the presence rule for this field).");
        }

        int actualLength = snapshot.value().trim().length();
        if (actualLength >= minLength) {
            return ValidationResult.pass(rule, field, snapshot.value(), snapshot.confidence(), snapshot.boundingBox(),
                    field + "'s value is " + actualLength + " characters, meeting the minimum of " + minLength + ".");
        }
        return ValidationResult.fail(rule, field, snapshot.value(), "at least " + minLength + " characters", snapshot.confidence(), snapshot.boundingBox(),
                field + "'s value is only " + actualLength + " characters (\"" + snapshot.value() + "\"), below the minimum of " + minLength + " — likely incomplete.");
    }
}
