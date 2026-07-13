import "server-only";

/*
 * Server-side calls to the Spring backend's REST /auth endpoints. This module is
 * the only place the backend base URL is used; it never runs in the browser.
 */

function backendUrl(path: string): string {
  const base = process.env.BACKEND_INTERNAL_URL ?? "http://localhost:8080";
  return new URL(path, base).toString();
}

export interface TokenPair {
  accessToken: string;
  refreshToken: string;
}

/** POST /auth/login. Returns the token pair, or the backend status on failure. */
export async function backendLogin(
  username: string,
  password: string,
): Promise<{ ok: true; tokens: TokenPair } | { ok: false; status: number }> {
  const res = await fetch(backendUrl("/auth/login"), {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ username, password }),
    cache: "no-store",
  });
  if (!res.ok) return { ok: false, status: res.status };
  const tokens = (await res.json()) as TokenPair;
  return { ok: true, tokens };
}

/** POST /auth/register. Returns the created account, or the backend status on failure. */
export async function backendRegister(
  username: string,
  password: string,
): Promise<{ ok: true } | { ok: false; status: number }> {
  const res = await fetch(backendUrl("/auth/register"), {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ username, password }),
    cache: "no-store",
  });
  if (!res.ok) return { ok: false, status: res.status };
  return { ok: true };
}

const inFlightRefreshes = new Map<string, Promise<string | null>>();

/**
 * POST /auth/refresh, exchanging a refresh token for a fresh access token.
 * Per-instance single-flight: concurrent calls with the same refresh token share
 * one request. This is an optimisation, not a correctness requirement — the backend
 * does not rotate refresh tokens, so redundant refreshes are harmless (design D1).
 * Returns the new access token, or null when the refresh token is rejected.
 */
export function backendRefresh(refreshToken: string): Promise<string | null> {
  const existing = inFlightRefreshes.get(refreshToken);
  if (existing) return existing;

  const promise = (async () => {
    const res = await fetch(backendUrl("/auth/refresh"), {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ refreshToken }),
      cache: "no-store",
    });
    if (!res.ok) return null;
    const { accessToken } = (await res.json()) as { accessToken: string };
    return accessToken;
  })().finally(() => {
    inFlightRefreshes.delete(refreshToken);
  });

  inFlightRefreshes.set(refreshToken, promise);
  return promise;
}

/** Forward a GraphQL request body to the backend /graphql with a Bearer token. */
export async function backendGraphql(accessToken: string, body: string): Promise<Response> {
  return fetch(backendUrl("/graphql"), {
    method: "POST",
    headers: {
      "content-type": "application/json",
      authorization: `Bearer ${accessToken}`,
    },
    body,
    cache: "no-store",
  });
}
