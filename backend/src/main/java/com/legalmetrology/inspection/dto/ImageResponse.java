package com.legalmetrology.inspection.dto;

import com.legalmetrology.common.enums.ImageRecommendedAction;
import com.legalmetrology.common.enums.ImageType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ImageResponse(
        UUID id,
        UUID inspectionId,
        String storagePath,
        String signedUrl,
        ImageType imageType,
        BigDecimal qualityScore,
        List<String> qualityWarnings,
        ImageRecommendedAction recommendedAction,
        Instant uploadedAt
) {
}
