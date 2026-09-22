package com.legalmetrology.auth.dto;

import com.legalmetrology.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Full name must not exceed 150 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 255)
        String email,

        @NotBlank(message = "Password is required")
        @ValidPassword
        String password,

        @Size(max = 50, message = "Employee code must not exceed 50 characters")
        String employeeCode,

        @Size(max = 20, message = "Phone number must not exceed 20 characters")
        String phone
) {
    /** See {@link LoginRequest}'s compact constructor — same reasoning: trim only email, never password. */
    public SignupRequest {
        if (email != null) {
            email = email.trim();
        }
        if (fullName != null) {
            fullName = fullName.trim();
        }
    }
}
