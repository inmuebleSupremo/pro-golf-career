"use client";

import { AnimatePresence } from "motion/react";
import { Flame } from "lucide-react";

import { cn } from "@/lib/utils";
import { humanize } from "@/lib/play/options";
import { Hole2d } from "@/components/play/hole-2d";
import { PostHoleOverlay, PreHoleOverlay } from "@/components/play/hole-overlays";
import type { ShotPhysicsProfile } from "@/lib/play/shot-router";
import type { HoleGeom, ResolvedShot } from "@/lib/play/hole-geometry";
import type { PostHoleSummary, SeqPhase } from "@/components/play/use-play-sequence";
import type { Situation } from "@/components/play/play-shared";

// Between-hole transition timing (spec: web-hole-visualization). The wipe hides the hole swap; its CSS sweep
// duration is kept in step with the sequence's `wipe` phase length.
const WIPE_MS = 800;

/**
 * The stage's hole — a presentational surface driven entirely by the play sequence (see `usePlaySequence`). It
 * paints the currently displayed hole and its last shot, layers the pre-/post-hole overlays for the `intro` and
 * `score` phases, and covers the between-hole swap with the diagonal wipe. It owns no timing of its own; the
 * sequence decides what phase is live and which hole is shown.
 */
export function StageHole({
  displayHole,
  situation,
  pressure,
  playbackShot,
  playbackProfile,
  phase,
  postHole,
}: {
  displayHole: HoleGeom | null;
  situation: Situation;
  pressure: number | null;
  playbackShot: ResolvedShot | null;
  playbackProfile: ShotPhysicsProfile | null;
  phase: SeqPhase;
  postHole: PostHoleSummary | null;
}) {
  // Chrome (hole chip, pressure) rides with live play and the score beat, but yields to the intro overlay and
  // hides through the wipe so nothing shows over the swap.
  const showChrome = phase === "play" || phase === "holeout" || phase === "score";

  return (
    <>
      {displayHole ? (
        <div className="flex h-full w-full items-center justify-center">
          <Hole2d hole={displayHole} ball={playbackShot} profile={playbackProfile} className="block max-h-full w-auto" />
        </div>
      ) : (
        <div className="bg-surface-3 aspect-[1/2] h-[70%] animate-pulse rounded-lg" />
      )}

      {phase === "wipe" ? <WipePanel /> : null}

      <AnimatePresence>
        {phase === "intro" && displayHole ? (
          <PreHoleOverlay key="intro" holeNumber={situation.holeNumber} par={situation.par} length={displayHole.length} />
        ) : null}
        {phase === "score" && postHole ? <PostHoleOverlay key="score" summary={postHole} /> : null}
      </AnimatePresence>

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
