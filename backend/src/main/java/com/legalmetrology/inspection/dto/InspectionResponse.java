package com.legalmetrology.inspection.dto;

import com.legalmetrology.common.enums.FraudRisk;
import com.legalmetrology.common.enums.InspectionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InspectionResponse(
        UUID id,
        UUID inspectorId,
        String inspectorName,
        UUID productId,
        String productName,
        InspectionStatus status,
        Double locationLat,
        Double locationLng,
        String region,
        BigDecimal complianceScore,
        FraudRisk fraudRisk,
        Instant startedAt,
        Instant completedAt
) {
}
