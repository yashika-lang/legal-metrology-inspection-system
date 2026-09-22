package com.legalmetrology.storage.config;

import com.legalmetrology.config.TimeoutHttpConnector;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class SupabaseWebClientConfig {

    /**
     * Deliberately more generous than the AI-provider timeout (30s). A
     * storage upload carries a multi-MB multipart body — this backend runs
     * on a home connection with limited upload bandwidth, and several
     * inspection photos uploaded in close succession genuinely compete for
     * that bandwidth. Found live: a real Guided Capture upload read-timed
     * out at exactly 30s with "request never reached Supabase" while the
     * body was still being sent, even though the upload itself was
     * otherwise healthy and would have completed with more headroom.
     */
    private static final Duration STORAGE_TIMEOUT = Duration.ofSeconds(75);

    private final SupabaseStorageProperties supabaseStorageProperties;

    @Bean
    public WebClient supabaseStorageWebClient() {
        return WebClient.builder()
                .baseUrl(supabaseStorageProperties.url() + "/storage/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseStorageProperties.serviceRoleKey())
                .defaultHeader("apikey", supabaseStorageProperties.serviceRoleKey())
                .clientConnector(TimeoutHttpConnector.of(STORAGE_TIMEOUT))
                .codecs(config -> config.defaultCodecs().maxInMemorySize(20 * 1024 * 1024)) // 20MB, matches upload limit
                .build();
    }
}
