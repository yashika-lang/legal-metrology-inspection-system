package com.legalmetrology.vision.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalmetrology.ai.config.GeminiProperties;
import com.legalmetrology.exception.BadRequestException;
import com.legalmetrology.exception.ServiceNotConfiguredException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * {@link VisionAiProvider} backed by Google Gemini's multimodal
 * {@code generateContent} endpoint — the preferred Vision AI provider (see
 * docs/ARCHITECTURE.md §7/§14). Requests the image inline (base64) alongside
 * the structured-output instruction in {@link VisionPrompts}, and asks
 * Gemini to constrain its response to JSON via {@code responseMimeType}.
 */
@Slf4j
@Component
public class GeminiVisionProvider implements VisionAiProvider {

    private final WebClient geminiWebClient;
    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;

    public GeminiVisionProvider(@Qualifier("geminiWebClient") WebClient geminiWebClient,
                                 GeminiProperties properties,
                                 ObjectMapper objectMapper) {
        this.geminiWebClient = geminiWebClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String providerKey() {
        return "gemini";
    }

    @Override
    public VisionAnalysisResult analyzeLabel(byte[] imageBytes, String mimeType) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw ServiceNotConfiguredException.apiKeyMissing("Gemini");
        }
        long startedAtMs = System.currentTimeMillis();

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(
                                Map.of("text", VisionPrompts.buildInstruction()),
                                Map.of("inline_data", Map.of(
                                        "mime_type", mimeType,
                                        "data", Base64.getEncoder().encodeToString(imageBytes)))
                        )
                )),
                "generationConfig", Map.of("responseMimeType", "application/json")
        );

        JsonNode response = geminiWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/models/{model}:generateContent")
                        .queryParam("key", properties.apiKey())
                        .build(properties.visionModel()))
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        String jsonText = extractResponseText(response);
        VisionAnalysisResult parsed = VisionResponseParser.parse(jsonText, objectMapper);

        int latencyMs = (int) (System.currentTimeMillis() - startedAtMs);
        return new VisionAnalysisResult(parsed.labelSection(), parsed.declarations(), properties.visionModel(), latencyMs);
    }

    private String extractResponseText(JsonNode response) {
        if (response == null) {
            throw new BadRequestException("Gemini Vision returned an empty response");
        }
        JsonNode textNode = response.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (textNode.isMissingNode() || textNode.asText().isBlank()) {
            log.error("Unexpected Gemini Vision response shape: {}", response);
            throw new BadRequestException("Gemini Vision response did not contain the expected text payload");
        }
        return textNode.asText();
    }
}
