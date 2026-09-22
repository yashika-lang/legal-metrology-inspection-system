/** Mirrors `com.legalmetrology.auth.dto.UserResponse`. */
export interface UserResponse {
  id: string;
  fullName: string;
  email: string;
  employeeCode: string | null;
  phone: string | null;
  preferredLocale: string;
  active: boolean;
  roles: string[];
  createdAt: string;
}

/** Mirrors `com.legalmetrology.auth.dto.AuthResponse`. */
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInMs: number;
  user: UserResponse;
}

/** Mirrors `com.legalmetrology.auth.dto.LoginRequest`. */
export interface LoginRequest {
  email: string;
  password: string;
}

/** Mirrors `com.legalmetrology.auth.dto.SignupRequest`. */
export interface SignupRequest {
  fullName: string;
  email: string;
  password: string;
  employeeCode?: string;
  phone?: string;
}

/** Mirrors `com.legalmetrology.auth.dto.TokenRefreshResponse`. */
export interface TokenRefreshResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInMs: number;
}
