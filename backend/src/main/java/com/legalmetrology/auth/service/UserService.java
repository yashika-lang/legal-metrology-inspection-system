package com.legalmetrology.auth.service;

import com.legalmetrology.auth.dto.AssignRolesRequest;
import com.legalmetrology.auth.dto.UpdateProfileRequest;
import com.legalmetrology.auth.dto.UpdateUserStatusRequest;
import com.legalmetrology.auth.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserResponse getById(UUID userId);

    Page<UserResponse> list(Pageable pageable);

    UserResponse updateProfile(UUID userId, UpdateProfileRequest request);

    UserResponse assignRoles(UUID userId, AssignRolesRequest request);

    UserResponse updateStatus(UUID userId, UpdateUserStatusRequest request);
}
