import type { Point } from "@/lib/play/hole-geometry";

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
