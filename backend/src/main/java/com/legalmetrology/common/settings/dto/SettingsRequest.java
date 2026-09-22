package com.legalmetrology.common.settings.dto;

import jakarta.validation.constraints.NotBlank;

public record SettingsRequest(
        @NotBlank(message = "Setting key is required")
        String key,

        @NotBlank(message = "Setting value is required")
        String value
) {
}
