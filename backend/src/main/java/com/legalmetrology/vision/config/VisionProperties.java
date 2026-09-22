package com.legalmetrology.vision.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Selects which {@code VisionAiProvider} bean the {@code vision} module's label-analysis service uses. */
@ConfigurationProperties(prefix = "ai.vision")
public record VisionProperties(String provider) {
}
