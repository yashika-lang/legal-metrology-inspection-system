package com.legalmetrology.ocr.config;

import com.google.auth.oauth2.GoogleCredentials;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Resolves an OAuth2 bearer token from a Google Cloud service account
 * (Application Default Credentials) when {@code GOOGLE_APPLICATION_CREDENTIALS}
 * is set, so {@link com.legalmetrology.ocr.provider.GoogleVisionOcrProvider}
 * can authenticate the "proper" enterprise way (a service account JSON key)
 * instead of a raw API key — see SETUP.md. Deliberately optional: if the
 * env var isn't set, {@link #getAccessToken()} returns empty and the
 * provider falls back to {@code GOOGLE_VISION_API_KEY}, so a developer who
 * only has an API key loses nothing.
 * <p>
 * Resolution happens once at startup (not per-request) and is never fatal —
 * a malformed or unreadable credentials file is logged and treated the same
 * as "not configured," not a boot failure, per the standing "never crash on
 * missing AI configuration" requirement.
 */
@Slf4j
@Component
public class GoogleCloudCredentialsProvider {

    private static final String CLOUD_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";

    private GoogleCredentials credentials;

    @PostConstruct
    void loadIfConfigured() {
        String credentialsPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.info("GOOGLE_APPLICATION_CREDENTIALS not set — Google Vision will use the API-key auth path if configured.");
            return;
        }
        try {
            credentials = GoogleCredentials.getApplicationDefault().createScoped(List.of(CLOUD_PLATFORM_SCOPE));
            log.info("Loaded Google Cloud service-account credentials from {}", credentialsPath);
        } catch (IOException ex) {
            log.warn("GOOGLE_APPLICATION_CREDENTIALS is set to '{}' but could not be loaded ({}) — "
                    + "falling back to the API-key auth path if configured.", credentialsPath, ex.getMessage());
            credentials = null;
        }
    }

    /** Empty when no service account is configured (or it failed to load) — callers must fall back to their own API-key path. */
    public Optional<String> getAccessToken() {
        if (credentials == null) {
            return Optional.empty();
        }
        try {
            credentials.refreshIfExpired();
            return Optional.ofNullable(credentials.getAccessToken()).map(token -> token.getTokenValue());
        } catch (IOException ex) {
            log.warn("Failed to refresh Google Cloud access token: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
