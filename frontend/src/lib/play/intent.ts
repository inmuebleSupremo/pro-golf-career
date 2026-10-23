import type { Point } from "@/lib/play/hole-geometry";

export type AimEnvelope = { minX: number; maxX: number; minY: number; maxY: number };
export type AimSelection = { revision: string; point: Point };
export type CourseAimState = { selection: AimSelection; usedCourseAim: boolean };

/** A manual target belongs to one server shot revision, never to the next stroke. */
export function currentAimPoint(selection: AimSelection, revision: string, initial: Point): Point {
  return selection.revision === revision ? selection.point : initial;
}

/** Mirrors the server's inclusive planning envelope before a target is shown or submitted. */
export function aimWithinEnvelope(point: Point, envelope?: AimEnvelope | null): boolean {
  return Number.isFinite(point.x) && Number.isFinite(point.y)
    && (!envelope || (point.x >= envelope.minX && point.x <= envelope.maxX
      && point.y >= envelope.minY && point.y <= envelope.maxY));
}

/** Only a new, legal course selection dismisses the introductory aiming hint. */
export function acceptCourseAim(
  state: CourseAimState,
  revision: string,
  current: Point,
  candidate: Point,
  envelope?: AimEnvelope | null,
): CourseAimState {
  if (!aimWithinEnvelope(candidate, envelope) || (candidate.x === current.x && candidate.y === current.y)) return state;
  return { selection: { revision, point: candidate }, usedCourseAim: true };
}

/** Builds the literal, server-owned ball-strike input after planning has been unlocked. */
export function buildBallStrikeIntent(
  club: string,
  aimPoint: Point,
  shotFamily: string,
  shotShape: string,
  expectedShotRevision: string,
) {
  return { club, aimPoint, shotFamily, shotShape, expectedShotRevision };
}
