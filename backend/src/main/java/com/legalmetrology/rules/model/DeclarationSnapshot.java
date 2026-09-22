package com.legalmetrology.rules.model;

import com.legalmetrology.common.enums.DeclarationType;
import com.legalmetrology.common.enums.LabelSection;
import com.legalmetrology.vision.provider.VisionBoundingBox;

import java.math.BigDecimal;

/**
 * The rule engine's entire view of one declaration for an inspection —
 * deliberately provider-agnostic: no field here says whether the value
 * came from Google Vision, Tesseract, Gemini, or Claude, or even whether
 * it came from OCR text or multimodal Vision AI at all. The engine
 * consumes only this shape (built from {@code ocr.entity.FusedDeclaration}
 * by the one adapter method that's allowed to know about that), so
 * swapping or adding an upstream provider never touches {@code rules}.
 */
public record DeclarationSnapshot(
        DeclarationType type,
        boolean present,
        String value,
        double confidence,
        VisionBoundingBox boundingBox,
        BigDecimal fontSizeEstimate,
        BigDecimal readabilityScore,
        BigDecimal contrastScore,
        LabelSection labelSection
) {
    public static DeclarationSnapshot absent(DeclarationType type) {
        return new DeclarationSnapshot(type, false, null, 0.0, null, null, null, null, null);
    }
}
