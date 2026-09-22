package com.legalmetrology.rules.engine.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.common.enums.ValidationType;
import com.legalmetrology.inspection.entity.Rule;
import com.legalmetrology.rules.engine.RuleExpressionParser;
import com.legalmetrology.rules.engine.ValidationContext;
import com.legalmetrology.rules.model.DeclarationSnapshot;
import com.legalmetrology.rules.model.ValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** {@code {"field":"COUNTRY_OF_ORIGIN","allowedValues":["India","China",...]}} — case-insensitive membership check. */
@Component
@RequiredArgsConstructor
public class EnumValidator implements FieldValidator {

    private final RuleExpressionParser expressionParser;

    @Override
    public ValidationType supportedType() {
        return ValidationType.ENUM;
    }

    @Override
    public ValidationResult validate(Rule rule, ValidationContext context) {
        var params = expressionParser.parse(rule);
        DeclarationType field = expressionParser.getField(rule);

        List<String> allowedValues = new ArrayList<>();
        for (JsonNode value : params.path("allowedValues")) {
            allowedValues.add(value.asText());
        }

        DeclarationSnapshot snapshot = context.get(field);
        if (!snapshot.present() || snapshot.value() == null) {
            return ValidationResult.pass(rule, field, null, snapshot.confidence(), snapshot.boundingBox(), field + " was not detected.");
        }

        boolean matches = allowedValues.stream().anyMatch(allowed -> allowed.equalsIgnoreCase(snapshot.value().trim()));
        if (matches) {
            return ValidationResult.pass(rule, field, snapshot.value(), snapshot.confidence(), snapshot.boundingBox(),
                    "\"" + snapshot.value() + "\" is a recognized value for " + field + ".");
        }
        return ValidationResult.fail(rule, field, snapshot.value(), "one of " + allowedValues, snapshot.confidence(), snapshot.boundingBox(),
                "\"" + snapshot.value() + "\" is not among the recognized values for " + field + " — possibly OCR noise or an unlisted value.");
    }
}
