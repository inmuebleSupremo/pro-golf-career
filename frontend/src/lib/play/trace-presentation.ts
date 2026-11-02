import type { Point, ResolvedShot } from "@/lib/play/hole-geometry";

export type ShotTrace = NonNullable<ResolvedShot["trace"]>;
export type TracePlaybackPhase = "airborne" | "contact" | "bounce" | "roll" | "transition" | "final";

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
const distance = (a: Point, b: Point) => Math.hypot(a.x - b.x, a.y - b.y);

type GroundPoint = NonNullable<ShotTrace["groundPath"]>[number];
type GroundSegment = { from: Point; to: Point; fromHeight: number; toHeight: number;
  kind: "rise" | "fall" | "roll"; durationMs: number };

function runningChip(trace: ShotTrace): boolean {
  const path = authoritativeGroundPath(trace);
  return path?.length === 1 && path[0].kind === "ROLL_STOP" && path[0].heightYards === 0
    && (trace.club.endsWith("_IRON") || trace.club.endsWith("_WEDGE"))
    && typeof trace.backspinRpm === "number" && trace.backspinRpm > 0 && trace.backspinRpm <= 2500
    && distance(trace.origin, trace.contact.position) > 0;
}

/** A complete event sequence is required before replacing historical straight-roll playback. */
export function authoritativeGroundPath(trace: ShotTrace): readonly GroundPoint[] | null {
  const path = trace.groundPath;
  if (trace.club === "PUTTER" || !trace.roll || trace.transition || !path?.length) return null;
  const last = path[path.length - 1];
  if (last.position.x !== trace.finalPoint.x || last.position.y !== trace.finalPoint.y
    || !["ROLL_STOP", "BOUNDARY_STOP"].includes(last.kind)) return null;
  let awaitingLanding = false;
  let landed = false;
  for (const [index, point] of path.entries()) {
    if (!Number.isFinite(point.position.x) || !Number.isFinite(point.position.y)
      || !Number.isFinite(point.heightYards) || point.heightYards < 0) return null;
    if (point.kind === "BOUNCE_APEX") {
      if (awaitingLanding || landed || point.heightYards === 0) return null;
      awaitingLanding = true;
    } else if (point.kind === "BOUNCE_LANDING") {
      if (!awaitingLanding || point.heightYards !== 0) return null;
      awaitingLanding = false;
      landed = true;
    } else if (point.kind === "ROLL_STOP" || point.kind === "BOUNDARY_STOP") {
      if (awaitingLanding || index !== path.length - 1 || point.heightYards !== 0) return null;
    } else return null;
  }
  return awaitingLanding ? null : path;
}

function groundSegments(trace: ShotTrace, flightMs: number): GroundSegment[] | null {
  const path = authoritativeGroundPath(trace);
  if (!path) return null;
  const carry = distance(trace.origin, trace.contact.position);
  const release = distance(trace.contact.position, trace.finalPoint);
  const running = clamp((release / Math.max(carry, 1) - 0.04) / 0.12, 0, 1)
    * clamp((45 - carry) / 30, 0, 1);
  const chip = runningChip(trace);
  const segments: GroundSegment[] = [];
  let from = trace.contact.position;
  let fromHeight = 0;
  for (const point of path) {
    const kind = point.kind === "BOUNCE_APEX" ? "rise"
      : point.kind === "BOUNCE_LANDING" ? "fall" : "roll";
    const bounceHeight = kind === "rise" ? point.heightYards : fromHeight;
    // A ballistic half-hop supplies a conservative clock; the server owns its height and endpoints.
    const bounceMs = clamp(Math.round(1000 * Math.sqrt(2 * bounceHeight * 0.9144 / 9.81)), 90, 320);
    const segmentDistance = distance(from, point.position);
    let durationMs = kind === "roll"
      ? segmentDistance > 0 ? clamp(Math.round((90 + 137 * Math.sqrt(segmentDistance)) * (1 - 0.33 * running)), 90, 850) : 0
      : bounceMs;
    if (chip && kind === "roll" && carry > 0) {
      // The ease-out begins at 2.5x average roll speed. Match it to at most
      // the displayed incoming flight speed, then let it decelerate naturally.
      durationMs = Math.max(durationMs, Math.ceil(2.5 * segmentDistance * flightMs / carry));
    }
    segments.push({ from, to: point.position, fromHeight, toHeight: point.heightYards, kind, durationMs });
    from = point.position;
    fromHeight = point.heightYards;
  }
  return segments;
}

