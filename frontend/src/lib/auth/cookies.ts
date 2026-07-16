import type { NextResponse } from "next/server";
import { cookies } from "next/headers";

import {
  ACCESS_COOKIE,
  ACCESS_MAX_AGE,
  REFRESH_COOKIE,
  REFRESH_MAX_AGE,
} from "@/lib/auth/cookie-names";

/*
 * Cookie policy (design D1). Two httpOnly cookies hold the tokens so they are
 * never readable from client JavaScript; Max-Age mirrors the backend TTLs.
 * Secure is on everywhere except local dev over http (localhost).
 */

const baseOptions = {
  httpOnly: true,
  secure: process.env.NODE_ENV === "production",
  sameSite: "lax" as const,
  path: "/",
};

/** Write the access cookie (used on both login and silent refresh). */
export function setAccessCookie(res: NextResponse, accessToken: string) {
  res.cookies.set({
    name: ACCESS_COOKIE,
    value: accessToken,
    ...baseOptions,
    maxAge: ACCESS_MAX_AGE,
  });
}

/** Write the refresh cookie (used on login only — refresh does not rotate it). */
export function setRefreshCookie(res: NextResponse, refreshToken: string) {
  res.cookies.set({
    name: REFRESH_COOKIE,
    value: refreshToken,
    ...baseOptions,
    maxAge: REFRESH_MAX_AGE,
  });
}

/** Delete both cookies (logout, or when refresh is rejected). */
export function clearAuthCookies(res: NextResponse) {
  res.cookies.set({ name: ACCESS_COOKIE, value: "", ...baseOptions, maxAge: 0 });
  res.cookies.set({ name: REFRESH_COOKIE, value: "", ...baseOptions, maxAge: 0 });
}

/** Read the current tokens from the request cookies (server-side only). */
export async function readAuthCookies() {
  const store = await cookies();
  return {
    accessToken: store.get(ACCESS_COOKIE)?.value,
    refreshToken: store.get(REFRESH_COOKIE)?.value,
  };
}
