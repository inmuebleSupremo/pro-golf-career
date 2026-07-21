import { redirect } from "next/navigation";

import { getSession } from "@/lib/auth/session";

/*
 * Protected app boundary. Presence-gated server-side (design D5): unauthenticated
 * users are redirected to /login; token validity is enforced at /api/graphql (D6).
 * Renders children full-bleed — each route group supplies its own chrome: the
 * standard group a top header + centered column, the career route the command-centre
 * shell. Marked dynamic so per-user state is never cached.
 */
export const dynamic = "force-dynamic";

export default async function AppLayout({ children }: { children: React.ReactNode }) {
  const { authenticated } = await getSession();
  if (!authenticated) redirect("/login");

  return <div className="flex min-h-dvh flex-col">{children}</div>;
}
