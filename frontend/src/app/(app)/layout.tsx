import { redirect } from "next/navigation";

import { getSession } from "@/lib/auth/session";
import { AppHeader } from "@/components/app/app-header";

/*
 * Protected shell. Presence-gated server-side (design D5): unauthenticated users
 * are redirected to /login. Authoritative token validity is enforced downstream at
 * the /api/graphql boundary. Marked dynamic so per-user state is never cached (D6).
 */
export const dynamic = "force-dynamic";

export default async function AppLayout({ children }: { children: React.ReactNode }) {
  const { authenticated } = await getSession();
  if (!authenticated) redirect("/login");

  return (
    <div className="flex min-h-full flex-1 flex-col">
      <AppHeader />
      <main className="mx-auto w-full max-w-[var(--container-page)] flex-1 px-6 py-10">
        {children}
      </main>
    </div>
  );
}
