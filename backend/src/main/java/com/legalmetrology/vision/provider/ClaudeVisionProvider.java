package com.legalmetrology.vision.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalmetrology.ai.config.ClaudeProperties;
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
 * {@link VisionAiProvider} backed by Anthropic Claude's vision-capable
 * Messages API — the configurable alternate/second-opinion provider (see
 * docs/ARCHITECTURE.md §7/§14). Same {@link VisionAnalysisResult} contract
 * as {@link GeminiVisionProvider}, so callers (and {@code VisionAiProviderFactory})
 * can swap between them with a single config value.
 */
@Slf4j
@Component
public class ClaudeVisionProvider implements VisionAiProvider {

    private static final int MAX_TOKENS = 4096;

    private final WebClient claudeWebClient;
    private final ClaudeProperties properties;
    private final ObjectMapper objectMapper;

    public ClaudeVisionProvider(@Qualifier("claudeWebClient") WebClient claudeWebClient,
                                 ClaudeProperties properties,
                                 ObjectMapper objectMapper) {
        this.claudeWebClient = claudeWebClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String providerKey() {
        return "claude";
    }

    @Override
    public VisionAnalysisResult analyzeLabel(byte[] imageBytes, String mimeType) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw ServiceNotConfiguredException.apiKeyMissing("Claude");
        }
        long startedAtMs = System.currentTimeMillis();

        Map<String, Object> requestBody = Map.of(
                "model", properties.model(),
                "max_tokens", MAX_TOKENS,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "image", "source", Map.of(
                                        "type", "base64", "media_type", mimeType,
                                        "data", Base64.getEncoder().encodeToString(imageBytes))),
                                Map.of("type", "text", "text", VisionPrompts.buildInstruction()
                                        + "\n\nRemember: respond with ONLY the JSON object, nothing else.")
                        )
                ))
        );

        JsonNode response = claudeWebClient.post()
                .uri("/messages")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        String jsonText = extractResponseText(response);
        VisionAnalysisResult parsed = VisionResponseParser.parse(jsonText, objectMapper);

        int latencyMs = (int) (System.currentTimeMillis() - startedAtMs);
        return new VisionAnalysisResult(parsed.labelSection(), parsed.declarations(), properties.model(), latencyMs);
    }

    private String extractResponseText(JsonNode response) {
        if (response == null) {
            throw new BadRequestException("Claude Vision returned an empty response");
        }
        JsonNode textNode = response.path("content").path(0).path("text");
        if (textNode.isMissingNode() || textNode.asText().isBlank()) {
            log.error("Unexpected Claude Vision response shape: {}", response);
            throw new BadRequestException("Claude Vision response did not contain the expected text payload");
        }
        return textNode.asText();
    }
}
