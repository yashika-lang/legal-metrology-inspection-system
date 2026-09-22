import axios, { type AxiosError, type InternalAxiosRequestConfig } from "axios";
import { tokenStore } from "./tokenStore";
import type { ApiResponse } from "@/types/api.types";
import type { TokenRefreshResponse } from "@/features/auth/types";

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/v1";

export const apiClient = axios.create({
  baseURL: BASE_URL,
  headers: { "Content-Type": "application/json" },
});

apiClient.interceptors.request.use((config) => {
  const token = tokenStore.getAccessToken();
  if (token) {
    config.headers.set("Authorization", `Bearer ${token}`);
  }
  return config;
});

/**
 * Single-flight refresh: if five requests 401 at once, only the first
 * triggers `/auth/refresh` — the rest wait on the same promise instead of
 * each racing to rotate the (single-use) refresh token and invalidating
 * each other.
 */
let refreshPromise: Promise<string> | null = null;

async function refreshAccessToken(): Promise<string> {
  const refreshToken = tokenStore.getRefreshToken();
  if (!refreshToken) throw new Error("No refresh token available");

  const response = await axios.post<ApiResponse<TokenRefreshResponse>>(
    `${BASE_URL}/auth/refresh`,
    { refreshToken },
  );
  const { accessToken, refreshToken: newRefreshToken } = response.data.data;
  tokenStore.setTokens(accessToken, newRefreshToken);
  return accessToken;
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined;

    if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        refreshPromise ??= refreshAccessToken().finally(() => {
          refreshPromise = null;
        });
        const newAccessToken = await refreshPromise;
        originalRequest.headers.set("Authorization", `Bearer ${newAccessToken}`);
        return apiClient(originalRequest);
      } catch {
        tokenStore.clear();
        window.location.assign("/");
      }
    }

    return Promise.reject(error);
  },
);

/** Unwraps the `ApiResponse<T>` envelope every endpoint returns, so callers work with plain `T`. */
export async function unwrap<T>(promise: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  const response = await promise;
  return response.data.data;
}
