package com.legalmetrology.common.enums;

/** How a {@code LabelDetection} row was produced. */
public enum DetectionSource {
    /** Parsed out of the OCR'd/corrected text of the image. */
    OCR_TEXT,
    /** Located directly on the image by a multimodal Vision AI provider, with a bounding box. */
    VISION_AI
}
