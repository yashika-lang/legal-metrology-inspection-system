package com.legalmetrology.rules.dto;

import com.legalmetrology.common.enums.Severity;
import com.legalmetrology.common.enums.ValidationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RuleRequest(

        @NotBlank(message = "Rule number/code is required")
        String ruleCode,

        @NotBlank(message = "Title is required")
        String title,

        String description,

        String legalReference,

        String category,

        @NotNull(message = "Severity is required")
        Severity severity,

        @NotNull(message = "Validation type is required")
        ValidationType validationType,

        @NotBlank(message = "Validation expression is required")
        String validationExpression,

        boolean mandatory,

        String suggestion,

        String penaltyReference
) {
}
