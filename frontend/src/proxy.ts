import { NextResponse, type NextRequest } from "next/server";

import { ACCESS_COOKIE, REFRESH_COOKIE } from "@/lib/auth/cookie-names";

/*
 * Proxy (Next.js 16's renamed Middleware) — UX optimisation ONLY (design D5).
 * It checks for the *presence* of a session cookie and fast-redirects; it never
 * decodes/validates tokens, calls the backend, or refreshes. Real validation is
 * the job of the /api/graphql route (the authoritative boundary). Not a security
 * gate: a present-but-invalid cookie passes here and is rejected at the API.
 */
const PUBLIC_PATHS = ["/", "/login", "/register", "/scenes"];

function isPublic(pathname: string): boolean {
  return PUBLIC_PATHS.some((p) => pathname === p || (p !== "/" && pathname.startsWith(p + "/")));
}

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const hasSession = Boolean(
    request.cookies.get(ACCESS_COOKIE)?.value || request.cookies.get(REFRESH_COOKIE)?.value,
  );

  if (!hasSession && !isPublic(pathname)) {
    const url = request.nextUrl.clone();
    url.pathname = "/login";
    return NextResponse.redirect(url);
  }

  if (hasSession && (pathname === "/login" || pathname === "/register")) {
    const url = request.nextUrl.clone();
    url.pathname = "/saves";
    return NextResponse.redirect(url);
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/((?!api|_next/static|_next/image|favicon.ico).*)"],
};
