package com.legalmetrology.auth.service.impl;

import com.legalmetrology.auth.dto.AuthResponse;
import com.legalmetrology.auth.dto.LoginRequest;
import com.legalmetrology.auth.dto.SignupRequest;
import com.legalmetrology.auth.dto.TokenRefreshResponse;
import com.legalmetrology.auth.entity.PasswordResetToken;
import com.legalmetrology.auth.entity.RefreshToken;
import com.legalmetrology.auth.entity.Role;
import com.legalmetrology.auth.entity.User;
import com.legalmetrology.auth.mapper.UserMapper;
import com.legalmetrology.auth.repository.PasswordResetTokenRepository;
import com.legalmetrology.auth.repository.RefreshTokenRepository;
import com.legalmetrology.auth.repository.RoleRepository;
import com.legalmetrology.auth.repository.UserRepository;
import com.legalmetrology.auth.service.AuthService;
import com.legalmetrology.common.enums.RoleName;
import com.legalmetrology.config.JwtProperties;
import com.legalmetrology.config.PasswordResetProperties;
import com.legalmetrology.exception.BadRequestException;
import com.legalmetrology.exception.DuplicateResourceException;
import com.legalmetrology.exception.UnauthorizedException;
import com.legalmetrology.history.service.AuditLogService;
import com.legalmetrology.security.JwtTokenProvider;
import com.legalmetrology.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final PasswordResetProperties passwordResetProperties;
    private final UserMapper userMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        if (request.employeeCode() != null && !request.employeeCode().isBlank()
                && userRepository.existsByEmployeeCode(request.employeeCode())) {
            throw new DuplicateResourceException("An account with this employee code already exists");
        }

        Role inspectorRole = roleRepository.findByName(RoleName.INSPECTOR)
                .orElseThrow(() -> new IllegalStateException(
                        "Default role INSPECTOR is not seeded — check the Flyway seed migration"));

        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .employeeCode(request.employeeCode())
                .phone(request.phone())
                .roles(Set.of(inspectorRole))
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());
        auditLogService.record(user.getId(), "SIGNUP", "User", user.getId(), null, null);

        return issueTokens(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password()));
        } catch (org.springframework.security.authentication.BadCredentialsException
                 | org.springframework.security.authentication.DisabledException ex) {
            throw new UnauthorizedException("Invalid email or password");
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        log.info("User logged in: {}", user.getEmail());
        auditLogService.record(user.getId(), "LOGIN", "User", user.getId(), null, null);
        return issueTokens(user);
    }

    @Override
    @Transactional
    public TokenRefreshResponse refreshToken(String rawRefreshToken) {
        RefreshToken existing = refreshTokenRepository.findByToken(rawRefreshToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (existing.isRevoked() || existing.isExpired()) {
            throw new UnauthorizedException("Refresh token has expired or been revoked. Please log in again.");
        }

        User user = existing.getUser();

        // Rotate: revoke the used token and issue a brand new one, so a
        // leaked refresh token cannot be replayed after legitimate use.
        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        String newRefreshToken = createAndPersistRefreshToken(user).getToken();
        String accessToken = generateAccessToken(user);

        return TokenRefreshResponse.of(accessToken, newRefreshToken, jwtProperties.accessTokenExpirationMs());
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(token -> refreshTokenRepository.revokeByToken(token.getToken()));
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .token(token)
                    .expiresAt(Instant.now().plusMillis(passwordResetProperties.tokenExpirationMs()))
                    .build();
            passwordResetTokenRepository.save(resetToken);

            // Email delivery is intentionally out of scope for this phase —
            // no email provider is wired up yet. Logging the token keeps the
            // flow testable end-to-end locally until that integration lands.
            // DEBUG (not INFO): dev/local profiles enable com.legalmetrology at
            // DEBUG so this is visible where it's needed for testing, while the
            // prod profile stays at INFO — a raw, unhashed reset token must
            // never reach production logs, which anyone with log access could
            // use to take over any account.
            log.debug("Password reset requested for {}. Reset token (dev-only log): {}", user.getEmail(), token);
        });
        // Deliberately do not reveal whether the email exists — same response either way.
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token"));

        if (resetToken.isUsed() || resetToken.isExpired()) {
            throw new BadRequestException("Invalid or expired password reset token");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        refreshTokenRepository.revokeAllByUser(user);
        log.info("Password reset completed for {}", user.getEmail());
        auditLogService.record(user.getId(), "PASSWORD_RESET", "User", user.getId(), null, null);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = generateAccessToken(user);
        String refreshToken = createAndPersistRefreshToken(user).getToken();
        return AuthResponse.of(accessToken, refreshToken, jwtProperties.accessTokenExpirationMs(),
                userMapper.toResponse(user));
    }

    private String generateAccessToken(User user) {
        UserPrincipal principal = UserPrincipal.from(user);
        return jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), principal.getAuthorities().stream().toList());
    }

    private RefreshToken createAndPersistRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(Instant.now().plusMillis(jwtProperties.refreshTokenExpirationMs()))
                .build();
        return refreshTokenRepository.save(refreshToken);
    }
}
