import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { tokenStore } from "@/services/tokenStore";
import { authApi } from "@/features/auth/api/authApi";
import type { SignupRequest, UserResponse } from "@/features/auth/types";

interface AuthContextValue {
  user: UserResponse | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (payload: SignupRequest) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const token = tokenStore.getAccessToken();
    if (!token) {
      setIsLoading(false);
      return;
    }
    authApi
      .me()
      .then(setUser)
      .catch(() => tokenStore.clear())
      .finally(() => setIsLoading(false));
  }, []);

  async function login(email: string, password: string) {
    const auth = await authApi.login({ email, password });
    tokenStore.setTokens(auth.accessToken, auth.refreshToken);
    setUser(auth.user);
  }

  /** Signup returns the same `AuthResponse` shape as login, so registration signs the officer straight in — no separate "now log in" step. */
  async function register(payload: SignupRequest) {
    const auth = await authApi.signup(payload);
    tokenStore.setTokens(auth.accessToken, auth.refreshToken);
    setUser(auth.user);
  }

  function logout() {
    const refreshToken = tokenStore.getRefreshToken();
    if (refreshToken) authApi.logout(refreshToken).catch(() => undefined);
    tokenStore.clear();
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, isLoading, isAuthenticated: !!user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
