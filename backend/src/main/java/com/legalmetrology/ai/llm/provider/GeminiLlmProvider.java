package com.legalmetrology.ai.llm.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.legalmetrology.ai.config.GeminiProperties;
import com.legalmetrology.exception.BadRequestException;
import com.legalmetrology.exception.ServiceNotConfiguredException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/** {@link LlmProvider} backed by Gemini's text-only {@code generateContent} — the preferred LLM provider. */
@Slf4j
@Component
public class GeminiLlmProvider implements LlmProvider {

    private final WebClient geminiWebClient;
    private final GeminiProperties properties;

    public GeminiLlmProvider(@Qualifier("geminiWebClient") WebClient geminiWebClient, GeminiProperties properties) {
        this.geminiWebClient = geminiWebClient;
        this.properties = properties;
    }

    @Override
    public String providerKey() {
        return "gemini";
    }

    @Override
    public LlmCompletion complete(String systemPrompt, String userPrompt) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw ServiceNotConfiguredException.apiKeyMissing("Gemini");
        }
        long startedAtMs = System.currentTimeMillis();

        Map<String, Object> requestBody = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", List.of(Map.of("parts", List.of(Map.of("text", userPrompt))))
        );

        JsonNode response = geminiWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/models/{model}:generateContent")
                        .queryParam("key", properties.apiKey())
                        .build(properties.model()))
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null) {
            throw new BadRequestException("Gemini returned an empty response");
        }
        JsonNode textNode = response.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (textNode.isMissingNode()) {
            log.error("Unexpected Gemini response shape: {}", response);
            throw new BadRequestException("Gemini response did not contain the expected text payload");
        }

        int latencyMs = (int) (System.currentTimeMillis() - startedAtMs);
        Integer tokenCount = response.path("usageMetadata").has("totalTokenCount")
                ? response.path("usageMetadata").path("totalTokenCount").asInt() : null;

        return new LlmCompletion(textNode.asText(), properties.model(), tokenCount, latencyMs);
    }
}
