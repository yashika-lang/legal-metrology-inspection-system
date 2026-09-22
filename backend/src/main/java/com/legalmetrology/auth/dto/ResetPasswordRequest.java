package com.legalmetrology.auth.dto;

import com.legalmetrology.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(

        @NotBlank(message = "Reset token is required")
        String token,

        @NotBlank(message = "New password is required")
        @ValidPassword
        String newPassword
) {
}
