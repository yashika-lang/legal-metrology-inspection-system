package com.legalmetrology.inspection.dto;

import com.legalmetrology.common.enums.InspectionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InspectionStatusUpdateRequest(
        @NotNull(message = "Target status is required")
        InspectionStatus status,

        @Size(max = 500)
        String note
) {
}
