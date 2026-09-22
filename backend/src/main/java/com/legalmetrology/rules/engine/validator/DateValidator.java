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

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * {@code {"field":"MFG_MONTH","format":"yyyy-MM","notFuture":true}} —
 * validates the value parses as a year-month (matching the format
 * {@code ocr.normalize.OcrNormalizationService} normalizes month/year
 * declarations to) and, optionally, isn't in the future.
 */
@Component
@RequiredArgsConstructor
public class DateValidator implements FieldValidator {

    private static final String DEFAULT_FORMAT = "yyyy-MM";

    private final RuleExpressionParser expressionParser;

    @Override
    public ValidationType supportedType() {
        return ValidationType.DATE;
    }

    @Override
    public ValidationResult validate(Rule rule, ValidationContext context) {
        var params = expressionParser.parse(rule);
        DeclarationType field = expressionParser.getField(rule);
        String format = params.path("format").asText(DEFAULT_FORMAT);
        boolean notFuture = params.path("notFuture").asBoolean(false);

        DeclarationSnapshot snapshot = context.get(field);
        if (!snapshot.present() || snapshot.value() == null) {
            return ValidationResult.pass(rule, field, null, snapshot.confidence(), snapshot.boundingBox(), field + " was not detected.");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        YearMonth parsed;
        try {
            parsed = YearMonth.parse(snapshot.value(), formatter);
        } catch (DateTimeParseException ex) {
            return ValidationResult.fail(rule, field, snapshot.value(), "a valid date in " + format + " format", snapshot.confidence(), snapshot.boundingBox(),
                    "\"" + snapshot.value() + "\" for " + field + " could not be parsed as a valid date.");
        }

        if (notFuture && parsed.isAfter(YearMonth.now())) {
            return ValidationResult.fail(rule, field, snapshot.value(), "not later than " + YearMonth.now(), snapshot.confidence(), snapshot.boundingBox(),
                    field + " (\"" + snapshot.value() + "\") is in the future, which is not plausible for a manufacturing date.");
        }

        return ValidationResult.pass(rule, field, snapshot.value(), snapshot.confidence(), snapshot.boundingBox(),
                field + " (\"" + snapshot.value() + "\") is a valid" + (notFuture ? ", non-future" : "") + " date.");
    }
}
