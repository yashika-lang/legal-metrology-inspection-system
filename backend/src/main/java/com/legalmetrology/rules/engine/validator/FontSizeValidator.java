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
 * {@code {"field":"NET_QUANTITY","minSizePercent":1.2}} — reads
 * {@code vision.font.FontAnalysisService}'s relative font-height estimate
 * (bounding-box height as % of image height; see that service's Javadoc
 * for why this is relative, not an absolute millimeter measurement).
 */
@Component
@RequiredArgsConstructor
public class FontSizeValidator implements FieldValidator {

    private final RuleExpressionParser expressionParser;

    @Override
    public ValidationType supportedType() {
        return ValidationType.FONT_SIZE;
    }

    @Override
    public ValidationResult validate(Rule rule, ValidationContext context) {
        var params = expressionParser.parse(rule);
        DeclarationType field = expressionParser.getField(rule);
        double minSizePercent = params.path("minSizePercent").asDouble(1.0);

        DeclarationSnapshot snapshot = context.get(field);
        if (!snapshot.present() || snapshot.fontSizeEstimate() == null) {
            return ValidationResult.pass(rule, field, null, snapshot.confidence(), snapshot.boundingBox(),
                    field + " has no font-size measurement available (not detected, or not visually localized) — skipped.");
        }

        double actual = snapshot.fontSizeEstimate().doubleValue();
        if (actual >= minSizePercent) {
            return ValidationResult.pass(rule, field, actual + "%", snapshot.confidence(), snapshot.boundingBox(),
                    field + "'s estimated font height (" + actual + "% of image height) meets the minimum of " + minSizePercent + "%.");
        }
        return ValidationResult.fail(rule, field, actual + "%", "at least " + minSizePercent + "%", snapshot.confidence(), snapshot.boundingBox(),
                field + "'s estimated font height (" + actual + "% of image height) is below the minimum of " + minSizePercent + "% — text may be too small to read.");
    }
}
