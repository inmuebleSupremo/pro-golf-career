"use client";

import { createContext, useCallback, useContext, useEffect, useState } from "react";

import {
  DelayedOperationRegistry,
  runTrackedOperation,
  shouldAnimateGolfLoading,
  SingleFlight,
  type ForegroundOperationOptions,
  type ForegroundOperationSnapshot,
} from "@/components/loading/delayed-operation-registry";

type ForegroundLoadingContextValue = {
  readonly hasPendingForegroundOperation: boolean;
  run<T>(options: ForegroundOperationOptions, work: () => Promise<T>): Promise<T>;
};

const ForegroundLoadingContext = createContext<ForegroundLoadingContextValue | null>(null);
const EMPTY_SNAPSHOT: ForegroundOperationSnapshot = { activeCount: 0, visibleOperations: [] };

export function ForegroundLoadingProvider({ children }: { children: React.ReactNode }) {
  const [registry] = useState(() => new DelayedOperationRegistry());
  const [snapshot, setSnapshot] = useState<ForegroundOperationSnapshot>(EMPTY_SNAPSHOT);

  useEffect(() => {
    return registry.subscribe(() => setSnapshot(registry.snapshot()));
  }, [registry]);

  useEffect(() => () => registry.dispose(), [registry]);

  const run = useCallback(
    async <T,>(options: ForegroundOperationOptions, work: () => Promise<T>): Promise<T> => {
      return runTrackedOperation(registry, options, work);
    },
    [registry],
  );

  const visibleOperation = snapshot.visibleOperations.at(-1) ?? null;
  return (
    <ForegroundLoadingContext.Provider
      value={{ hasPendingForegroundOperation: snapshot.activeCount > 0, run }}
    >
      <div className="contents" inert={visibleOperation ? true : undefined}>
        {children}
      </div>
      {visibleOperation ? <ForegroundLoadingOverlay label={visibleOperation.label} /> : null}
    </ForegroundLoadingContext.Provider>
  );
}

/** Wrap an intentionally enrolled foreground promise without treating every request or route as loading. */
export function useForegroundOperation() {
  const context = useContext(ForegroundLoadingContext);
  if (!context) throw new Error("useForegroundOperation must be used within ForegroundLoadingProvider");
  const [singleFlight] = useState(() => new SingleFlight());
  const [isPending, setIsPending] = useState(false);

  const run = useCallback(
    async <T,>(options: ForegroundOperationOptions, work: () => Promise<T>): Promise<T | undefined> => {
      // `isPending` updates after this event handler returns; the ref closes the small
      // same-tick window in which a rapid second click could otherwise submit twice.
      if (!singleFlight.tryStart()) return undefined;
      setIsPending(true);
      try {
        return await context.run(options, work);
      } finally {
        singleFlight.finish();
        setIsPending(false);
      }
    },
    [context, singleFlight],
  );

  return { ...context, isPending, run };
}

function ForegroundLoadingOverlay({ label }: { label: string }) {
  return (
    <div
      className="command-centre foreground-loading-scrim fixed inset-0 z-[var(--z-toast)] grid place-items-center p-5"
      role="status"
      aria-live="polite"
      aria-label={label}
    >
      <div className="flex w-full max-w-xs flex-col items-center gap-4 text-center">
        <GolfLoadingMark />
        <p className="text-foreground text-base font-semibold tracking-[-0.01em]">{label}</p>
        <p className="text-muted-foreground -mt-2 text-sm">Working on your career…</p>
      </div>
    </div>
  );
}

/** A compact topographic course study: contours, a one-way route trace, and a flag marker. */
export function GolfLoadingMark() {
  const reducedMotion = usePrefersReducedMotion();
  const traceClass = shouldAnimateGolfLoading(reducedMotion)
    ? "foreground-route-trace"
    : "foreground-route-trace-static";
  return (
    <svg viewBox="0 0 280 132" className="h-auto w-72 max-w-full" aria-hidden="true">
      <g fill="none" stroke="currentColor" strokeWidth="1.25" strokeLinecap="round" className="text-muted-foreground/45">
        <path d="M18 88C40 49 76 35 114 45c35 9 41 36 74 38 33 2 55-19 73-42" />
        <path d="M12 108c28-42 61-57 98-47 34 9 41 35 76 37 38 3 64-20 82-45" />
        <path d="M27 59c26-23 55-29 83-20 27 8 42 28 67 27 28-1 51-21 65-43" />
        <path d="M49 119c24-18 47-24 70-16 28 10 41 27 71 23 26-4 46-21 59-36" />
      </g>
      <path d="M30 104C73 95 98 54 141 64c37 9 56 35 95 5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" className="text-primary/35" />
      <path d="M30 104C73 95 98 54 141 64c37 9 56 35 95 5" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" className={`${traceClass} text-primary`} />
      <g className="text-gold" fill="currentColor" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round">
        <path d="M238 69V32" fill="none" />
        <path d="M239 33h20l-10 8-10-8Z" stroke="none" />
      </g>
    </svg>
  );
}

function usePrefersReducedMotion(): boolean {
  const [reducedMotion, setReducedMotion] = useState(false);
  useEffect(() => {
    const query = window.matchMedia("(prefers-reduced-motion: reduce)");
    const sync = () => setReducedMotion(query.matches);
    sync();
    query.addEventListener("change", sync);
    return () => query.removeEventListener("change", sync);
  }, []);
  return reducedMotion;
}
