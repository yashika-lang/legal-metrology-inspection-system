package com.legalmetrology.ocr.provider;

import com.legalmetrology.exception.BadRequestException;
import com.legalmetrology.exception.OcrUnavailableException;
import com.legalmetrology.ocr.config.OcrProperties;
import com.legalmetrology.ocr.model.OcrExtractionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves the configured primary {@link OcrProvider} and runs OCR through
 * it, automatically falling back to any other registered provider if the
 * primary throws. This is the only place in the codebase that knows there
 * even *is* more than one OCR provider — {@code ocr.service.OcrService}
 * (and everything above it) just calls {@link #extractWithFallback} and
 * gets a result back, with no branching on which backend actually ran.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OcrProviderFactory {

    private final List<OcrProvider> providers;
    private final OcrProperties properties;

    private Map<String, OcrProvider> providersByKey;

    public OcrProvider resolvePrimary() {
        OcrProvider provider = byKey().get(properties.provider());
        if (provider == null) {
            throw new BadRequestException("No OCR provider registered for ai.ocr.provider=" + properties.provider());
        }
        return provider;
    }

    /**
     * Runs OCR through the configured primary provider; if it throws (API
     * outage, quota, missing native Tesseract install, etc.), tries every
     * other registered provider in turn before giving up. The caller only
     * ever sees the successful {@link OcrExtractionResult} — which provider
     * actually served it is recorded on the result itself ({@code providerKey}).
     */
    public OcrExtractionResult extractWithFallback(byte[] imageBytes, String mimeType) {
        OcrProvider primary = resolvePrimary();
        try {
            return primary.extractText(imageBytes, mimeType);
        } catch (Throwable primaryFailure) {
            log.warn("Primary OCR provider '{}' failed, falling back: {}", primary.providerKey(), primaryFailure.getMessage());
            return tryFallbacks(primary, imageBytes, mimeType, primaryFailure);
        }
    }

    /**
     * Catches {@link Throwable}, not just {@link Exception}, in both this
     * method and its caller — deliberately. Tesseract is a JNA-wrapped
     * native library: a missing/unloadable {@code libtesseract} on the
     * host surfaces as {@link UnsatisfiedLinkError}, an {@link Error}, not
     * an {@link Exception}. One optional fallback provider having a
     * native-library or environment problem must still degrade to a clean
     * "all OCR providers failed" response — it must never crash the
     * request past this factory, which is the one place responsible for
     * OCR provider failure being recoverable at all.
     */
    private OcrExtractionResult tryFallbacks(OcrProvider primary, byte[] imageBytes, String mimeType, Throwable primaryFailure) {
        for (OcrProvider provider : providers) {
            if (provider == primary) {
                continue;
            }
            try {
                OcrExtractionResult result = provider.extractText(imageBytes, mimeType);
                log.info("OCR fallback provider '{}' succeeded after '{}' failed", provider.providerKey(), primary.providerKey());
                return result;
            } catch (Throwable fallbackFailure) {
                log.warn("Fallback OCR provider '{}' also failed: {}", provider.providerKey(), fallbackFailure.getMessage());
            }
        }
        throw new OcrUnavailableException("All OCR providers failed. Primary error: " + primaryFailure.getMessage());
    }

    private Map<String, OcrProvider> byKey() {
        if (providersByKey == null) {
            Map<String, OcrProvider> map = new LinkedHashMap<>();
            providers.forEach(provider -> map.put(provider.providerKey(), provider));
            providersByKey = map;
        }
        return providersByKey;
    }
}
