import { AppHeader } from "@/components/app/app-header";

/*
 * Standard chrome: a top header + a centered content column. Wraps the saves list
 * and the create-a-golfer flow. Auth is enforced by the parent (app) layout.
 */
export default function StandardLayout({ children }: { children: React.ReactNode }) {
  return (
    <>
      <AppHeader />
      <main className="mx-auto w-full max-w-[var(--container-page)] flex-1 px-6 py-10">
        {children}
      </main>
    </>
  );
}
