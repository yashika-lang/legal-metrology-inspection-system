package com.legalmetrology.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Credentials and model selection for Google Gemini — the preferred Vision AI/LLM/embedding provider. */
@ConfigurationProperties(prefix = "ai.gemini")
public record GeminiProperties(
        String apiKey,
        String model,
        String visionModel,
        String embeddingModel,
        int embeddingDimensions,
        String baseUrl
) {
}
