package com.legalmetrology.evidence.dto;

import com.legalmetrology.common.enums.Severity;
import com.legalmetrology.vision.provider.VisionBoundingBox;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** API-facing view of one evidence record — never expose {@code Evidence} itself. */
public record EvidenceResponse(
        UUID id,
        String sha256Hash,
        UUID inspectionId,
        UUID imageId,
        UUID violationId,
        String originalImageUrl,
        String annotatedImageUrl,
        VisionBoundingBox boundingBox,
        String ocrText,
        String normalizedValue,
        BigDecimal visionConfidence,
        BigDecimal ocrConfidence,
        BigDecimal fusedConfidence,
        String expectedValue,
        String actualValue,
        String reason,
        String suggestedFix,
        String legalRuleReference,
        Severity severity,
        boolean immutable,
        Instant createdAt
) {
}
