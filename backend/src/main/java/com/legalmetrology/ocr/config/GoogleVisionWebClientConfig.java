package com.legalmetrology.ocr.config;

import com.legalmetrology.config.TimeoutHttpConnector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Configuration
public class GoogleVisionWebClientConfig {

    private static final String GOOGLE_VISION_BASE_URL = "https://vision.googleapis.com/v1";
    private static final int MAX_IN_MEMORY_SIZE_BYTES = 25 * 1024 * 1024;
    private static final Duration PROVIDER_TIMEOUT = Duration.ofSeconds(30);

    @Bean("googleVisionWebClient")
    public WebClient googleVisionWebClient() {
        return WebClient.builder()
                .baseUrl(GOOGLE_VISION_BASE_URL)
                .clientConnector(TimeoutHttpConnector.of(PROVIDER_TIMEOUT))
                .codecs(config -> config.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE_BYTES))
                .build();
    }
}
