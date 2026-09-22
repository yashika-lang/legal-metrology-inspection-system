package com.legalmetrology.auth.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String email,
        String employeeCode,
        String phone,
        String preferredLocale,
        boolean active,
        Set<String> roles,
        Instant createdAt
) {
}
