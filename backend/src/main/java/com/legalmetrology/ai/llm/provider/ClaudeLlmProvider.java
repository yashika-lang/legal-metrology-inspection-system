package com.legalmetrology.ai.llm.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.legalmetrology.ai.config.ClaudeProperties;
import com.legalmetrology.exception.BadRequestException;
import com.legalmetrology.exception.ServiceNotConfiguredException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/** {@link LlmProvider} backed by Claude's text-only Messages API — the configurable alternate LLM provider. */
@Slf4j
@Component
public class ClaudeLlmProvider implements LlmProvider {

    private static final int MAX_TOKENS = 2048;

    private final WebClient claudeWebClient;
    private final ClaudeProperties properties;

    public ClaudeLlmProvider(@Qualifier("claudeWebClient") WebClient claudeWebClient, ClaudeProperties properties) {
        this.claudeWebClient = claudeWebClient;
        this.properties = properties;
    }

    @Override
    public String providerKey() {
        return "claude";
    }

    @Override
    public LlmCompletion complete(String systemPrompt, String userPrompt) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw ServiceNotConfiguredException.apiKeyMissing("Claude");
        }
        long startedAtMs = System.currentTimeMillis();

        Map<String, Object> requestBody = Map.of(
                "model", properties.model(),
                "max_tokens", MAX_TOKENS,
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", userPrompt))
        );

        JsonNode response = claudeWebClient.post()
                .uri("/messages")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null) {
            throw new BadRequestException("Claude returned an empty response");
        }
        JsonNode textNode = response.path("content").path(0).path("text");
        if (textNode.isMissingNode()) {
            log.error("Unexpected Claude response shape: {}", response);
            throw new BadRequestException("Claude response did not contain the expected text payload");
        }

        int latencyMs = (int) (System.currentTimeMillis() - startedAtMs);
        Integer tokenCount = response.path("usage").has("output_tokens")
                ? response.path("usage").path("output_tokens").asInt() : null;

        return new LlmCompletion(textNode.asText(), properties.model(), tokenCount, latencyMs);
    }
}
