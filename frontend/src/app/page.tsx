import Link from "next/link";

import { Button } from "@/components/ui/button";
import { Wordmark } from "@/components/app/wordmark";

export default function Home() {
  return (
    <div className="command-centre flex min-h-dvh flex-1 flex-col">
      <header className="mx-auto flex w-full max-w-[var(--container-page)] items-center px-6 py-6">
        <Wordmark subtitle="Command Centre" />
      </header>

      <main className="flex flex-1 flex-col items-center justify-center px-6 py-16">
        <div className="flex max-w-[var(--container-content)] flex-col items-center gap-6 text-center">
          <p className="text-subtle-foreground font-mono text-xs tracking-[0.18em] uppercase">
            Career mode · Weekly turns
          </p>
          <h1 className="text-foreground text-5xl leading-[1.03] font-bold tracking-[-0.03em] text-balance sm:text-6xl">
            Build a legacy over decades.
          </h1>
          <p className="text-muted-foreground max-w-[52ch] text-lg leading-relaxed text-pretty">
            Guide one professional golfer across a multi-decade career — you make the strategic
            decisions, the simulation resolves the outcomes.
          </p>
          <div className="mt-3 flex flex-wrap items-center justify-center gap-3">
            <Button asChild size="lg">
              <Link href="/login">Sign in</Link>
            </Button>
            <Button asChild variant="secondary" size="lg">
              <Link href="/register">Create account</Link>
            </Button>
          </div>
        </div>
      </main>
    </div>
  );
}
