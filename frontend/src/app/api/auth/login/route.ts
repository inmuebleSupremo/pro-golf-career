import { NextResponse } from "next/server";
import { z } from "zod";

import { backendLogin } from "@/lib/auth/backend";
import { setAccessCookie, setRefreshCookie } from "@/lib/auth/cookies";

export const dynamic = "force-dynamic";

const schema = z.object({
  username: z.string().min(1),
  password: z.string().min(1),
});

export async function POST(request: Request) {
  const body = await request.json().catch(() => null);
  const parsed = schema.safeParse(body);
  if (!parsed.success) {
    return NextResponse.json({ error: "Enter a username and password." }, { status: 400 });
  }

  const result = await backendLogin(parsed.data.username, parsed.data.password);
  if (!result.ok) {
    if (result.status === 401) {
      return NextResponse.json(
        { error: "That username or password isn't right." },
        { status: 401 },
      );
    }
    return NextResponse.json({ error: "Couldn't sign in. Try again." }, { status: 502 });
  }

  const res = NextResponse.json({ ok: true });
  setAccessCookie(res, result.tokens.accessToken);
  setRefreshCookie(res, result.tokens.refreshToken);
  return res;
}
