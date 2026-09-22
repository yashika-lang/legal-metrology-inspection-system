package com.legalmetrology.ai.llm.provider;

/**
 * Strategy interface for plain text-completion LLM calls (no image, no
 * chat history) — the building block both {@code ocr.correction.OcrCorrectionService}
 * (Step 3) and the future {@code ai} Compliance Assistant module are built
 * on. Every text-generation feature in this system goes through this
 * interface, never a provider SDK directly.
 */
public interface LlmProvider {

    /** A stable key ("gemini", "claude") matched against {@code ai.llm.provider}. */
    String providerKey();

    LlmCompletion complete(String systemPrompt, String userPrompt);
}
