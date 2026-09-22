package com.legalmetrology.ai.copilot.prompt;

import java.util.Map;

/**
 * Loads named prompt templates from {@code classpath:prompts/copilot/*.txt}
 * and renders them with {@code {{variable}}} substitution — templates are
 * plain text files under version control, never string literals inside a
 * service class, so a prompt can be tuned without touching Java code.
 */
public interface PromptTemplateLoader {

    String render(String templateName, Map<String, String> variables);
}
