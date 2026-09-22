package com.legalmetrology.history.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID userId,
        String userName,
        String action,
        String entityType,
        UUID entityId,
        String metadata,
        String ipAddress,
        Instant createdAt
) {
}
