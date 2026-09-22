package com.legalmetrology.ocr.correction;

public record CorrectionResult(String correctedText, double confidence, String changesSummary) {
}
