package com.legalmetrology.vision.provider;

import com.legalmetrology.common.enums.DeclarationType;

/** One declaration as reported by a Vision AI provider — present or explicitly (confidently) absent. */
public record DetectedDeclaration(
        DeclarationType type,
        boolean present,
        String value,
        double confidence,
        VisionBoundingBox boundingBox
) {
}
