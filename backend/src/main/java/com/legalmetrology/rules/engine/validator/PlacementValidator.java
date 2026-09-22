package com.legalmetrology.rules.engine.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.common.enums.LabelSection;
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

/** {@code {"field":"MRP","allowedSections":["FRONT"]}} — the declaration must appear on one of the listed label faces. */
@Component
@RequiredArgsConstructor
public class PlacementValidator implements FieldValidator {

    private final RuleExpressionParser expressionParser;

    @Override
    public ValidationType supportedType() {
        return ValidationType.PLACEMENT;
    }

    @Override
    public ValidationResult validate(Rule rule, ValidationContext context) {
        var params = expressionParser.parse(rule);
        DeclarationType field = expressionParser.getField(rule);

        List<LabelSection> allowedSections = new ArrayList<>();
        for (JsonNode value : params.path("allowedSections")) {
            try {
                allowedSections.add(LabelSection.valueOf(value.asText()));
            } catch (IllegalArgumentException ignored) {
                // unrecognized section name in config — ignored rather than failing the whole rule
            }
        }

        DeclarationSnapshot snapshot = context.get(field);
        if (!snapshot.present() || snapshot.labelSection() == null) {
            return ValidationResult.pass(rule, field, null, snapshot.confidence(), snapshot.boundingBox(),
                    field + " has no known label-section placement — skipped.");
        }

        if (allowedSections.contains(snapshot.labelSection())) {
            return ValidationResult.pass(rule, field, snapshot.labelSection().name(), snapshot.confidence(), snapshot.boundingBox(),
                    field + " appears on the " + snapshot.labelSection() + " panel, which is an allowed placement.");
        }
        return ValidationResult.fail(rule, field, snapshot.labelSection().name(), "one of " + allowedSections, snapshot.confidence(), snapshot.boundingBox(),
                field + " was found on the " + snapshot.labelSection() + " panel, not " + allowedSections + " as required.");
    }
}
