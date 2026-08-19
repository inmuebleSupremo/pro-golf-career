import { AppHeader } from "@/components/app/app-header";

/*
 * Standard chrome: a top header + a centered content column, on the command-centre
 * ground so the home/new-career screens read as the same product as the game.
 * Wraps the home menu and the create-a-golfer flow. Auth is enforced by the parent
 * (app) layout.
 */
export default function StandardLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="command-centre flex min-h-dvh flex-1 flex-col">
      <AppHeader />
      <main className="mx-auto w-full max-w-[var(--container-page)] flex-1 px-6 py-10">
        {children}
      </main>
    </div>
  );
}
