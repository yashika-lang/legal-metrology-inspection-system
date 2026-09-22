package com.legalmetrology.auth.mapper;

import com.legalmetrology.auth.dto.UserResponse;
import com.legalmetrology.auth.entity.Role;
import com.legalmetrology.auth.entity.User;
import org.mapstruct.Mapper;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    default UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getEmployeeCode(),
                user.getPhone(),
                user.getPreferredLocale(),
                user.isActive(),
                toRoleNames(user.getRoles()),
                user.getCreatedAt()
        );
    }

    default Set<String> toRoleNames(Set<Role> roles) {
        if (roles == null) {
            return Set.of();
        }
        return roles.stream().map(role -> role.getName().name()).collect(Collectors.toSet());
    }
}
