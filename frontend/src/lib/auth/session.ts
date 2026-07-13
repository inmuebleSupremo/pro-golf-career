import "server-only";

import { readAuthCookies } from "@/lib/auth/cookies";

/*
 * Server-side session read for gating protected layouts. Presence-based: a session
 * exists if either token cookie is present (the access token may have lapsed but be
 * refreshable). Real token validity is enforced at the /api/graphql boundary, which
 * returns 401 (and clears cookies) when a token can neither be used nor refreshed.
 */
export async function getSession(): Promise<{ authenticated: boolean }> {
  const { accessToken, refreshToken } = await readAuthCookies();
  return { authenticated: Boolean(accessToken || refreshToken) };
}
