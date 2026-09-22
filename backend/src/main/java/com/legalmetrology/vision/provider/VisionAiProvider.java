package com.legalmetrology.vision.provider;

/**
 * Strategy interface for multimodal label analysis (Step 4). Concrete
 * providers ({@link GeminiVisionProvider}, {@link ClaudeVisionProvider})
 * send the raw image plus a structured-output prompt and parse the
 * response into a {@link VisionAnalysisResult} — callers never see the
 * provider-specific request/response shape.
 */
public interface VisionAiProvider {

    /** A stable key ("gemini", "claude") matched against {@code ai.vision.provider}. */
    String providerKey();

    VisionAnalysisResult analyzeLabel(byte[] imageBytes, String mimeType);
}
