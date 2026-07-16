import { NextResponse } from "next/server";

import { backendGraphql, backendRefresh } from "@/lib/auth/backend";
import { clearAuthCookies, readAuthCookies, setAccessCookie } from "@/lib/auth/cookies";

export const dynamic = "force-dynamic";

/*
 * The same-origin GraphQL proxy and authoritative auth boundary (design D1/D5).
 * The browser calls this route (never the cross-origin backend directly); the
 * access token is attached server-side. Refresh is triggered ONLY on HTTP 401
 * from the backend — a GraphQL 200 carrying `errors` is a domain error and is
 * passed through untouched.
 */
export async function POST(request: Request) {
  const { accessToken, refreshToken } = await readAuthCookies();
  const body = await request.text();

  const sessionExpired = () => {
    const res = NextResponse.json(
      { errors: [{ message: "Your session has expired. Sign in again." }] },
      { status: 401 },
    );
    clearAuthCookies(res);
    return res;
  };

  // Track a freshly-minted access token so we can write it back on the way out.
  let refreshedAccess: string | null = null;
  let token = accessToken;

  // No access cookie (lapsed) but a refresh cookie present → refresh up front.
  if (!token) {
    if (!refreshToken) {
      return NextResponse.json({ errors: [{ message: "Not authenticated." }] }, { status: 401 });
    }
    refreshedAccess = await backendRefresh(refreshToken);
    if (!refreshedAccess) return sessionExpired();
    token = refreshedAccess;
  }

  let backendRes = await backendGraphql(token, body);

  // Access token rejected at the backend → refresh once and retry.
  if (backendRes.status === 401 && !refreshedAccess && refreshToken) {
    refreshedAccess = await backendRefresh(refreshToken);
    if (!refreshedAccess) return sessionExpired();
    backendRes = await backendGraphql(refreshedAccess, body);
  }

  if (backendRes.status === 401) return sessionExpired();

  const payload = await backendRes.text();
  const res = new NextResponse(payload, {
    status: backendRes.status,
    headers: { "content-type": backendRes.headers.get("content-type") ?? "application/json" },
  });
  if (refreshedAccess) setAccessCookie(res, refreshedAccess);
  return res;
}
