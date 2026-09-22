package com.legalmetrology.rules.dto;

import com.legalmetrology.common.enums.FraudRisk;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record EvaluationResponse(
        UUID inspectionId,
        BigDecimal complianceScore,
        FraudRisk fraudRisk,
        List<ViolationResponse> violations,
        List<ValidationResultResponse> results
) {
}
