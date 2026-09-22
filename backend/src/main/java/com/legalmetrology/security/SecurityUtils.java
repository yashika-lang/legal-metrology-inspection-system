package com.legalmetrology.security;

import com.legalmetrology.common.constant.AppConstants;
import com.legalmetrology.common.enums.RoleName;
import com.legalmetrology.exception.UnauthorizedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

/** Convenience accessors for "who is making this request", usable from any service. */
@Component
public class SecurityUtils {

    public UUID getCurrentUserId() {
        return getCurrentPrincipal().getId();
    }

    public String getCurrentUserEmail() {
        return getCurrentPrincipal().getEmail();
    }

    /** For service-layer "owner OR one of these roles" checks — e.g. deleting an image the current user didn't upload. */
    public boolean currentUserHasAnyRole(RoleName... roles) {
        var authorities = getCurrentPrincipal().getAuthorities();
        return Arrays.stream(roles)
                .anyMatch(role -> authorities.stream()
                        .anyMatch(authority -> authority.getAuthority().equals(AppConstants.ROLE_PREFIX + role.name())));
    }

    public UserPrincipal getCurrentPrincipal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("No authenticated user in the current request context");
        }
        return principal;
    }
}
