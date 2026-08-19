import { LogoutButton } from "@/components/app/logout-button";
import { Wordmark } from "@/components/app/wordmark";

export function AppHeader() {
  return (
    <header className="border-divider border-b">
      <div className="mx-auto flex h-16 w-full max-w-[var(--container-page)] items-center justify-between px-6">
        <Wordmark href="/saves" />
        <LogoutButton />
      </div>
    </header>
  );
}
