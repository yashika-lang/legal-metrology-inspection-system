package com.legalmetrology.auth.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

        @Size(max = 150, message = "Full name must not exceed 150 characters")
        String fullName,

        @Size(max = 20, message = "Phone number must not exceed 20 characters")
        String phone,

        @Size(max = 10, message = "Locale code must not exceed 10 characters")
        String preferredLocale
) {
}
