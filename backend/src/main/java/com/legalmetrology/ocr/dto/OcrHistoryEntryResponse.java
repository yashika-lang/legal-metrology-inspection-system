package com.legalmetrology.ocr.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OcrHistoryEntryResponse(
        UUID id,
        String provider,
        String detectedLanguage,
        String rawText,
        String correctedText,
        BigDecimal confidence,
        BigDecimal correctionConfidence,
        Instant runAt
) {
}
