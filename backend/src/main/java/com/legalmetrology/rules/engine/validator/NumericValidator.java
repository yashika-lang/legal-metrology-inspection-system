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

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** {@code {"field":"MRP","min":1,"max":100000}} — extracts the numeric portion of a normalized value (e.g. "₹120" → 120) and range-checks it. Bounds are optional. */
@Component
@RequiredArgsConstructor
public class NumericValidator implements FieldValidator {

    private static final Pattern NUMBER = Pattern.compile("[\\d,]+(?:\\.\\d+)?");

    private final RuleExpressionParser expressionParser;

    @Override
    public ValidationType supportedType() {
        return ValidationType.NUMERIC;
    }

    @Override
    public ValidationResult validate(Rule rule, ValidationContext context) {
        var params = expressionParser.parse(rule);
        DeclarationType field = expressionParser.getField(rule);

        DeclarationSnapshot snapshot = context.get(field);
        if (!snapshot.present() || snapshot.value() == null) {
            return ValidationResult.pass(rule, field, null, snapshot.confidence(), snapshot.boundingBox(), field + " was not detected.");
        }

        Matcher matcher = NUMBER.matcher(snapshot.value());
        if (!matcher.find()) {
            return ValidationResult.fail(rule, field, snapshot.value(), "a numeric value", snapshot.confidence(), snapshot.boundingBox(),
                    "\"" + snapshot.value() + "\" for " + field + " does not contain a recognizable number.");
        }

        BigDecimal number = new BigDecimal(matcher.group().replace(",", ""));

        if (params.hasNonNull("min") && number.compareTo(BigDecimal.valueOf(params.path("min").asDouble())) < 0) {
            return ValidationResult.fail(rule, field, snapshot.value(), "at least " + params.path("min").asText(), snapshot.confidence(), snapshot.boundingBox(),
                    field + "'s value " + number + " is below the minimum of " + params.path("min").asText() + ".");
        }
        if (params.hasNonNull("max") && number.compareTo(BigDecimal.valueOf(params.path("max").asDouble())) > 0) {
            return ValidationResult.fail(rule, field, snapshot.value(), "at most " + params.path("max").asText(), snapshot.confidence(), snapshot.boundingBox(),
                    field + "'s value " + number + " exceeds the maximum of " + params.path("max").asText() + ".");
        }

        return ValidationResult.pass(rule, field, snapshot.value(), snapshot.confidence(), snapshot.boundingBox(),
                field + "'s value " + number + " is a valid number within range.");
    }
}
