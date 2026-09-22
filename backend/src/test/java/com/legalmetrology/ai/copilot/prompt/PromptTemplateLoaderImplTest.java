package com.legalmetrology.ai.copilot.prompt;

import com.legalmetrology.ai.copilot.prompt.impl.PromptTemplateLoaderImpl;
import com.legalmetrology.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromptTemplateLoaderImplTest {

    private final PromptTemplateLoaderImpl loader = new PromptTemplateLoaderImpl();

    @Test
    void loadsAndSubstitutesTheExplainRuleTemplate() {
        String rendered = loader.render("explain_rule", Map.of(
                "ruleCode", "LM-MRP-001",
                "title", "MRP declaration",
                "description", "Must declare MRP",
                "category", "MANDATORY_DECLARATION",
                "severity", "CRITICAL",
                "mandatory", "true",
                "legalReference", "LMPC Rule 6",
                "penaltyReference", "Section 36"
        ));

        assertThat(rendered).contains("LM-MRP-001");
        assertThat(rendered).contains("MRP declaration");
        assertThat(rendered).contains("CRITICAL");
        assertThat(rendered).doesNotContain("{{");
    }

    @Test
    void loadsTheSystemBaseTemplateAndItContainsTheCoreGuardrail() {
        String rendered = loader.render("system_base", Map.of());

        assertThat(rendered).contains("NEVER decide");
        assertThat(rendered).contains("Rule Engine");
    }

    @Test
    void missingVariablesAreLeftAsLiteralPlaceholdersRatherThanCrashing() {
        String rendered = loader.render("explain_rule", Map.of("ruleCode", "LM-TEST-001"));

        assertThat(rendered).contains("LM-TEST-001");
        assertThat(rendered).contains("{{title}}"); // not substituted, but doesn't throw
    }

    @Test
    void anUnknownTemplateNameThrowsAClearError() {
        assertThatThrownBy(() -> loader.render("does_not_exist", Map.of()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does_not_exist");
    }

    @Test
    void everyDeclaredTemplateNameHasACorrespondingFile() {
        for (PromptTemplateName name : PromptTemplateName.values()) {
            assertThat(loader.render(name.fileName(), Map.of())).isNotBlank();
        }
    }
}
