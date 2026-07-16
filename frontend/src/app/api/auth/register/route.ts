import { NextResponse } from "next/server";
import { z } from "zod";

import { backendRegister } from "@/lib/auth/backend";

export const dynamic = "force-dynamic";

const schema = z.object({
  username: z.string().min(3, "Username must be at least 3 characters."),
  password: z.string().min(8, "Password must be at least 8 characters."),
});

export async function POST(request: Request) {
  const body = await request.json().catch(() => null);
  const parsed = schema.safeParse(body);
  if (!parsed.success) {
    const error = parsed.error?.issues[0]?.message ?? "Check your details and try again.";
    return NextResponse.json({ error }, { status: 400 });
  }

  const result = await backendRegister(parsed.data.username, parsed.data.password);
  if (!result.ok) {
    if (result.status === 409) {
      return NextResponse.json({ error: "That username is already taken." }, { status: 409 });
    }
    return NextResponse.json(
      { error: "Couldn't create your account. Try again." },
      { status: 502 },
    );
  }

  return NextResponse.json({ ok: true }, { status: 201 });
}
