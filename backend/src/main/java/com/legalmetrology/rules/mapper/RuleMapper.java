package com.legalmetrology.rules.mapper;

import com.legalmetrology.inspection.entity.Rule;
import com.legalmetrology.rules.dto.RuleResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RuleMapper {

    default RuleResponse toResponse(Rule rule) {
        if (rule == null) {
            return null;
        }
        return new RuleResponse(
                rule.getId(),
                rule.getRuleCode(),
                rule.getTitle(),
                rule.getDescription(),
                rule.getLegalReference(),
                rule.getCategory(),
                rule.getSeverity(),
                rule.getValidationType(),
                rule.getValidationExpression(),
                rule.isMandatory(),
                rule.getSuggestion(),
                rule.getPenaltyReference(),
                rule.getVersion(),
                rule.getEffectiveDate(),
                rule.isActive()
        );
    }
}
