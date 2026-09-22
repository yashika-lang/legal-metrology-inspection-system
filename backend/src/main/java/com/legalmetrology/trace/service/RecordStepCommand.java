package com.legalmetrology.trace.service;

import com.legalmetrology.common.enums.TraceStatus;
import com.legalmetrology.common.enums.TraceStepName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Everything one {@code DecisionTraceStep} row needs. The {@code success}/
 * {@code failure} factories take the step's own start-time (captured by the
 * caller as {@code System.currentTimeMillis()} before doing the work) and
 * compute {@code executionTimeMs} internally, so call sites stay a
 * three-line try/catch instead of repeating duration math everywhere.
 */
public record RecordStepCommand(
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
        UUID referencedImageId
) {
    public static RecordStepCommand success(UUID inspectionId, TraceStepName stepName, String module,
                                             String inputSummary, String outputSummary, BigDecimal confidence, long startedAtMs) {
        return new RecordStepCommand(inspectionId, stepName, module, inputSummary, outputSummary, confidence,
                System.currentTimeMillis() - startedAtMs, Instant.ofEpochMilli(startedAtMs), TraceStatus.SUCCESS, null,
                null, null, null);
    }

    public static RecordStepCommand failure(UUID inspectionId, TraceStepName stepName, String module,
                                             String inputSummary, String reason, long startedAtMs) {
        return new RecordStepCommand(inspectionId, stepName, module, inputSummary, null, null,
                System.currentTimeMillis() - startedAtMs, Instant.ofEpochMilli(startedAtMs), TraceStatus.FAILED, reason,
                null, null, null);
    }

    public RecordStepCommand withReferences(UUID referencedRuleId, UUID referencedEvidenceId, UUID referencedImageId) {
        return new RecordStepCommand(inspectionId, stepName, module, inputSummary, outputSummary, confidence,
                executionTimeMs, startedAt, status, reason, referencedRuleId, referencedEvidenceId, referencedImageId);
    }
}
