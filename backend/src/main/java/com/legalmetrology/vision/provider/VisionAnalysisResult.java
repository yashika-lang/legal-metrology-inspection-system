package com.legalmetrology.vision.provider;

import com.legalmetrology.common.enums.LabelSection;

import java.util.List;

public record VisionAnalysisResult(
        LabelSection labelSection,
        List<DetectedDeclaration> declarations,
        String model,
        int latencyMs
) {
}
