package com.legalmetrology.ocr.model;

import com.legalmetrology.vision.provider.VisionBoundingBox;

import java.util.List;

public record OcrParagraph(String text, VisionBoundingBox boundingBox, double confidence,
                            LanguageCode language, List<OcrLine> lines) {
}
