package com.legalmetrology.ocr.service;

import com.legalmetrology.inspection.entity.OCRResult;
import com.legalmetrology.ocr.dto.OcrRunResponse;

import java.util.List;
import java.util.UUID;

/**
 * Steps 2-3 facade: runs OCR (provider-agnostic, with automatic fallback),
 * classifies the extracted text into declarations, normalizes them, and
 * corrects the raw text via an LLM — all in one call per image. This is
 * the only entry point the rest of the system (controllers, the future
 * pipeline orchestrator) needs; nothing outside {@code ocr.provider}
 * knows which OCR backend actually ran.
 */
public interface OcrService {

    OcrRunResponse run(UUID imageId);

    /** Full OCR run history for an image (every {@code OCRResult} row), most recent first — for debugging and run-to-run comparison. */
    List<OCRResult> getHistory(UUID imageId);
}
