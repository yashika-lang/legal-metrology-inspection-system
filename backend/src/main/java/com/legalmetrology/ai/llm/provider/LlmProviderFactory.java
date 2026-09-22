package com.legalmetrology.ai.llm.provider;

import com.legalmetrology.ai.config.LlmProperties;
import com.legalmetrology.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resolves the {@link LlmProvider} selected by {@code ai.llm.provider} (Factory over the provider Strategy interface). */
@Component
@RequiredArgsConstructor
public class LlmProviderFactory {

    private final List<LlmProvider> providers;
    private final LlmProperties properties;

    private Map<String, LlmProvider> providersByKey;

    public LlmProvider resolve() {
        LlmProvider provider = byKey().get(properties.provider());
        if (provider == null) {
            throw new BadRequestException("No LLM provider registered for ai.llm.provider=" + properties.provider());
        }
        return provider;
    }

    private Map<String, LlmProvider> byKey() {
        if (providersByKey == null) {
            providersByKey = providers.stream().collect(Collectors.toMap(LlmProvider::providerKey, Function.identity()));
        }
        return providersByKey;
    }
}
