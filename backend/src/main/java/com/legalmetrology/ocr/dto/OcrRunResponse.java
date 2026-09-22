package com.legalmetrology.ocr.dto;

import com.legalmetrology.vision.dto.LabelDetectionResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OcrRunResponse(
        UUID id,
        UUID imageId,
        String provider,
        String detectedLanguage,
        String rawText,
        String correctedText,
        BigDecimal ocrConfidence,
        BigDecimal correctionConfidence,
        String correctionChangesSummary,
        List<LabelDetectionResponse> fields,
        Instant runAt
) {
}
