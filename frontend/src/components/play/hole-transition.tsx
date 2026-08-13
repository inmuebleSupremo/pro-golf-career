"use client";

import { useEffect, useRef, useState } from "react";
import { Flame } from "lucide-react";

import { cn } from "@/lib/utils";
import { humanize } from "@/lib/play/options";
import { Hole2d } from "@/components/play/hole-2d";
import type { HoleGeom, ResolvedShot } from "@/lib/play/hole-geometry";
import type { Outcome, Situation } from "@/components/play/play-shared";

// Paced between-hole transition (spec: web-hole-visualization). Deliberately unhurried for smoothness:
// hold on the finished hole, a diagonal wipe that hides the swap, an empty beat, then the next hole fades in.
const HOLD_MS = 900;
const WIPE_MS = 800;
const EMPTY_MS = 800;
const REVEAL_MS = 800;

type Phase = "idle" | "hold" | "wipe" | "empty" | "reveal";

function useReducedMotion(): boolean {
  const [reduced, setReduced] = useState(false);
  useEffect(() => {
    const query = window.matchMedia("(prefers-reduced-motion: reduce)");
    const sync = () => setReduced(query.matches);
    sync();
    query.addEventListener("change", sync);
    return () => query.removeEventListener("change", sync);
  }, []);
  return reduced;
}

/**
 * The stage's hole with its between-hole transition. It owns what geometry is painted: on a sequential hole
 * advance it holds the finished hole, wipes to an empty screen (swapping the hole under cover so the change is
 * never seen), pauses, then fades the next hole in. The first hole, a multi-hole sim jump, and reduced motion
 * all skip straight to the new hole. The situation chrome (hole/par chip, pressure) hides through the swap and
 * returns with the revealed hole.
 */
