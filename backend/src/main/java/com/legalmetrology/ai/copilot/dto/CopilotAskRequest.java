package com.legalmetrology.ai.copilot.dto;

import jakarta.validation.constraints.NotBlank;

public record CopilotAskRequest(
        @NotBlank(message = "Question is required")
        String question
) {
}
