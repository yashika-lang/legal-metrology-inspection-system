/**
 * The one place access/refresh tokens are read from and written to.
 * localStorage (not memory-only) so a page refresh doesn't force a
 * re-login — the backend's refresh-token rotation (see
 * AuthServiceImpl.refreshToken) is what actually keeps this safe long-term.
 */
const ACCESS_TOKEN_KEY = "nirikshan.accessToken";
const REFRESH_TOKEN_KEY = "nirikshan.refreshToken";

export const tokenStore = {
  getAccessToken: () => localStorage.getItem(ACCESS_TOKEN_KEY),
  getRefreshToken: () => localStorage.getItem(REFRESH_TOKEN_KEY),
  setTokens: (accessToken: string, refreshToken: string) => {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  },
  clear: () => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  },
};
