package com.legalmetrology.rules.dto;

import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.common.enums.Severity;
import com.legalmetrology.common.enums.ViolationStatus;
import com.legalmetrology.vision.provider.VisionBoundingBox;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A persisted {@code violations} row. Field-overlaps with {@link ValidationResultResponse} — see that record's Javadoc for why both exist. */
public record ViolationResponse(
        UUID id,
        UUID inspectionId,
        UUID ruleId,
        String ruleCode,
        String ruleTitle,
        Severity severity,
        DeclarationType field,
        String description,
        String actualValue,
        String expectedValue,
        BigDecimal confidence,
        VisionBoundingBox boundingBox,
        String suggestedFix,
        String legalReference,
        ViolationStatus status,
        Instant createdAt
) {
}
