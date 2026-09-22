package com.legalmetrology.ai.copilot.prompt.impl;

import com.legalmetrology.ai.copilot.context.CopilotContext;
import org.springframework.stereotype.Component;

/** Renders a {@link CopilotContext} as a plain-text block for injection into a prompt — the only place that happens. */
@Component
public class CopilotContextFormatter {

    public String format(CopilotContext context) {
        StringBuilder text = new StringBuilder();
        text.append("Product: ").append(context.productName())
                .append(" | Manufacturer: ").append(context.manufacturerName())
                .append(" | Category: ").append(context.categoryName()).append('\n');
        text.append("Inspection status: ").append(context.inspectionStatus())
                .append(" | Compliance score: ").append(context.complianceScore() != null ? context.complianceScore() + "/100" : "not yet computed")
                .append(" | Fraud risk: ").append(context.fraudRisk()).append('\n');

        text.append("\nDeclarations:\n");
        if (context.declarations().isEmpty()) {
            text.append("(none recorded yet)\n");
        }
        for (var declaration : context.declarations()) {
            text.append("- ").append(declaration.type())
                    .append(": ").append(declaration.present() ? "present" : "MISSING")
                    .append(declaration.value() != null ? " (\"" + declaration.value() + "\")" : "")
                    .append(declaration.confidence() != null ? ", confidence=" + declaration.confidence() : "")
                    .append(declaration.readabilityScore() != null ? ", readability=" + declaration.readabilityScore() + "/100" : "")
                    .append(declaration.labelSection() != null ? ", section=" + declaration.labelSection() : "")
                    .append('\n');
        }

        text.append("\nViolations (already determined by the Rule Engine — do not re-derive or contradict these):\n");
        if (context.violations().isEmpty()) {
            text.append("(none — this inspection has no violations)\n");
        }
        for (var violation : context.violations()) {
            text.append("- [").append(violation.severity()).append("] ").append(violation.ruleCode())
                    .append(" (").append(violation.ruleTitle()).append("): ").append(violation.description())
                    .append(violation.actualValue() != null ? " | actual=" + violation.actualValue() : "")
                    .append(violation.expectedValue() != null ? " | expected=" + violation.expectedValue() : "")
                    .append(violation.legalReference() != null ? " | legal reference: " + violation.legalReference() : "")
                    .append(violation.suggestedFix() != null ? " | suggested fix: " + violation.suggestedFix() : "")
                    .append('\n');
        }

        text.append("\nImage quality:\n");
        if (context.images().isEmpty()) {
            text.append("(no images uploaded yet)\n");
        }
        for (var image : context.images()) {
            text.append("- ").append(image.imageType()).append(": score=")
                    .append(image.qualityScore() != null ? image.qualityScore() + "/100" : "n/a")
                    .append(image.qualityWarnings() != null && !image.qualityWarnings().isBlank() ? ", warnings=" + image.qualityWarnings() : "")
                    .append(", recommended action=").append(image.recommendedAction()).append('\n');
        }

        return text.toString();
    }
}
