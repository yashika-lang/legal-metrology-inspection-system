package com.legalmetrology.ai.copilot.service;

import com.legalmetrology.ai.copilot.dto.CopilotAnswerResponse;

import java.util.UUID;

/**
 * Part 1 — the AI Compliance Copilot. Every method here only explains,
 * summarizes, recommends, or generates language; none of them write to an
 * Inspection's compliance score, a Violation, or any other compliance
 * decision — those remain exclusively the Rule Engine's output. See
 * {@code ai.copilot.context.CopilotContext}'s Javadoc for how that
 * guarantee is enforced architecturally, not just by prompt instruction.
 */
public interface CopilotService {

    CopilotAnswerResponse ask(UUID inspectionId, String question, UUID userId);

    CopilotAnswerResponse explainRule(String ruleCode, UUID userId);

    CopilotAnswerResponse summarizeInspection(UUID inspectionId, UUID userId);

    CopilotAnswerResponse generateOfficerNotes(UUID inspectionId, String roughNotes, UUID userId);

    CopilotAnswerResponse generateManufacturerRecommendations(UUID inspectionId, UUID userId);

    CopilotAnswerResponse compareInspections(UUID inspectionIdA, UUID inspectionIdB, UUID userId);
}
