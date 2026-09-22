package com.legalmetrology.auth.dto;

import com.legalmetrology.common.enums.RoleName;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record AssignRolesRequest(
        @NotEmpty(message = "At least one role must be provided")
        Set<RoleName> roles
) {
}
