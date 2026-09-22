package com.legalmetrology.rules.engine.validator;

import com.legalmetrology.common.enums.ValidationType;
import com.legalmetrology.exception.BadRequestException;
import com.legalmetrology.inspection.entity.Rule;
import com.legalmetrology.rules.engine.RuleExpressionParser;
import com.legalmetrology.rules.engine.ValidationContext;
import com.legalmetrology.rules.engine.custom.CustomRuleStrategy;
import com.legalmetrology.rules.model.ValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** {@code {"strategy":"IMPORTER_REQUIRES_ORIGIN"}} — dispatches to the named {@link CustomRuleStrategy} bean. */
@Component
@RequiredArgsConstructor
public class CustomValidator implements FieldValidator {

    private final List<CustomRuleStrategy> strategies;
    private final RuleExpressionParser expressionParser;

    private Map<String, CustomRuleStrategy> strategiesByKey;

    @Override
    public ValidationType supportedType() {
        return ValidationType.CUSTOM;
    }

    @Override
    public ValidationResult validate(Rule rule, ValidationContext context) {
        var params = expressionParser.parse(rule);
        String strategyKey = params.path("strategy").asText(null);
        if (strategyKey == null) {
            throw new BadRequestException("Rule " + rule.getRuleCode() + "'s CUSTOM validation_expression is missing \"strategy\"");
        }

        CustomRuleStrategy strategy = byKey().get(strategyKey);
        if (strategy == null) {
            throw new BadRequestException("No CustomRuleStrategy registered for key \"" + strategyKey + "\" (rule " + rule.getRuleCode() + ")");
        }
        return strategy.evaluate(rule, context);
    }

    private Map<String, CustomRuleStrategy> byKey() {
        if (strategiesByKey == null) {
            strategiesByKey = strategies.stream().collect(Collectors.toMap(CustomRuleStrategy::key, Function.identity()));
        }
        return strategiesByKey;
    }
}
