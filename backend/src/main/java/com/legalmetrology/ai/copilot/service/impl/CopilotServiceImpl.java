package com.legalmetrology.ai.copilot.service.impl;

import com.legalmetrology.ai.copilot.chat.ConversationMemoryService;
import com.legalmetrology.ai.copilot.context.CopilotContext;
import com.legalmetrology.ai.copilot.context.CopilotContextAssembler;
import com.legalmetrology.ai.copilot.deterministic.DeterministicAnswerService;
import com.legalmetrology.ai.copilot.dto.CopilotAnswerResponse;
import com.legalmetrology.ai.copilot.prompt.PromptBuilder;
import com.legalmetrology.ai.copilot.prompt.PromptPair;
import com.legalmetrology.ai.copilot.prompt.PromptTemplateName;
import com.legalmetrology.ai.copilot.response.ResponseParser;
import com.legalmetrology.ai.copilot.service.CopilotService;
import com.legalmetrology.ai.entity.AIResponse;
import com.legalmetrology.ai.llm.provider.LlmCompletion;
import com.legalmetrology.ai.llm.provider.LlmProviderFactory;
import com.legalmetrology.ai.repository.AIResponseRepository;
import com.legalmetrology.common.enums.AiRefType;
import com.legalmetrology.exception.ResourceNotFoundException;
import com.legalmetrology.inspection.entity.Rule;
import com.legalmetrology.inspection.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Every method runs in its own new transaction ({@code REQUIRES_NEW}), not
 * whatever transaction the caller happens to be in. This module is called
 * from {@code report}'s report-generation transaction (for the executive
 * summary / recommendations narrative) in addition to its own controller —
 * under the default {@code REQUIRED} propagation, an LLM failure here would
 * mark the *caller's* transaction rollback-only even after the caller
 * catches the exception and falls back gracefully, surfacing as a
 * confusing {@code UnexpectedRollbackException} on commit instead. A
 * Copilot call's own chat-history/audit-row writes are logically
 * independent of whatever business transaction triggered it, so isolating
 * them is correct even outside this failure mode.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CopilotServiceImpl implements CopilotService {

    private static final int MAX_HISTORY_TURNS = 10;

    private final CopilotContextAssembler contextAssembler;
    private final DeterministicAnswerService deterministicAnswerService;
    private final PromptBuilder promptBuilder;
    private final ResponseParser responseParser;
    private final ConversationMemoryService conversationMemoryService;
    private final LlmProviderFactory llmProviderFactory;
    private final AIResponseRepository aiResponseRepository;
    private final RuleRepository ruleRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CopilotAnswerResponse ask(UUID inspectionId, String question, UUID userId) {
        CopilotContext context = contextAssembler.assemble(inspectionId);
        conversationMemoryService.appendUserMessage(inspectionId, userId, question);

        var deterministicAnswer = deterministicAnswerService.tryAnswer(context, question);
        if (deterministicAnswer.isPresent()) {
            String answer = deterministicAnswer.get();
            conversationMemoryService.appendAssistantMessage(inspectionId, userId, answer, null, null);
            return CopilotAnswerResponse.deterministic(answer);
        }

        String history = conversationMemoryService.getHistoryAsText(inspectionId, MAX_HISTORY_TURNS);
        PromptPair prompt = promptBuilder.build(PromptTemplateName.ASK, context, Map.of("question", question, "history", history));

        return runGenerative(prompt, AiRefType.INSPECTION, inspectionId, inspectionId, userId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CopilotAnswerResponse explainRule(String ruleCode, UUID userId) {
        Rule rule = ruleRepository.findByRuleCodeAndActiveTrue(ruleCode)
                .orElseThrow(() -> ResourceNotFoundException.of("Rule", ruleCode));

        PromptPair prompt = promptBuilder.buildWithoutContext(PromptTemplateName.EXPLAIN_RULE, Map.of(
                "ruleCode", rule.getRuleCode(),
                "title", rule.getTitle(),
                "description", nullToEmpty(rule.getDescription()),
                "category", nullToEmpty(rule.getCategory()),
                "severity", rule.getSeverity().name(),
                "mandatory", String.valueOf(rule.isMandatory()),
                "legalReference", nullToEmpty(rule.getLegalReference()),
                "penaltyReference", nullToEmpty(rule.getPenaltyReference())
        ));

        return runGenerative(prompt, AiRefType.VIOLATION, rule.getId(), null, userId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CopilotAnswerResponse summarizeInspection(UUID inspectionId, UUID userId) {
        CopilotContext context = contextAssembler.assemble(inspectionId);
        PromptPair prompt = promptBuilder.build(PromptTemplateName.SUMMARIZE_INSPECTION, context, Map.of());
        return runGenerative(prompt, AiRefType.INSPECTION, inspectionId, inspectionId, userId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CopilotAnswerResponse generateOfficerNotes(UUID inspectionId, String roughNotes, UUID userId) {
        CopilotContext context = contextAssembler.assemble(inspectionId);
        PromptPair prompt = promptBuilder.build(PromptTemplateName.OFFICER_NOTES, context, Map.of("roughNotes", roughNotes));
        return runGenerative(prompt, AiRefType.INSPECTION, inspectionId, inspectionId, userId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CopilotAnswerResponse generateManufacturerRecommendations(UUID inspectionId, UUID userId) {
        CopilotContext context = contextAssembler.assemble(inspectionId);
        PromptPair prompt = promptBuilder.build(PromptTemplateName.MANUFACTURER_RECOMMENDATIONS, context, Map.of());
        return runGenerative(prompt, AiRefType.INSPECTION, inspectionId, inspectionId, userId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CopilotAnswerResponse compareInspections(UUID inspectionIdA, UUID inspectionIdB, UUID userId) {
        CopilotContext contextA = contextAssembler.assemble(inspectionIdA);
        CopilotContext contextB = contextAssembler.assemble(inspectionIdB);

        // buildWithoutContext because this capability needs two context blocks, not the single-context template shape.
        PromptPair basePrompt = promptBuilder.buildWithoutContext(PromptTemplateName.COMPARE_INSPECTIONS, Map.of(
                "contextA", formatContextForComparison(contextA),
                "contextB", formatContextForComparison(contextB)
        ));

        return runGenerative(basePrompt, AiRefType.INSPECTION, inspectionIdA, inspectionIdA, userId);
    }

    private CopilotAnswerResponse runGenerative(PromptPair prompt, AiRefType refType, UUID refId, UUID inspectionIdForChatHistory, UUID userId) {
        long startedAtMs = System.currentTimeMillis();
        LlmCompletion completion = llmProviderFactory.resolve().complete(prompt.systemPrompt(), prompt.userPrompt());
        String answer = responseParser.parse(completion.text());

        auditLlmCall(refType, refId, prompt, completion, startedAtMs);

        if (inspectionIdForChatHistory != null) {
            conversationMemoryService.appendAssistantMessage(inspectionIdForChatHistory, userId, answer, completion.model(), null);
        }

        log.info("Copilot generative answer produced: refType={} refId={} model={} latencyMs={}", refType, refId, completion.model(), completion.latencyMs());
        return CopilotAnswerResponse.generative(answer, completion.model());
    }

    private void auditLlmCall(AiRefType refType, UUID refId, PromptPair prompt, LlmCompletion completion, long startedAtMs) {
        AIResponse auditRow = AIResponse.builder()
                .refType(refType)
                .refId(refId)
                .prompt(prompt.systemPrompt() + "\n\n---\n\n" + prompt.userPrompt())
                .response(completion.text())
                .model(completion.model())
                .tokenCount(completion.tokenCount())
                .latencyMs((int) (System.currentTimeMillis() - startedAtMs))
                .build();
        aiResponseRepository.save(auditRow);
    }

    private String formatContextForComparison(CopilotContext context) {
        return "Product: " + context.productName() + " | Compliance score: " + context.complianceScore()
                + " | Fraud risk: " + context.fraudRisk() + " | Violations: " + context.violations().size()
                + " (" + context.violations().stream().map(v -> v.severity() + ":" + v.ruleCode()).toList() + ")";
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "(not specified)";
    }
}
