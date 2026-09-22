package com.legalmetrology.auth.dto;

public record TokenRefreshResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInMs
) {
    public static TokenRefreshResponse of(String accessToken, String refreshToken, long expiresInMs) {
        return new TokenRefreshResponse(accessToken, refreshToken, "Bearer", expiresInMs);
    }
}