/** Returned endpoints determine pacing; the prototype's four shots are references, not presets. */
export function tracePlaybackTiming(trace: ShotTrace | null | undefined): TracePlaybackTiming {
  if (!trace) return { flightMs: 650, contactMs: 0, rollMs: 0, transitionMs: 0, totalMs: 650 };
  // Legacy putting has no airborne samples. Keep it outside the non-putt motion change.
  if (trace.club === "PUTTER") return { flightMs: 650, contactMs: 0, rollMs: 0, transitionMs: 0, totalMs: 650 };

  const carry = distance(trace.contact.position, trace.origin);
  const release = trace.roll ? distance(trace.roll.to, trace.roll.from) : 0;
  const running = clamp((release / Math.max(carry, 1) - 0.04) / 0.12, 0, 1)
    * clamp((45 - carry) / 30, 0, 1);
  const flightMs = clamp(Math.round((540 + 84 * Math.sqrt(carry)) * (1 - 0.33 * running)), 400, 2200);
  const contactMs = runningChip(trace) ? 0 : clamp(Math.round(46 + 2.4 * Math.sqrt(carry)), 48, 90);
  const segments = groundSegments(trace, flightMs);
  const rollMs = segments
    ? segments.reduce((total, segment) => total + segment.durationMs, 0)
    : release > 0 ? clamp(Math.round((90 + 137 * Math.sqrt(release)) * (1 - 0.33 * running)), 90, 850) : 0;
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

/** Only the path already reached by the playback clock; its endpoint matches the airborne ball track. */
export function traversedAirbornePoints(trace: ShotTrace, progress: number): Point[] {
  if (trace.club === "PUTTER") return [];
  const timing = tracePlaybackTiming(trace);
  const elapsed = clamp(progress, 0, 1) * timing.totalMs;
  if (elapsed <= 0) return [];
  const flightProgress = clamp(elapsed / timing.flightMs, 0, 1);
  const prior = (trace.airbornePath ?? [])
    .filter((sample) => sample.progress > 0 && sample.progress < flightProgress)
    .map((sample) => sample.position);
  return [trace.origin, ...prior, airborneSample(trace, flightProgress).position];
}

/** Restrained screen-space depth only; it never changes the yard-space track or contact. */
export function traceHeightCue(heightYards: number): { lift: number; scale: number; shadowOpacity: number } {
  // Finite slope at launch avoids a screen-space jump as the first positive height sample arrives.
  const lift = 9 * (1 - Math.exp(-Math.max(0, heightYards) / 11));
  return { lift, scale: 1 + Math.min(0.16, lift * 0.018), shadowOpacity: Math.max(0.18, 0.42 - lift * 0.024) };
}

/** Small real bounce heights need a closer screen-space scale than full-shot apex heights. */
export function traceBounceCue(heightYards: number): { lift: number; scale: number; shadowOpacity: number } {
  const lift = clamp(Math.max(0, heightYards) * 6, 0, 6);
  return { lift, scale: 1 + Math.min(0.16, lift * 0.018), shadowOpacity: Math.max(0.18, 0.42 - lift * 0.024) };
}

/** Project the ground track once, then offset only the visible ball; aiming geometry stays unchanged. */
export function traceBallProjection(frame: TracePlaybackFrame, project: (point: Point) => Point) {
  const ground = project(frame.point);
  const cue = frame.phase === "bounce" ? traceBounceCue(frame.height ?? 0)
    : traceHeightCue(frame.phase === "airborne" ? frame.height ?? 0 : 0);
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
  const segments = groundSegments(trace, timing.flightMs);
  if (segments && timing.rollMs > 0) {
    let remaining = elapsed - timing.flightMs - timing.contactMs;
    for (const segment of segments) {
      if (remaining < segment.durationMs) {
        const local = clamp(remaining / segment.durationMs, 0, 1);
        const positionProgress = segment.kind === "roll" ? 1 - Math.pow(1 - local, 2.5) : local;
        const height = segment.kind === "rise"
          ? segment.toHeight * (2 * local - local * local)
          : segment.kind === "fall" ? segment.fromHeight * (1 - local * local) : 0;
        return { phase: segment.kind === "roll" ? "roll" : "bounce", point: {
          x: segment.from.x + (segment.to.x - segment.from.x) * positionProgress,
          y: segment.from.y + (segment.to.y - segment.from.y) * positionProgress,
        }, height, finalVisible: false };
      }
      remaining -= segment.durationMs;
    }
    return { phase: "roll", point: trace.finalPoint, height: 0, finalVisible: false };
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
  if (frame.phase === "bounce") return "Bouncing";
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
