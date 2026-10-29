import type { Point, ResolvedShot } from "@/lib/play/hole-geometry";

export type ShotTrace = NonNullable<ResolvedShot["trace"]>;
export type TracePlaybackPhase = "airborne" | "contact" | "roll" | "transition" | "final";

export interface TracePlaybackFrame {
  readonly phase: TracePlaybackPhase;
  /** The authoritative yard-space ground track, even while the ball is visibly raised above it. */
  readonly point: Point;
  readonly height?: number;
  readonly finalVisible: boolean;
}

export interface TracePlaybackTiming {
  readonly flightMs: number;
  readonly contactMs: number;
  readonly rollMs: number;
  readonly transitionMs: number;
  readonly totalMs: number;
}

const clamp = (value: number, minimum: number, maximum: number) =>
  Math.min(maximum, Math.max(minimum, value));

/** Returned endpoints determine pacing; the prototype's four shots are references, not presets. */
export function tracePlaybackTiming(trace: ShotTrace | null | undefined): TracePlaybackTiming {
  if (!trace) return { flightMs: 650, contactMs: 0, rollMs: 0, transitionMs: 0, totalMs: 650 };
  // Legacy putting has no airborne samples. Keep it outside the non-putt motion change.
  if (trace.club === "PUTTER") return { flightMs: 650, contactMs: 0, rollMs: 0, transitionMs: 0, totalMs: 650 };

  const carry = Math.hypot(trace.contact.position.x - trace.origin.x, trace.contact.position.y - trace.origin.y);
  const release = trace.roll
    ? Math.hypot(trace.roll.to.x - trace.roll.from.x, trace.roll.to.y - trace.roll.from.y) : 0;
  const running = clamp((release / Math.max(carry, 1) - 0.04) / 0.12, 0, 1)
    * clamp((45 - carry) / 30, 0, 1);
  const flightMs = clamp(Math.round((540 + 84 * Math.sqrt(carry)) * (1 - 0.33 * running)), 400, 2200);
  const contactMs = clamp(Math.round(46 + 2.4 * Math.sqrt(carry)), 48, 90);
  const rollMs = release > 0 ? clamp(Math.round((90 + 137 * Math.sqrt(release)) * (1 - 0.33 * running)), 90, 850) : 0;
  const transitionMs = trace.transition ? 280 : 0;
  return { flightMs, contactMs, rollMs, transitionMs,
    totalMs: flightMs + contactMs + rollMs + transitionMs };
}

export function tracePlaybackDurationMs(trace: ShotTrace | null | undefined): number {
  return tracePlaybackTiming(trace).totalMs;
}

/** A completed trace always owns result feedback over live planning state. */
export function feedbackAimPoint(trace: ShotTrace | null | undefined, planningAim: Point | undefined): Point | undefined {
  return trace?.aimPoint ?? planningAim;
}

/** Interpolate only solver-authored samples; historical traces without height stay on the ground plane. */
function airborneSample(trace: ShotTrace, progress: number): { position: Point; height: number } {
  const t = clamp(progress, 0, 1);
  if (t === 0) return { position: trace.origin, height: 0 };
  if (t === 1) return { position: trace.contact.position, height: 0 };
  const path = trace.airbornePath ?? [];
  if (path.length > 0) {
    const next = path.find((point) => point.progress >= t) ?? path[path.length - 1];
    const previous = [...path].reverse().find((point) => point.progress <= t) ?? path[0];
    const span = next.progress - previous.progress;
    const local = span === 0 ? 0 : (t - previous.progress) / span;
    return {
      position: { x: previous.position.x + (next.position.x - previous.position.x) * local,
        y: previous.position.y + (next.position.y - previous.position.y) * local },
      height: Math.max(0, previous.height + (next.height - previous.height) * local),
    };
  }
  return { position: {
    x: trace.origin.x + (trace.contact.position.x - trace.origin.x) * t,
    y: trace.origin.y + (trace.contact.position.y - trace.origin.y) * t,
  }, height: 0 };
}

