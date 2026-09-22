package com.legalmetrology.vision.dto;

import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.common.enums.DetectionSource;
import com.legalmetrology.common.enums.FontIssue;
import com.legalmetrology.common.enums.LabelSection;
import com.legalmetrology.vision.provider.VisionBoundingBox;

import java.math.BigDecimal;
import java.util.UUID;

public record LabelDetectionResponse(
        UUID id,
        UUID imageId,
        DeclarationType declarationType,
        String detectedValue,
        BigDecimal confidence,
        DetectionSource source,
        VisionBoundingBox boundingBox,
        boolean present,
        LabelSection labelSection,
        BigDecimal fontSizeEstimate,
        BigDecimal readabilityScore,
        BigDecimal contrastScore,
        FontIssue fontIssue
) {
}
