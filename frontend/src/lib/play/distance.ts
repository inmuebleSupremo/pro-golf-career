import type { Point } from "@/lib/play/hole-geometry";
import type { ShotSelection, Situation } from "@/components/play/play-shared";

/** All server shot distances are yards. Keep one display convention across the dock and result. */
export function formatShotDistance(yards: number, putting = false): string {
  if (!Number.isFinite(yards)) return "—";
  if (!putting && yards >= 10) return `${Math.round(yards)} yd`;
  const feet = yards * 3;
  if (feet < 0.1 && feet >= 0) return "<0.1 ft";
  return `${feet < 10 ? feet.toFixed(1) : Math.round(feet)} ft`;
}

/** The comparison shown in the dock; reach comes from the current server club projection. */
export function shotDistanceReadout(
  situation: Situation,
  origin: Point,
  target: Point,
  selection: ShotSelection,
): { pin: string; target: string; reach: string } {
  const reach = situation.guidance?.clubs.find((candidate) => candidate.club === selection.club)?.normalReach;
  return {
    pin: formatShotDistance(situation.distanceToPin),
    target: formatShotDistance(Math.hypot(target.x - origin.x, target.y - origin.y)),
    reach: reach != null && Number.isFinite(reach) && reach >= 0 ? formatShotDistance(reach) : "—",
  };
}

