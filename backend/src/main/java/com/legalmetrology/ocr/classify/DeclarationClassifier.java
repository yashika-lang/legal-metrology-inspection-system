package com.legalmetrology.ocr.classify;

import com.legalmetrology.ocr.model.OcrExtractionResult;

import java.util.List;

/**
 * Maps raw OCR text lines onto structured {@link com.legalmetrology.common.enums.DeclarationType}s
 * — "Net Wt. 500g" → NET_QUANTITY, "Packed By" → PACKER, "Customer Care" → CUSTOMER_CARE, etc.
 */
public interface DeclarationClassifier {

    List<ClassifiedField> classify(OcrExtractionResult extraction);
}
