import { NextResponse } from "next/server";

import { clearAuthCookies } from "@/lib/auth/cookies";

export const dynamic = "force-dynamic";

/*
 * Logout deletes both cookies. It performs no refresh afterwards. Note the backend
 * cannot revoke the refresh token server-side (no revocation store, design D1);
 * clearing the cookie is the only server-visible action. The client clears its
 * TanStack Query cache after this returns so no authenticated data lingers.
 */
export async function POST() {
  const res = NextResponse.json({ ok: true });
  clearAuthCookies(res);
  return res;
}
