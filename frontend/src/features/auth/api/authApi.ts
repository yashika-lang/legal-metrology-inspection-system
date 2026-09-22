import { apiClient, unwrap } from "@/services/apiClient";
import type { ApiResponse } from "@/types/api.types";
import type { AuthResponse, LoginRequest, SignupRequest, UserResponse } from "../types";

export const authApi = {
  login: (payload: LoginRequest) =>
    unwrap(apiClient.post<ApiResponse<AuthResponse>>("/auth/login", payload)),

  signup: (payload: SignupRequest) =>
    unwrap(apiClient.post<ApiResponse<AuthResponse>>("/auth/signup", payload)),

  me: () => unwrap(apiClient.get<ApiResponse<UserResponse>>("/auth/me")),

  logout: (refreshToken: string) => apiClient.post("/auth/logout", { refreshToken }),
};
