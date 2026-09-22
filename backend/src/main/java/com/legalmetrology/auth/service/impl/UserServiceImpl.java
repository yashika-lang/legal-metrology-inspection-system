package com.legalmetrology.auth.service.impl;

import com.legalmetrology.auth.dto.AssignRolesRequest;
import com.legalmetrology.auth.dto.UpdateProfileRequest;
import com.legalmetrology.auth.dto.UpdateUserStatusRequest;
import com.legalmetrology.auth.dto.UserResponse;
import com.legalmetrology.auth.entity.Role;
import com.legalmetrology.auth.entity.User;
import com.legalmetrology.auth.mapper.UserMapper;
import com.legalmetrology.auth.repository.RoleRepository;
import com.legalmetrology.auth.repository.UserRepository;
import com.legalmetrology.auth.service.UserService;
import com.legalmetrology.common.enums.RoleName;
import com.legalmetrology.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(UUID userId) {
        return userMapper.toResponse(findUserOrThrow(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> list(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUserOrThrow(userId);

        if (StringUtils.hasText(request.fullName())) {
            user.setFullName(request.fullName());
        }
        if (StringUtils.hasText(request.phone())) {
            user.setPhone(request.phone());
        }
        if (StringUtils.hasText(request.preferredLocale())) {
            user.setPreferredLocale(request.preferredLocale());
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse assignRoles(UUID userId, AssignRolesRequest request) {
        User user = findUserOrThrow(userId);

        Set<Role> resolvedRoles = request.roles().stream()
                .map(this::findRoleOrThrow)
                .collect(Collectors.toSet());

        user.setRoles(resolvedRoles);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse updateStatus(UUID userId, UpdateUserStatusRequest request) {
        User user = findUserOrThrow(userId);
        user.setActive(request.active());
        return userMapper.toResponse(userRepository.save(user));
    }

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
    }

    private Role findRoleOrThrow(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> ResourceNotFoundException.of("Role", roleName));
    }
}