export function StageHole({
  hole,
  situation,
  pressure,
  lastShot,
}: {
  hole: HoleGeom | null;
  situation: Situation;
  pressure: number | null;
  lastShot: Outcome | null;
}) {
  const reduced = useReducedMotion();
  const [displayed, setDisplayed] = useState<HoleGeom | null>(hole);
  const [phase, setPhase] = useState<Phase>("idle");
  const [covered, setCovered] = useState(false);
  const prevNum = useRef<number | null>(hole?.holeNumber ?? null);
  const holeNum = hole?.holeNumber ?? null;

  // Keyed on the hole NUMBER so a refetch of the same hole doesn't restart the transition; `hole` is captured
  // from this render's closure (its number just changed, so it is the incoming hole) intentionally.
  useEffect(() => {
    const prev = prevNum.current;
    prevNum.current = holeNum;

    // First hole, no hole, a jump (sim to round/event), or reduced motion → show immediately, no transition.
    if (holeNum == null || prev == null || holeNum !== prev + 1 || reduced) {
      setDisplayed(hole);
      setCovered(false);
      setPhase("idle");
      return;
    }

    // Sequential advance → run the paced, covered transition.
    setPhase("hold");
    setCovered(false);
    const timers = [
      window.setTimeout(() => setPhase("wipe"), HOLD_MS),
      // Mid-wipe, fully covered: swap the geometry and hide the hole, so the wipe reveals an empty stage.
      window.setTimeout(() => {
        setDisplayed(hole);
        setCovered(true);
      }, HOLD_MS + WIPE_MS / 2),
      window.setTimeout(() => setPhase("empty"), HOLD_MS + WIPE_MS),
      window.setTimeout(() => {
        setPhase("reveal");
        setCovered(false);
      }, HOLD_MS + WIPE_MS + EMPTY_MS),
      window.setTimeout(() => setPhase("idle"), HOLD_MS + WIPE_MS + EMPTY_MS + REVEAL_MS),
    ];
    return () => timers.forEach(clearTimeout);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [holeNum, reduced]);

  const idle = phase === "idle";
  const showChrome = idle || phase === "reveal";
  const shot: ResolvedShot | null =
    idle && lastShot
      ? { finalSurface: lastShot.finalSurface, carry: lastShot.carry, lateral: lastShot.lateral, distanceRemaining: lastShot.distanceRemaining }
      : null;

  const holeClass =
    phase === "reveal"
      ? "opacity-100 duration-[800ms] ease-out"
      : covered
        ? "opacity-0 duration-0"
        : "opacity-100 duration-0";

  return (
    <>
      {displayed ? (
        <div className={cn("flex h-full w-full items-center justify-center transition-opacity", holeClass)}>
          <Hole2d hole={displayed} ball={shot} className="block max-h-full w-auto" />
        </div>
      ) : (
        <div className="bg-surface-3 aspect-[1/2] h-[70%] animate-pulse rounded-lg" />
      )}

      {phase === "wipe" ? <WipePanel /> : null}

      <div
        aria-hidden={!showChrome}
        className={cn("transition-opacity duration-500", showChrome ? "opacity-100" : "opacity-0")}
      >
        <StageChip situation={situation} />
        {pressure && pressure > 0 ? <StagePressure pressure={pressure} /> : null}
      </div>
    </>
  );
}

/** The diagonal wipe: a skewed panel sweeps across, covering at its midpoint (where the swap happens). */
function WipePanel() {
  return (
    <div className="pointer-events-none absolute inset-0 z-20 overflow-hidden" aria-hidden="true">
      <div className="pgw-band" />
      <style>{`
        @keyframes pgw-sweep {
          0%   { transform: translateX(-175%) skewX(-14deg); }
          50%  { transform: translateX(0%) skewX(-14deg); }
          100% { transform: translateX(175%) skewX(-14deg); }
        }
        .pgw-band {
          position: absolute; top: -12%; bottom: -12%; left: 0; width: 175%;
          background: linear-gradient(135deg,
            color-mix(in oklch, var(--surface), transparent 3%) 0%,
            var(--background) 52%,
            color-mix(in oklch, var(--background), black 22%) 100%);
          box-shadow: -24px 0 70px -10px rgba(0,0,0,.55);
          animation: pgw-sweep ${WIPE_MS}ms cubic-bezier(0.76, 0, 0.24, 1) both;
        }
        .pgw-band::before {
          content: ""; position: absolute; left: -3.5%; top: 0; bottom: 0; width: 3px;
          background: color-mix(in oklch, var(--primary), transparent 25%);
          box-shadow: 0 0 22px -2px color-mix(in oklch, var(--primary), transparent 40%);
        }
      `}</style>
    </div>
  );
}

function StageChip({ situation }: { situation: Situation }) {
  return (
    <div className="border-border bg-background/70 absolute top-4 left-4 z-10 rounded-xl border px-3.5 py-2.5 backdrop-blur">
      <div className="flex items-baseline gap-2">
        <span className="font-serif text-lg font-medium tracking-[-0.01em]">Hole {situation.holeNumber}</span>
        <span className="text-muted-foreground text-sm">Par {situation.par}</span>
      </div>
      <p className="text-muted-foreground mt-1 text-xs tabular-nums">
        Shot <span className="text-foreground font-mono">{situation.shotNumber}</span> ·{" "}
        <span className="text-foreground font-mono">{Math.round(situation.distanceToPin)}</span> yds · {humanize(situation.lie)}
      </p>
    </div>
  );
}

function StagePressure({ pressure }: { pressure: number }) {
  const intense = pressure >= 0.6;
  const label = intense ? "Intense pressure" : pressure >= 0.3 ? "High pressure" : "Pressure building";
  return (
    <div
      className={cn(
        "border-border bg-background/70 absolute top-4 right-4 z-10 inline-flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs backdrop-blur",
        intense ? "text-destructive" : "text-warning",
      )}
    >
      <Flame className="size-3.5" aria-hidden="true" />
      {label}
    </div>
  );
}
