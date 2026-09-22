package com.legalmetrology.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
    /**
     * Trims the email before {@code @Email} validation ever sees it — a
     * mobile browser's autofill/keyboard commonly appends a trailing space,
     * which would otherwise fail validation with a confusing "not a valid
     * email address" even though the credentials themselves are correct.
     * Password is deliberately left untouched: silently altering it could
     * mask a genuine typo or a password that legitimately contains
     * meaningful leading/trailing characters.
     */
    public LoginRequest {
        if (email != null) {
            email = email.trim();
        }
    }
}
