package com.legalmetrology.ai.llm.provider;

public record LlmCompletion(String text, String model, Integer tokenCount, int latencyMs) {
}
