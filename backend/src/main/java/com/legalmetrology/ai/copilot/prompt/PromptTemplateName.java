package com.legalmetrology.ai.copilot.prompt;

/** The template files under {@code resources/prompts/copilot/} — the name is the file's base name. */
public enum PromptTemplateName {
    SYSTEM_BASE("system_base"),
    ASK("ask"),
    EXPLAIN_RULE("explain_rule"),
    SUMMARIZE_INSPECTION("summarize_inspection"),
    OFFICER_NOTES("officer_notes"),
    MANUFACTURER_RECOMMENDATIONS("manufacturer_recommendations"),
    COMPARE_INSPECTIONS("compare_inspections");

    private final String fileName;

    PromptTemplateName(String fileName) {
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }
}
