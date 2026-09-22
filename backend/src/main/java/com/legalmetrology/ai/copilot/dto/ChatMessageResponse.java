package com.legalmetrology.ai.copilot.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        String role,
        String message,
        String provider,
        UUID reportId,
        Instant createdAt
) {
}
