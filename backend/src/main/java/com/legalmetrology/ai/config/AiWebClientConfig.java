package com.legalmetrology.ai.config;

import com.legalmetrology.config.TimeoutHttpConnector;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * One {@link WebClient} per external AI provider, each pre-configured with
 * that provider's base URL/auth headers so the individual provider adapters
 * (Gemini/Claude/Google Vision) only deal with request/response bodies.
 */
@Configuration
@RequiredArgsConstructor
public class AiWebClientConfig {

    private static final int MAX_IN_MEMORY_SIZE_BYTES = 25 * 1024 * 1024; // 25MB — comfortably above one label photo + JSON response
    private static final Duration PROVIDER_TIMEOUT = Duration.ofSeconds(30);

    private final GeminiProperties geminiProperties;
    private final ClaudeProperties claudeProperties;

    @Bean("geminiWebClient")
    public WebClient geminiWebClient() {
        return WebClient.builder()
                .baseUrl(geminiProperties.baseUrl())
                .clientConnector(TimeoutHttpConnector.of(PROVIDER_TIMEOUT))
                .codecs(config -> config.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE_BYTES))
                .build();
    }

    @Bean("claudeWebClient")
    public WebClient claudeWebClient() {
        return WebClient.builder()
                .baseUrl(claudeProperties.baseUrl())
                .defaultHeader("x-api-key", claudeProperties.apiKey())
                .defaultHeader("anthropic-version", "2023-06-01")
                .clientConnector(TimeoutHttpConnector.of(PROVIDER_TIMEOUT))
                .codecs(config -> config.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE_BYTES))
                .build();
    }
}
