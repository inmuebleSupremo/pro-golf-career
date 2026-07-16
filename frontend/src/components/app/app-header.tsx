import Link from "next/link";

import { LogoutButton } from "@/components/app/logout-button";

export function AppHeader() {
  return (
    <header className="border-divider border-b">
      <div className="mx-auto flex h-16 w-full max-w-[var(--container-page)] items-center justify-between px-6">
        <Link href="/saves" className="font-serif text-lg font-medium tracking-[-0.01em]">
          Pro Golf Career
        </Link>
        <LogoutButton />
      </div>
    </header>
  );
}
