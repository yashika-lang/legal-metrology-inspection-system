package com.legalmetrology.ai.copilot.prompt;

/** The (system, user) prompt pair every {@code LlmProvider.complete(...)} call takes. */
public record PromptPair(String systemPrompt, String userPrompt) {
}
