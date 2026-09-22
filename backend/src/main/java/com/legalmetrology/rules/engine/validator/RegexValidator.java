package com.legalmetrology.rules.engine.validator;

import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.common.enums.ValidationType;
import com.legalmetrology.exception.BadRequestException;
import com.legalmetrology.inspection.entity.Rule;
import com.legalmetrology.rules.engine.RuleExpressionParser;
import com.legalmetrology.rules.engine.ValidationContext;
import com.legalmetrology.rules.model.DeclarationSnapshot;
import com.legalmetrology.rules.model.ValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** {@code {"field":"MRP","pattern":"^₹\\d+(\\.\\d{2})?$"}} — the field's value must match a regex. Skipped (not failed) when the field is absent — that's FIELD_EXISTS/FIELD_NOT_EMPTY's job. */
@Component
@RequiredArgsConstructor
public class RegexValidator implements FieldValidator {

    private final RuleExpressionParser expressionParser;

    @Override
    public ValidationType supportedType() {
        return ValidationType.REGEX;
    }

    @Override
    public ValidationResult validate(Rule rule, ValidationContext context) {
        var params = expressionParser.parse(rule);
        DeclarationType field = expressionParser.getField(rule);
        String patternText = params.path("pattern").asText(null);
        if (patternText == null) {
            throw new BadRequestException("Rule " + rule.getRuleCode() + "'s REGEX validation_expression is missing \"pattern\"");
        }

        DeclarationSnapshot snapshot = context.get(field);
        if (!snapshot.present() || snapshot.value() == null) {
            return ValidationResult.pass(rule, field, null, snapshot.confidence(), snapshot.boundingBox(),
                    field + " was not detected, so its format could not be checked (see the presence rule for this field).");
        }

        Pattern pattern;
        try {
            pattern = Pattern.compile(patternText);
        } catch (PatternSyntaxException ex) {
            throw new BadRequestException("Rule " + rule.getRuleCode() + " has an invalid regex pattern: " + ex.getMessage());
        }

        boolean matches = pattern.matcher(snapshot.value()).find();
        if (matches) {
            return ValidationResult.pass(rule, field, snapshot.value(), snapshot.confidence(), snapshot.boundingBox(),
                    "\"" + snapshot.value() + "\" matches the required format for " + field + ".");
        }
        return ValidationResult.fail(rule, field, snapshot.value(), "matches pattern " + patternText, snapshot.confidence(), snapshot.boundingBox(),
                "\"" + snapshot.value() + "\" does not match the required format for " + field + " (" + patternText + ").");
    }
}
