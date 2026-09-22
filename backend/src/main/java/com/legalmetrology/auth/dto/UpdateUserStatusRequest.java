package com.legalmetrology.auth.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull(message = "active flag is required")
        Boolean active
) {
}
