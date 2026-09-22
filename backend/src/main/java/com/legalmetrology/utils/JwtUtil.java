package com.legalmetrology.utils;

import com.legalmetrology.common.constant.AppConstants;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Low-level, framework-agnostic JWT header helpers usable outside the
 * security filter chain (e.g. from a controller that needs the raw token
 * for logging/audit purposes). Token issuing/validation itself lives in
 * {@code security.JwtTokenProvider}, which is Spring-Security-aware.
 */
public final class JwtUtil {

    private JwtUtil() {
    }

    public static String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(AppConstants.AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(AppConstants.BEARER_PREFIX)) {
            return header.substring(AppConstants.BEARER_PREFIX.length());
        }
        return null;
    }

    public static String maskToken(String token) {
        if (token == null || token.length() < 12) {
            return "****";
        }
        return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
    }
}
