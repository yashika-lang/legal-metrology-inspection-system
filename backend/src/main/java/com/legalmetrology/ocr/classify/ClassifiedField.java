package com.legalmetrology.ocr.classify;

import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.ocr.model.LanguageCode;
import com.legalmetrology.vision.provider.VisionBoundingBox;

/** One OCR line/phrase classified as a specific declaration, with its normalized value ready to persist as a {@code LabelDetection}. */
public record ClassifiedField(
        DeclarationType declarationType,
        String rawText,
        String normalizedValue,
        double confidence,
        VisionBoundingBox boundingBox,
        LanguageCode language
) {
}
