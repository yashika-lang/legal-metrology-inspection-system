package com.legalmetrology.vision.provider;

import com.legalmetrology.exception.BadRequestException;
import com.legalmetrology.vision.config.VisionProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves the {@link VisionAiProvider} bean selected by {@code ai.vision.provider}
 * (Factory pattern over the provider Strategy interface). Every provider
 * self-registers via Spring component scanning and {@link #providerKey()} —
 * adding a third provider later means implementing the interface, nothing
 * here changes.
 */
@Component
@RequiredArgsConstructor
public class VisionAiProviderFactory {

    private final List<VisionAiProvider> providers;
    private final VisionProperties visionProperties;

    private Map<String, VisionAiProvider> providersByKey;

    public VisionAiProvider resolve() {
        VisionAiProvider provider = byKey().get(visionProperties.provider());
        if (provider == null) {
            throw new BadRequestException("No Vision AI provider registered for ai.vision.provider=" + visionProperties.provider());
        }
        return provider;
    }

    private Map<String, VisionAiProvider> byKey() {
        if (providersByKey == null) {
            providersByKey = providers.stream()
                    .collect(Collectors.toMap(VisionAiProvider::providerKey, Function.identity()));
        }
        return providersByKey;
    }
}
