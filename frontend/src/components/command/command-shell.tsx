import { Sidebar } from "@/components/command/sidebar";
import { IdentityStrip } from "@/components/command/identity-strip";

/*
 * The persistent command-centre shell: a grouped left nav and a player identity strip
 * that stay mounted across navigation (only the page body swaps) — the low-CLS
 * backbone. The `.command-centre` class scopes the dark, broadcast-grade token values
 * to this subtree so the career surface adopts the v2 look without disturbing the rest
 * of the app. See docs/frontend/references.md.
 */
export function CommandShell({ id, children }: { id: string; children: React.ReactNode }) {
  return (
    <div className="command-centre flex min-h-dvh flex-1">
      <Sidebar id={id} />
      <div className="flex min-w-0 flex-1 flex-col">
        <IdentityStrip id={id} />
        <div className="mx-auto w-full max-w-[1500px] flex-1 px-5 py-6 md:px-7 md:py-7">
          {children}
        </div>
      </div>
    </div>
  );
}
