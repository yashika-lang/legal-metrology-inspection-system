package com.legalmetrology.common.settings.dto;

import java.time.Instant;
import java.util.UUID;

public record SettingsResponse(UUID id, String key, String value, Instant updatedAt) {
}
