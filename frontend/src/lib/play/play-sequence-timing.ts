import type { HoleGeom } from "./hole-geometry";
import type { Situation } from "../../components/play/play-shared";

/** The physical screen is an eligible state only; it is not allowed to pre-empt result playback. */
export function physicalPuttingEligible(hole: HoleGeom | null, situation: Situation): boolean {
  return hole?.puttingPolicy === "PHYSICAL_V1" && situation.lie === "GREEN" && hole.physicalGreen != null;
}

/** Uses the duration from the same renderer that draws the authoritative result. */
export function resolvedPlaybackDurationMs(authoredDuration: number, fallback: number, reducedMotion: boolean): number {
  const duration = authoredDuration || fallback;
  return reducedMotion ? Math.max(1, Math.round(duration * 0.4)) : duration;
}

export function physicalTransitionObservationMs(reducedMotion: boolean): number {
  return reducedMotion ? 250 : 1700;
}
