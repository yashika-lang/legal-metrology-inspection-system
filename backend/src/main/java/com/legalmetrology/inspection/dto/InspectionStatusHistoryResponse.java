package com.legalmetrology.inspection.dto;

import com.legalmetrology.common.enums.InspectionStatus;

import java.time.Instant;
import java.util.UUID;

public record InspectionStatusHistoryResponse(
        UUID id,
        InspectionStatus fromStatus,
        InspectionStatus toStatus,
        String changedByName,
        String note,
        Instant createdAt
) {
}
