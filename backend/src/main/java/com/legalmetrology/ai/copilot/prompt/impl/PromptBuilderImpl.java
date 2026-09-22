package com.legalmetrology.ai.copilot.prompt.impl;

import com.legalmetrology.ai.copilot.context.CopilotContext;
import com.legalmetrology.ai.copilot.prompt.PromptBuilder;
import com.legalmetrology.ai.copilot.prompt.PromptPair;
import com.legalmetrology.ai.copilot.prompt.PromptTemplateLoader;
import com.legalmetrology.ai.copilot.prompt.PromptTemplateName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PromptBuilderImpl implements PromptBuilder {

    private final PromptTemplateLoader templateLoader;
    private final CopilotContextFormatter contextFormatter;

    @Override
    public PromptPair build(PromptTemplateName template, CopilotContext context, Map<String, String> extraVariables) {
        Map<String, String> variables = new HashMap<>(extraVariables);
        variables.put("context", contextFormatter.format(context));

        String systemPrompt = templateLoader.render(PromptTemplateName.SYSTEM_BASE.fileName(), Map.of());
        String userPrompt = templateLoader.render(template.fileName(), variables);
        return new PromptPair(systemPrompt, userPrompt);
    }

    @Override
    public PromptPair buildWithoutContext(PromptTemplateName template, Map<String, String> variables) {
        String systemPrompt = templateLoader.render(PromptTemplateName.SYSTEM_BASE.fileName(), Map.of());
        String userPrompt = templateLoader.render(template.fileName(), variables);
        return new PromptPair(systemPrompt, userPrompt);
    }
}
