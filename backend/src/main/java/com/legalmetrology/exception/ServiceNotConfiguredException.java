package com.legalmetrology.exception;

/**
 * Thrown at the point an external service (an AI provider, Supabase
 * Storage) is actually used, when its required credentials are missing or
 * blank — never at application startup. A missing OCR key must not take
 * down a backend where auth, rules, and every non-AI endpoint work fine;
 * this fails only the one request that needed the missing credential, with
 * a message an operator can act on immediately.
 */
public class ServiceNotConfiguredException extends RuntimeException {

    public ServiceNotConfiguredException(String message) {
        super(message);
    }

    public static ServiceNotConfiguredException apiKeyMissing(String serviceName) {
        return new ServiceNotConfiguredException(serviceName + " API key is not configured.");
    }
}
