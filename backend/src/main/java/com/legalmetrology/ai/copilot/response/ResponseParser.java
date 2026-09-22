package com.legalmetrology.ai.copilot.response;

/**
 * Cleans up raw LLM output before it's shown to an officer or persisted —
 * strips stray markdown fences some models add despite instructions not
 * to, and trims whitespace. Kept as its own component (not inlined in the
 * service) so the cleanup rules are unit-testable in isolation and shared
 * identically across every Copilot capability.
 */
public interface ResponseParser {

    String parse(String rawLlmResponse);
}
