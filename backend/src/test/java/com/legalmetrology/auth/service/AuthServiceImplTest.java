package com.legalmetrology.auth.service;

import com.legalmetrology.auth.dto.AuthResponse;
import com.legalmetrology.auth.dto.LoginRequest;
import com.legalmetrology.auth.dto.SignupRequest;
import com.legalmetrology.auth.dto.UserResponse;
import com.legalmetrology.auth.entity.Role;
import com.legalmetrology.auth.entity.User;
import com.legalmetrology.auth.mapper.UserMapper;
import com.legalmetrology.auth.repository.PasswordResetTokenRepository;
import com.legalmetrology.auth.repository.RefreshTokenRepository;
import com.legalmetrology.auth.repository.RoleRepository;
import com.legalmetrology.auth.repository.UserRepository;
import com.legalmetrology.auth.service.impl.AuthServiceImpl;
import com.legalmetrology.common.enums.RoleName;
import com.legalmetrology.config.JwtProperties;
import com.legalmetrology.config.PasswordResetProperties;
import com.legalmetrology.exception.DuplicateResourceException;
import com.legalmetrology.history.service.AuditLogService;
import com.legalmetrology.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AuditLogService auditLogService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties(
                "unit-test-secret-key-long-enough-for-hmac-sha-256", 900_000L, 604_800_000L, "test-issuer");
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(jwtProperties);
        PasswordResetProperties passwordResetProperties = new PasswordResetProperties(3_600_000L);

        authService = new AuthServiceImpl(
                userRepository, roleRepository, refreshTokenRepository, passwordResetTokenRepository,
                passwordEncoder, authenticationManager, jwtTokenProvider, jwtProperties,
                passwordResetProperties, userMapper, auditLogService);
    }

    @Test
    void signupRejectsAnAlreadyRegisteredEmail() {
        SignupRequest request = new SignupRequest("Asha Rao", "asha@lmis.gov.in", "Str0ng!Pass", "EMP001", "9999999999");
        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void signupPersistsAUserWithAnEncodedPasswordAndDefaultInspectorRole() {
        SignupRequest request = new SignupRequest("Asha Rao", "asha@lmis.gov.in", "Str0ng!Pass", "EMP001", "9999999999");
        Role inspectorRole = Role.builder().name(RoleName.INSPECTOR).build();

        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);
        when(userRepository.existsByEmployeeCode(request.employeeCode())).thenReturn(false);
        when(roleRepository.findByName(RoleName.INSPECTOR)).thenReturn(java.util.Optional.of(inspectorRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            // Mimic what Hibernate's @UuidGenerator does on insert — the mocked repository
            // never actually persists, so the entity would otherwise keep a null id.
            User savedUser = invocation.getArgument(0);
            savedUser.setId(UUID.randomUUID());
            return savedUser;
        });
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(
                new UserResponse(UUID.randomUUID(), "Asha Rao", "asha@lmis.gov.in", "EMP001",
                        "9999999999", "en", true, Set.of("INSPECTOR"), null));

        AuthResponse response = authService.signup(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo("asha@lmis.gov.in");
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("Str0ng!Pass");
        assertThat(passwordEncoder.matches("Str0ng!Pass", savedUser.getPasswordHash())).isTrue();
        assertThat(savedUser.getRoles()).containsExactly(inspectorRole);

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.user().email()).isEqualTo("asha@lmis.gov.in");
    }

    @Test
    void loginRejectsInvalidCredentialsWithAUnauthorizedException() {
        LoginRequest request = new LoginRequest("asha@lmis.gov.in", "wrong-password");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("bad creds"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(com.legalmetrology.exception.UnauthorizedException.class);
    }
}
