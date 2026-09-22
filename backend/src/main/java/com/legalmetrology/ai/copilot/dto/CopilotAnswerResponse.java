package com.legalmetrology.ai.copilot.dto;

import java.time.Instant;

public record CopilotAnswerResponse(
        String answer,
        boolean generatedByAi,
        String provider,
        Instant answeredAt
) {
    public static CopilotAnswerResponse deterministic(String answer) {
        return new CopilotAnswerResponse(answer, false, null, Instant.now());
    }

    public static CopilotAnswerResponse generative(String answer, String provider) {
        return new CopilotAnswerResponse(answer, true, provider, Instant.now());
    }
}
