package com.legalmetrology.ocr.model;

import java.time.Instant;
import java.util.List;

/**
 * The provider-agnostic, structured output of one {@code OcrProvider} run —
 * full text hierarchy (paragraphs → lines → words, each with coordinates
 * and confidence), the provider that produced it, and when. Business logic
 * (field classification, correction, fusion) works exclusively against this
 * shape, never against a provider's raw response.
 */
public record OcrExtractionResult(
        String providerKey,
        LanguageCode detectedLanguage,
        String fullText,
        List<OcrParagraph> paragraphs,
        double overallConfidence,
        Instant timestamp
) {
    public static OcrExtractionResult empty(String providerKey) {
        return new OcrExtractionResult(providerKey, LanguageCode.UNKNOWN, "", List.of(), 0.0, Instant.now());
    }
}
