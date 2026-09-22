package com.legalmetrology.rules.dto;

import com.legalmetrology.common.enums.Severity;
import com.legalmetrology.common.enums.ValidationType;

import java.time.LocalDate;
import java.util.UUID;

public record RuleResponse(
        UUID id,
        String ruleCode,
        String title,
        String description,
        String legalReference,
        String category,
        Severity severity,
        ValidationType validationType,
        String validationExpression,
        boolean mandatory,
        String suggestion,
        String penaltyReference,
        int version,
        LocalDate effectiveDate,
        boolean active
) {
}
