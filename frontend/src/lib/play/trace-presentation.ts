import type { Point, ResolvedShot } from "@/lib/play/hole-geometry";

export type ShotTrace = NonNullable<ResolvedShot["trace"]>;
export type TracePlaybackPhase = "airborne" | "contact" | "roll" | "transition" | "final";

export interface TracePlaybackFrame {
  readonly phase: TracePlaybackPhase;
  readonly point: Point;
  readonly finalVisible: boolean;
}

const BASE_PLAYBACK_MS = 650;
const ROLL_PLAYBACK_MS = 1050;
const ROLL_AIRBORNE_END = 0.42;
const ROLL_CONTACT_END = 0.57;
const POST_CONTACT_START = 0.72;

/** Positive authoritative releases receive enough display time to be perceived at whole-hole scale. */
export function tracePlaybackDurationMs(trace: ShotTrace | null | undefined): number {
  return trace?.roll ? ROLL_PLAYBACK_MS : BASE_PLAYBACK_MS;
}

/** A completed trace always owns result feedback over live planning state. */
export function feedbackAimPoint(trace: ShotTrace | null | undefined, planningAim: Point | undefined): Point | undefined {
  return trace?.aimPoint ?? planningAim;
}

/** Display-only interpolation between resolver-authored samples; it never generates a flight or collision. */
export function interpolatedAirbornePoint(trace: ShotTrace, progress: number): Point {
  const t = Math.max(0, Math.min(1, progress));
  const path = trace.airbornePath ?? [];
  if (path.length > 0) {
    const next = path.find((point) => point.progress >= t) ?? path[path.length - 1];
    const previous = [...path].reverse().find((point) => point.progress <= t) ?? path[0];
    const span = next.progress - previous.progress;
    const local = span === 0 ? 0 : (t - previous.progress) / span;
    return { x: previous.position.x + (next.position.x - previous.position.x) * local,
      y: previous.position.y + (next.position.y - previous.position.y) * local };
  }
  return {
    x: trace.origin.x + (trace.contact.position.x - trace.origin.x) * t,
    y: trace.origin.y + (trace.contact.position.y - trace.origin.y) * t,
  };
}

/** @deprecated Compatibility alias; consumers should use the resolver-authored airborne path function. */
export const interpolatedContactPoint = interpolatedAirbornePoint;

/**
 * Presentation-only trace sequencing. Every endpoint comes from the resolver; this only chooses when each
 * authoritative phase is shown. Rules recovery is kept distinct from ground roll.
 */
export function tracePlaybackFrame(trace: ShotTrace, progress: number): TracePlaybackFrame {
  const t = Math.max(0, Math.min(1, progress));
  if (t >= 1) return { phase: "final", point: trace.finalPoint, finalVisible: true };
  if (trace.roll) {
    if (t < ROLL_AIRBORNE_END) {
      return {
        phase: "airborne",
        point: interpolatedAirbornePoint(trace, t / ROLL_AIRBORNE_END),
        finalVisible: false,
      };
    }
    if (t < ROLL_CONTACT_END) {
      return { phase: "contact", point: trace.contact.position, finalVisible: false };
    }
    const local = (t - ROLL_CONTACT_END) / (1 - ROLL_CONTACT_END);
    return {
      phase: "roll",
      point: {
        x: trace.roll.from.x + (trace.roll.to.x - trace.roll.from.x) * local,
        y: trace.roll.from.y + (trace.roll.to.y - trace.roll.from.y) * local,
      },
      finalVisible: false,
    };
  }

  const postContact = trace.transition;
  if (!postContact || t < POST_CONTACT_START) {
    return {
      phase: "airborne",
      point: interpolatedAirbornePoint(trace, postContact ? t / POST_CONTACT_START : t),
      finalVisible: false,
    };
  }
  const local = (t - POST_CONTACT_START) / (1 - POST_CONTACT_START);
  const from = postContact.from;
  const to = postContact.to;
  return {
    phase: trace.roll ? "roll" : "transition",
    point: { x: from.x + (to.x - from.x) * local, y: from.y + (to.y - from.y) * local },
    finalVisible: false,
  };
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
