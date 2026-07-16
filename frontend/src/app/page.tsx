export default function Home() {
  return (
    <main className="flex flex-1 flex-col items-center justify-center px-6 py-24">
      <div className="flex max-w-[var(--container-content)] flex-col items-center gap-6 text-center">
        <p className="text-subtle-foreground font-mono text-xs tracking-[0.18em] uppercase">
          Season 1 · Week 1
        </p>
        <h1 className="text-foreground font-serif text-5xl leading-[1.05] font-medium tracking-[-0.02em] text-balance">
          Build a legacy over decades.
        </h1>
        <p className="text-muted-foreground max-w-[52ch] text-lg leading-relaxed text-pretty">
          Guide one professional golfer across a multi-decade career — you make the strategic
          decisions, the simulation resolves the outcomes.
        </p>
        <div className="mt-2 flex items-center gap-3">
          <a
            href="/login"
            className="bg-primary text-primary-foreground hover:bg-primary-hover inline-flex h-[var(--control-lg)] items-center justify-center rounded-md px-6 text-sm font-medium shadow-sm transition-colors duration-[var(--duration-base)] ease-[var(--ease-out)]"
          >
            Sign in
          </a>
          <a
            href="/register"
            className="border-border text-foreground hover:bg-surface inline-flex h-[var(--control-lg)] items-center justify-center rounded-md border px-6 text-sm font-medium transition-colors duration-[var(--duration-base)] ease-[var(--ease-out)]"
          >
            Create account
          </a>
        </div>
      </div>
    </main>
  );
}
