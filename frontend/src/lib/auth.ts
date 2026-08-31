// Known simplification: tokens are stored in localStorage for now, which is
// simple but vulnerable to XSS (any injected script can read it). A more
// production-appropriate approach is an httpOnly cookie set by a backend
// endpoint, which client-side JS can't read at all. Fine for local dev.

const ACCESS_TOKEN_KEY = "banking_access_token";
const REFRESH_TOKEN_KEY = "banking_refresh_token";

export function saveTokens(accessToken: string, refreshToken: string) {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
}

export function getAccessToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getRefreshToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function clearTokens() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}
