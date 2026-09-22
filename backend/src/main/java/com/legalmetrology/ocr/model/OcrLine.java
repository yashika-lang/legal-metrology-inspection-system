package com.legalmetrology.ocr.model;

import com.legalmetrology.vision.provider.VisionBoundingBox;

import java.util.List;

public record OcrLine(String text, VisionBoundingBox boundingBox, double confidence, List<OcrWord> words) {
}
