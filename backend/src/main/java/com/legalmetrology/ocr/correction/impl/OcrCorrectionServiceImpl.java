package com.legalmetrology.ocr.correction.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalmetrology.ai.llm.provider.LlmCompletion;
import com.legalmetrology.ai.llm.provider.LlmProviderFactory;
import com.legalmetrology.ai.llm.provider.LlmResponseUtil;
import com.legalmetrology.ocr.correction.CorrectionResult;
import com.legalmetrology.ocr.correction.OcrCorrectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrCorrectionServiceImpl implements OcrCorrectionService {

    private static final String SYSTEM_PROMPT = """
            You are correcting raw OCR output from a photograph of an Indian packaged-commodity
            label. OCR frequently confuses visually similar characters (I/1, O/0, S/5, l/1, B/8),
            mis-splits currency symbols, and garbles regional place names. Fix ONLY clear OCR
            errors — preserve the original wording, language, and formatting otherwise. Do not
            invent information that isn't implied by the raw text. Do not translate between
            languages.

            Respond with ONLY a single JSON object, no markdown fences, no commentary:
            {
              "correctedText": "the corrected text",
              "confidence": 0.0-1.0 (how confident you are the corrections are right),
              "changesSummary": "a short human-readable list of what was changed, or empty if nothing was changed"
            }
            """;

    private final LlmProviderFactory llmProviderFactory;
    private final ObjectMapper objectMapper;

    @Override
    public CorrectionResult correct(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return new CorrectionResult(rawText, 1.0, "");
        }

        LlmCompletion completion = llmProviderFactory.resolve().complete(SYSTEM_PROMPT, rawText);

        try {
            JsonNode root = objectMapper.readTree(LlmResponseUtil.stripMarkdownFences(completion.text()));
            String correctedText = root.path("correctedText").asText(rawText);
            double confidence = root.path("confidence").asDouble(0.5);
            String changesSummary = root.path("changesSummary").asText("");
            return new CorrectionResult(correctedText, confidence, changesSummary);
        } catch (Exception ex) {
            log.warn("Failed to parse OCR correction response, keeping raw text unmodified: {}", ex.getMessage());
            return new CorrectionResult(rawText, 0.0, "Correction step failed to parse — raw text kept as-is");
        }
    }
}
