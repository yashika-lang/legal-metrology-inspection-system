package com.legalmetrology.ai.copilot.deterministic;

import com.legalmetrology.ai.copilot.context.CopilotContext;

import java.util.Optional;

/**
 * Answers purely factual questions directly from {@link CopilotContext} —
 * no LLM call at all. "What is the compliance score?" and "which violation
 * is most critical?" have exactly one correct answer already sitting in
 * the database; routing them through an LLM would add hallucination risk
 * and cost for zero benefit. This is what keeps the Rule Engine the single
 * source of truth in practice, not just in a system prompt instruction.
 */
public interface DeterministicAnswerService {

    Optional<String> tryAnswer(CopilotContext context, String question);
}
