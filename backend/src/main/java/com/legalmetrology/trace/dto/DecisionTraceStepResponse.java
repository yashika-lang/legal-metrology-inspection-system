package com.legalmetrology.trace.dto;

import com.legalmetrology.common.enums.TraceStatus;
import com.legalmetrology.common.enums.TraceStepName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** API-facing view of one decision trace step — never expose {@code DecisionTraceStep} itself. */
public record DecisionTraceStepResponse(
        UUID id,
        UUID inspectionId,
        TraceStepName stepName,
        String module,
        String inputSummary,
        String outputSummary,
        BigDecimal confidence,
        long executionTimeMs,
        Instant startedAt,
        TraceStatus status,
        String reason,
        UUID referencedRuleId,
        UUID referencedEvidenceId,
        UUID referencedImageId,
        Instant createdAt
) {
}
