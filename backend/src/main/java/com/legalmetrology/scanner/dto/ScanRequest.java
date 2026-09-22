package com.legalmetrology.scanner.dto;

import com.legalmetrology.common.enums.ScanType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ScanRequest(
        @NotNull(message = "Inspection id is required")
        UUID inspectionId,

        @NotNull(message = "Scan type is required")
        ScanType scanType,

        @NotBlank(message = "Scan value is required")
        String scanValue
) {
}
