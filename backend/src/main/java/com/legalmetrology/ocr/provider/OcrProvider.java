package com.legalmetrology.ocr.provider;

import com.legalmetrology.ocr.model.OcrExtractionResult;

/**
 * Strategy interface every OCR backend implements. Business logic
 * ({@code ocr.service.OcrService}, the classifier, the normalizer, the
 * fusion engine) depends on this interface only — nothing outside
 * {@code ocr.provider} ever references a Google Vision or Tesseract type
 * directly, so a provider can be added, removed, or reordered in the
 * fallback chain without touching anything else in the module.
 */
public interface OcrProvider {

    /** A stable key ("google-vision", "tesseract") matched against {@code ai.ocr.provider} and used in the fallback chain. */
    String providerKey();

    OcrExtractionResult extractText(byte[] imageBytes, String mimeType);
}
