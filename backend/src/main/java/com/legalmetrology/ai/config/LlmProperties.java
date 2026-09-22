package com.legalmetrology.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Selects which {@code LlmProvider} bean the {@code ai} module's text-generation services use. */
@ConfigurationProperties(prefix = "ai.llm")
public record LlmProperties(String provider) {
}
