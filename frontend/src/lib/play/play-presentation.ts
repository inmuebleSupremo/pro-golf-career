import { resolveBiome } from "./biomes";
import type { HoleGeom } from "./hole-geometry";

export type PlayPresentation = "polished" | "premium-v1" | "premium";

/** Persisted family provenance keeps old Woodland (Heathland) distinct from new Woodland (Parkland). */
export function isParklandFamily(hole: HoleGeom | null): boolean {
  return hole?.courseFamily === "PARKLAND"
    || (!hole?.courseFamily && (hole?.canonicalBiome === "PARKLAND"
      || (!hole?.canonicalBiome && resolveBiome(hole?.courseType) === "parkland")));
}

export function isWoodlandVariation(hole: HoleGeom | null): boolean {
  return isParklandFamily(hole) && hole?.canonicalBiome === "WOODLAND";
}

/** V2 is enabled only for live Generator 8 Parkland geometry; other saves retain Current. */
export function defaultPlayPresentation(hole: HoleGeom | null): "polished" | "premium" {
  return isParklandFamily(hole)
    && (hole?.courseFamily === "PARKLAND" || hole?.canonicalBiome === "PARKLAND")
    && resolveBiome(hole.courseType) === "parkland"
    && hole.courseGeneratorVersion === 8
    && hole.canonicalGeometrySource === "GENERATOR_8_PRINCIPLED_ARCHITECTURE"
    && !!hole.geometry
    && hole.geometry.playableBoundary.length >= 3
    ? "premium" : "polished";
}

/** The existing development comparison may override the normal selection. */
export function playPresentation(hole: HoleGeom | null, comparison: PlayPresentation | null,
  showComparison: boolean): PlayPresentation {
  return showComparison && comparison ? comparison : defaultPlayPresentation(hole);
}
