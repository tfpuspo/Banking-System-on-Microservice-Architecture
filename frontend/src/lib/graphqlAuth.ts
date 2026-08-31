import { getAccessToken, getRefreshToken, saveTokens, clearTokens } from "./auth";

const API_URL = process.env.NEXT_PUBLIC_GRAPHQL_URL || "http://localhost:8000/graphql";

const REFRESH_MUTATION = `
  mutation RefreshToken($refreshToken: String!) {
    refreshToken(refreshToken: $refreshToken) {
      accessToken
      refreshToken
      expiresIn
    }
  }
`;

type RefreshResponse = {
  refreshToken: { accessToken: string; refreshToken: string; expiresIn: number };
};

async function rawRequest<T>(query: string, variables: Record<string, unknown>, accessToken: string): Promise<T> {
  const res = await fetch(API_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify({ query, variables }),
  });

  const json = await res.json();
  if (json.errors?.length) {
    throw new Error(json.errors[0].message);
  }
  return json.data as T;
}

// Prevents multiple simultaneous 401s from each independently trying to
// refresh — e.g. loadAccounts() fires several parallel accountBalance
// requests, and if the token expired, all of them fail together. Without
// this, each one would race to call refreshToken itself, and the second
// one to finish would try to reuse a refresh token the first one already
// rotated (and invalidated) — see auth-service's refresh rotation, Phase 2.
let refreshInFlight: Promise<string> | null = null;

async function refreshAccessToken(): Promise<string> {
  if (refreshInFlight) return refreshInFlight;

  refreshInFlight = (async () => {
    const currentRefreshToken = getRefreshToken();
    if (!currentRefreshToken) {
      clearTokens();
      throw new Error("No refresh token available — please sign in again.");
    }

    try {
      const data = await rawRequest<RefreshResponse>(REFRESH_MUTATION, { refreshToken: currentRefreshToken }, "");
      saveTokens(data.refreshToken.accessToken, data.refreshToken.refreshToken);
      return data.refreshToken.accessToken;
    } catch (err) {
      clearTokens();
      throw err;
    }
  })();

  try {
    return await refreshInFlight;
  } finally {
    refreshInFlight = null;
  }
}

/**
 * Same as a normal authenticated request, but if the token has expired,
 * transparently refreshes it once and retries — the caller never sees the
 * expiry at all unless the refresh token itself is also invalid (e.g. after
 * logout, or past its own 7-day lifetime).
 */
export async function authedGraphqlRequest<T>(
  query: string,
  variables: Record<string, unknown>,
  accessTokenOverride?: string
): Promise<T> {
  let token = accessTokenOverride ?? getAccessToken();
  if (!token) {
    throw new Error("Not signed in.");
  }

  try {
    return await rawRequest<T>(query, variables, token);
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    const looksExpired = message.toLowerCase().includes("invalid") || message.toLowerCase().includes("expired");

    if (!looksExpired) throw err;

    // one retry, with a freshly refreshed token
    token = await refreshAccessToken();
    return rawRequest<T>(query, variables, token);
  }
}
