package com.legalmetrology.ai.copilot.dto;

import jakarta.validation.constraints.NotBlank;

public record OfficerNotesRequest(
        @NotBlank(message = "Rough notes are required")
        String roughNotes
) {
}
