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

/** {@code {"field":"PRODUCT_NAME","maxLength":150}} — an unusually long value is usually OCR noise, not a real declaration. */
@Component
@RequiredArgsConstructor
public class MaxLengthValidator implements FieldValidator {

    private final RuleExpressionParser expressionParser;

    @Override
    public ValidationType supportedType() {
        return ValidationType.MAX_LENGTH;
    }

    @Override
    public ValidationResult validate(Rule rule, ValidationContext context) {
        var params = expressionParser.parse(rule);
        DeclarationType field = expressionParser.getField(rule);
        int maxLength = params.path("maxLength").asInt(Integer.MAX_VALUE);

        DeclarationSnapshot snapshot = context.get(field);
        if (!snapshot.present() || snapshot.value() == null) {
            return ValidationResult.pass(rule, field, null, snapshot.confidence(), snapshot.boundingBox(), field + " was not detected.");
        }

        int actualLength = snapshot.value().trim().length();
        if (actualLength <= maxLength) {
            return ValidationResult.pass(rule, field, snapshot.value(), snapshot.confidence(), snapshot.boundingBox(),
                    field + "'s value is " + actualLength + " characters, within the limit of " + maxLength + ".");
        }
        return ValidationResult.fail(rule, field, snapshot.value(), "at most " + maxLength + " characters", snapshot.confidence(), snapshot.boundingBox(),
                field + "'s value is " + actualLength + " characters, exceeding the limit of " + maxLength + " — likely contains OCR noise.");
    }
}
