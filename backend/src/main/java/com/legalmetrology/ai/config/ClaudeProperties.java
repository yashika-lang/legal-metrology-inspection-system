package com.legalmetrology.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Credentials and model selection for the Anthropic Claude API — the configurable alternate Vision AI/LLM provider. */
@ConfigurationProperties(prefix = "ai.claude")
public record ClaudeProperties(
        String apiKey,
        String model,
        String baseUrl
) {
}