export function interpolatedAirbornePoint(trace: ShotTrace, progress: number): Point {
  return airborneSample(trace, progress).position;
}

/** Restrained screen-space depth only; it never changes the yard-space track or contact. */
export function traceHeightCue(heightYards: number): { lift: number; scale: number; shadowOpacity: number } {
  const lift = clamp(1.7 * Math.sqrt(Math.max(0, heightYards)), 0, 9);
  return { lift, scale: 1 + Math.min(0.16, lift * 0.018), shadowOpacity: Math.max(0.18, 0.42 - lift * 0.024) };
}

/** Project the ground track once, then offset only the visible ball; aiming geometry stays unchanged. */
export function traceBallProjection(frame: TracePlaybackFrame, project: (point: Point) => Point) {
  const ground = project(frame.point);
  const cue = traceHeightCue(frame.phase === "airborne" ? frame.height ?? 0 : 0);
  return { ground, ball: { x: ground.x, y: ground.y - cue.lift }, ...cue };
}

/** @deprecated Compatibility alias; consumers should use the resolver-authored airborne path function. */
export const interpolatedContactPoint = interpolatedAirbornePoint;

/**
 * Presentation-only trace sequencing. Every endpoint comes from the resolver; this only chooses when each
 * authoritative phase is shown. Rules recovery is kept distinct from ground roll.
 */
export function tracePlaybackFrame(trace: ShotTrace, progress: number): TracePlaybackFrame {
  const timing = tracePlaybackTiming(trace);
  const elapsed = clamp(progress, 0, 1) * timing.totalMs;
  if (elapsed >= timing.totalMs) return { phase: "final", point: trace.finalPoint, finalVisible: true };
  if (elapsed < timing.flightMs) {
    const sample = airborneSample(trace, elapsed / timing.flightMs);
    return { phase: "airborne", point: sample.position, height: sample.height, finalVisible: false };
  }
  if (elapsed < timing.flightMs + timing.contactMs) {
    return { phase: "contact", point: trace.contact.position, finalVisible: false };
  }
  if (trace.roll && timing.rollMs > 0) {
    const local = clamp((elapsed - timing.flightMs - timing.contactMs) / timing.rollMs, 0, 1);
    const eased = 1 - Math.pow(1 - local, 2.5);
    return { phase: "roll", point: {
      x: trace.roll.from.x + (trace.roll.to.x - trace.roll.from.x) * eased,
      y: trace.roll.from.y + (trace.roll.to.y - trace.roll.from.y) * eased,
    }, finalVisible: false };
  }
  if (trace.transition) {
    const local = clamp((elapsed - timing.flightMs - timing.contactMs) / timing.transitionMs, 0, 1);
    return { phase: "transition", point: {
      x: trace.transition.from.x + (trace.transition.to.x - trace.transition.from.x) * local,
      y: trace.transition.from.y + (trace.transition.to.y - trace.transition.from.y) * local,
    }, finalVisible: false };
  }
  return { phase: "contact", point: trace.contact.position, finalVisible: false };
}

/** Visible, presentation-only terminology for authoritative playback facts. */
export function tracePlaybackStatus(trace: ShotTrace | null | undefined, frame: TracePlaybackFrame | null): string | null {
  if (!trace || !frame) return null;
  if (frame.phase === "airborne") return "Airborne";
  if (frame.phase === "contact") return "Contact";
  if (frame.phase === "roll") return "Rolling";
  if (frame.phase === "final") return "Final";
  return null;
}

/** The aim canvas is locked only for an active authoritative result playback. */
export function isAimPointInteractionLocked(hasActivePlayback: boolean): boolean {
  return hasActivePlayback;
}

/** Human-readable recovery/replay state, explicitly separate from shot travel. */
export function transitionLabel(trace: ShotTrace | null | undefined): string | null {
  return trace?.transition ? trace.transition.kind.replace(/_/g, " ").toLowerCase() : null;
}
