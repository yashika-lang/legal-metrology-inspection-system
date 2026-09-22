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

/** {@code {"field":"MRP","minScore":55}} — reads {@code vision.font.FontAnalysisService}'s 0-100 contrast/edge-density-based readability score. */
@Component
@RequiredArgsConstructor
public class ReadabilityValidator implements FieldValidator {

    private final RuleExpressionParser expressionParser;

    @Override
    public ValidationType supportedType() {
        return ValidationType.READABILITY;
    }

    @Override
    public ValidationResult validate(Rule rule, ValidationContext context) {
        var params = expressionParser.parse(rule);
        DeclarationType field = expressionParser.getField(rule);
        double minScore = params.path("minScore").asDouble(50.0);

        DeclarationSnapshot snapshot = context.get(field);
        if (!snapshot.present() || snapshot.readabilityScore() == null) {
            return ValidationResult.pass(rule, field, null, snapshot.confidence(), snapshot.boundingBox(),
                    field + " has no readability measurement available — skipped.");
        }

        double actual = snapshot.readabilityScore().doubleValue();
        if (actual >= minScore) {
            return ValidationResult.pass(rule, field, String.valueOf(actual), snapshot.confidence(), snapshot.boundingBox(),
                    field + "'s readability score (" + actual + "/100) meets the minimum of " + minScore + ".");
        }
        return ValidationResult.fail(rule, field, String.valueOf(actual), "at least " + minScore, snapshot.confidence(), snapshot.boundingBox(),
                field + "'s readability score (" + actual + "/100) is below the minimum of " + minScore + " — likely low contrast or poor visibility.");
    }
}
