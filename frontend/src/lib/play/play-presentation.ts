import { resolveBiome } from "./biomes";
import type { HoleGeom } from "./hole-geometry";

export type PlayPresentation = "polished" | "premium-v1" | "premium";

/** V2 is enabled only for live Generator 8 Parkland geometry; other saves retain Current. */
export function defaultPlayPresentation(hole: HoleGeom | null): "polished" | "premium" {
  return hole?.canonicalBiome === "PARKLAND"
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
