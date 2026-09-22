package com.legalmetrology.rules.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.exception.BadRequestException;
import com.legalmetrology.inspection.entity.Rule;
import org.springframework.stereotype.Component;

/**
 * Single place every {@link com.legalmetrology.rules.engine.validator.FieldValidator}
 * gets its parameters from — parses {@code rule.validationExpression} as
 * JSON once, with one clear error path when a rule is misconfigured,
 * instead of each validator re-implementing the same parsing/error
 * handling (the "no duplicated validation logic" requirement).
 */
@Component
public class RuleExpressionParser {

    private final ObjectMapper objectMapper;

    public RuleExpressionParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode parse(Rule rule) {
        String expression = rule.getValidationExpression();
        if (expression == null || expression.isBlank()) {
            throw new BadRequestException("Rule " + rule.getRuleCode() + " has no validation_expression configured");
        }
        try {
            return objectMapper.readTree(expression);
        } catch (Exception ex) {
            throw new BadRequestException("Rule " + rule.getRuleCode() + " has an invalid validation_expression: " + ex.getMessage());
        }
    }

    /** Every validation type except CUSTOM (which may target more than one field) requires a single "field" key. */
    public DeclarationType getField(Rule rule) {
        JsonNode node = parse(rule);
        String fieldName = node.path("field").asText(null);
        if (fieldName == null) {
            throw new BadRequestException("Rule " + rule.getRuleCode() + "'s validation_expression is missing required \"field\"");
        }
        try {
            return DeclarationType.valueOf(fieldName);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Rule " + rule.getRuleCode() + " targets unknown declaration type: " + fieldName);
        }
    }
}
