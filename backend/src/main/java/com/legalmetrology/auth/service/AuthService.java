package com.legalmetrology.auth.service;

import com.legalmetrology.auth.dto.AuthResponse;
import com.legalmetrology.auth.dto.LoginRequest;
import com.legalmetrology.auth.dto.SignupRequest;
import com.legalmetrology.auth.dto.TokenRefreshResponse;

public interface AuthService {

    AuthResponse signup(SignupRequest request);

    AuthResponse login(LoginRequest request);

    TokenRefreshResponse refreshToken(String refreshToken);

    void logout(String refreshToken);

    void forgotPassword(String email);

    void resetPassword(String token, String newPassword);
}
