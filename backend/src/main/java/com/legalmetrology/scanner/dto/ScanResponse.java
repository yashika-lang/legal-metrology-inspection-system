package com.legalmetrology.scanner.dto;

import com.legalmetrology.common.enums.ScanType;

import java.time.Instant;
import java.util.UUID;

public record ScanResponse(
        UUID id,
        UUID inspectionId,
        ScanType scanType,
        String scanValue,
        Instant scannedAt,
        UUID matchedProductId,
        String matchedProductName
) {
}
