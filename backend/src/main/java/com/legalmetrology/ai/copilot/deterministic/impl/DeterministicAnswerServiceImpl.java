package com.legalmetrology.ai.copilot.deterministic.impl;

import com.legalmetrology.ai.copilot.context.CopilotContext;
import com.legalmetrology.ai.copilot.deterministic.DeterministicAnswerService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Deliberately simple, explainable keyword matching rather than an LLM-based
 * intent classifier — consistent with this system's preference for
 * deterministic logic wherever a question has exactly one correct,
 * database-backed answer (see the analytics Insight Engine for the same
 * philosophy). Falls through to the generative path for anything not
 * matched here.
 */
@Service
public class DeterministicAnswerServiceImpl implements DeterministicAnswerService {

    private static final List<String> SEVERITY_ORDER = List.of("CRITICAL", "MAJOR", "MINOR");

    @Override
    public Optional<String> tryAnswer(CopilotContext context, String question) {
        String q = question.toLowerCase(Locale.ROOT);

        if (q.contains("compliance score")) {
            return Optional.of(answerComplianceScore(context));
        }
        if (q.contains("most critical") || q.contains("most severe") || q.contains("worst violation")) {
            return Optional.of(answerMostCriticalViolation(context));
        }
        if (q.contains("missing declaration") || q.contains("which declaration") || q.contains("what declaration")) {
            return Optional.of(answerMissingDeclarations(context));
        }
        if (q.contains("fraud risk") || q.contains("risk level") || (q.contains("risk") && q.contains("what is"))) {
            return Optional.of("Recorded fraud risk: " + context.fraudRisk() + ".");
        }
        if (q.contains("how many violation")) {
            return Optional.of("This inspection has " + context.violations().size() + " recorded violation(s).");
        }

        return Optional.empty();
    }

    private String answerComplianceScore(CopilotContext context) {
        if (context.complianceScore() == null) {
            return "No compliance score has been computed for this inspection yet.";
        }
        return "The compliance score recorded by the Rule Engine is " + context.complianceScore() + "/100.";
    }

    private String answerMostCriticalViolation(CopilotContext context) {
        if (context.violations().isEmpty()) {
            return "This inspection has no recorded violations.";
        }
        var mostCritical = context.violations().stream()
                .min(Comparator.comparingInt(v -> SEVERITY_ORDER.indexOf(v.severity())))
                .orElseThrow();
        return "The most critical violation is [" + mostCritical.severity() + "] " + mostCritical.ruleCode()
                + " (" + mostCritical.ruleTitle() + "): " + mostCritical.description();
    }

    private String answerMissingDeclarations(CopilotContext context) {
        List<String> missing = context.declarations().stream()
                .filter(declaration -> !declaration.present())
                .map(CopilotContext.DeclarationSummary::type)
                .collect(Collectors.toList());

        if (missing.isEmpty()) {
            return "No missing declarations are recorded for this inspection.";
        }
        return "Missing declarations: " + String.join(", ", missing) + ".";
    }
}
