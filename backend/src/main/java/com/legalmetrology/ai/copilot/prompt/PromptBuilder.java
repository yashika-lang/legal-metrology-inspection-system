package com.legalmetrology.ai.copilot.prompt;

import com.legalmetrology.ai.copilot.context.CopilotContext;

import java.util.Map;

/**
 * Combines a named template with a {@link CopilotContext} (rendered as a
 * structured text block) and any capability-specific variables (the user's
 * question, rough notes to polish, etc.) into the final system/user prompt
 * pair. This is the only place a {@code CopilotContext} is turned into text
 * an LLM reads.
 */
public interface PromptBuilder {

    PromptPair build(PromptTemplateName template, CopilotContext context, Map<String, String> extraVariables);

    /** For capabilities that don't need inspection context (e.g. explaining a rule in isolation). */
    PromptPair buildWithoutContext(PromptTemplateName template, Map<String, String> variables);
}
