package com.legalmetrology.ocr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Selects and configures the {@code OcrProvider} chain (Google Vision primary, Tesseract fallback). */
@ConfigurationProperties(prefix = "ai.ocr")
public record OcrProperties(
        String provider,
        GoogleVision googleVision,
        Tesseract tesseract
) {
    /** {@code apiKey} is the simple auth path; {@code projectId} is optional and only used to set the quota-project header when authenticating via a service account (see {@code GoogleCloudCredentialsProvider}). */
    public record GoogleVision(String apiKey, String projectId) {
    }

    public record Tesseract(String dataPath, String language) {
    }
}
