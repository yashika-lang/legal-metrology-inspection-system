package com.legalmetrology.ocr.model;

import com.legalmetrology.vision.provider.VisionBoundingBox;

/** The finest grain of the OCR hierarchy: one word, its box, and the provider's confidence in it. */
public record OcrWord(String text, VisionBoundingBox boundingBox, double confidence) {
}
