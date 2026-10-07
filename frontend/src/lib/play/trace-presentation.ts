import type { Point, ResolvedShot } from "@/lib/play/hole-geometry";

export type ShotTrace = NonNullable<ResolvedShot["trace"]>;

/** A completed trace always owns result feedback over live planning state. */
export function feedbackAimPoint(trace: ShotTrace | null | undefined, planningAim: Point | undefined): Point | undefined {
  return trace?.aimPoint ?? planningAim;
}

/** Display-only interpolation between authoritative endpoints; it is not physical trajectory data. */
export function interpolatedContactPoint(trace: ShotTrace, progress: number): Point {
  const t = Math.max(0, Math.min(1, progress));
  return {
    x: trace.origin.x + (trace.contact.position.x - trace.origin.x) * t,
    y: trace.origin.y + (trace.contact.position.y - trace.origin.y) * t,
  };
}

/** Human-readable recovery/replay state, explicitly separate from shot travel. */
export function transitionLabel(trace: ShotTrace | null | undefined): string | null {
  return trace?.transition ? trace.transition.kind.replace(/_/g, " ").toLowerCase() : null;
}
