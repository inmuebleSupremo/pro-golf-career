"use client";

import { motion, useReducedMotion } from "motion/react";

import { cn } from "@/lib/utils";
import { formatScore, scoreToneClass } from "@/components/play/play-shared";
import type { PostHoleSummary } from "@/components/play/use-play-sequence";

/**
 * The start- and end-of-hole overlays (spec: Play Event polish steps 1 & 4). Both sit in the foreground over the
 * 2D hole and share one restrained, typography-led treatment: a soft centered panel, a serif headline, and a
 * short caption — a "refined gamified" beat that stays inside the app's motion doctrine (state, not decoration;
 * a single subtle rise; reduced motion collapses to a plain fade).
 */

/** The name of a hole result relative to par (e.g. -1 → "Birdie"). */
function resultLabel(strokes: number, toPar: number): string {
  if (strokes === 1) return "Hole in one";
  switch (toPar) {
    case -3:
      return "Albatross";
    case -2:
      return "Eagle";
    case -1:
      return "Birdie";
    case 0:
      return "Par";
    case 1:
      return "Bogey";
    case 2:
      return "Double bogey";
    case 3:
      return "Triple bogey";
    default:
      return toPar > 0 ? `${toPar} over` : `${-toPar} under`;
  }
}

/** Shared centered-panel shell for both overlays. */
function OverlayPanel({ children }: { children: React.ReactNode }) {
  const reduced = useReducedMotion();
  return (
    <div className="pointer-events-none absolute inset-0 z-30 flex items-center justify-center" aria-live="polite">
      <motion.div
        initial={reduced ? { opacity: 0 } : { opacity: 0, y: 10, scale: 0.98 }}
        animate={reduced ? { opacity: 1 } : { opacity: 1, y: 0, scale: 1 }}
        exit={reduced ? { opacity: 0 } : { opacity: 0, y: -8, scale: 0.98 }}
        transition={{ duration: 0.28, ease: "easeOut" }}
        className="border-border/70 bg-background/70 flex min-w-56 flex-col items-center gap-1 rounded-2xl border px-8 py-6 text-center shadow-lg backdrop-blur-md"
      >
        {children}
      </motion.div>
    </div>
  );
}

/** Pre-hole intro: hole number, par, and yardage, shown as the new hole comes into view. */
export function PreHoleOverlay({ holeNumber, par, length }: { holeNumber: number; par: number; length: number }) {
  return (
    <OverlayPanel>
      <span className="text-subtle-foreground text-[11px] font-medium tracking-[0.16em] uppercase">Hole</span>
      <span className="font-serif text-6xl leading-none font-medium tracking-[-0.02em] tabular-nums">{holeNumber}</span>
      <span className="text-muted-foreground mt-1 font-mono text-sm tabular-nums">
        Par {par} · {length} yds
      </span>
    </OverlayPanel>
  );
}

/** Post-hole score: the result headline, then hole / round / (from round 2) event totals. */
export function PostHoleOverlay({ summary }: { summary: PostHoleSummary }) {
  const tone = scoreToneClass(summary.toParHole);
  return (
    <OverlayPanel>
      <span className={cn("font-serif text-4xl leading-none font-medium tracking-[-0.02em]", tone)}>
        {resultLabel(summary.strokes, summary.toParHole)}
      </span>
      <span className="text-muted-foreground mt-1 font-mono text-sm tabular-nums">
        {summary.strokes} {summary.strokes === 1 ? "stroke" : "strokes"}
      </span>
      <div className="border-border/60 mt-4 flex items-stretch gap-5 border-t pt-4">
        <ScoreStat label="Hole" value={formatScore(summary.toParHole)} tone={scoreToneClass(summary.toParHole)} />
        <ScoreStat label="Round" value={formatScore(summary.roundToPar)} tone={scoreToneClass(summary.roundToPar)} />
        {summary.eventToPar !== null ? (
          <ScoreStat label="Event" value={formatScore(summary.eventToPar)} tone={scoreToneClass(summary.eventToPar)} />
        ) : null}
      </div>
    </OverlayPanel>
  );
}

function ScoreStat({ label, value, tone }: { label: string; value: string; tone: string }) {
  return (
    <div className="flex flex-col items-center gap-1">
      <span className="text-subtle-foreground text-[10px] tracking-[0.08em] uppercase">{label}</span>
      <span className={cn("font-mono text-lg font-semibold tabular-nums", tone)}>{value}</span>
    </div>
  );
}
