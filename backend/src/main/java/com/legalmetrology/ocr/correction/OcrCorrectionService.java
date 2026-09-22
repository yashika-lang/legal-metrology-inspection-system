package com.legalmetrology.ocr.correction;

/**
 * Step 3: fixes common OCR mistakes an LLM can recognize from context that
 * regex normalization can't — digit/letter confusions ("₹I2O" → "₹120",
 * "1OOg" → "100g"), spelling ("Maharastra" → "Maharashtra") — and reports
 * its own confidence in the rewrite.
 */
public interface OcrCorrectionService {

    CorrectionResult correct(String rawText);
}
