package com.legalmetrology.ai.llm.provider;

/** Shared cleanup for LLM text responses that were asked to return raw JSON but occasionally wrap it in markdown fences anyway. */
public final class LlmResponseUtil {

    private LlmResponseUtil() {
    }

    public static String stripMarkdownFences(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\n?", "").replaceFirst("```\\s*$", "");
        }
        return trimmed.trim();
    }
}
