import { resolveBiome } from "./biomes";
import type { HoleGeom } from "./hole-geometry";

export type PlayPresentation = "polished" | "premium-v1" | "premium";

export function hasPrincipledGeometryProvenance(hole: HoleGeom | null): boolean {
  return hole?.courseGeneratorVersion === 8
    && hole.canonicalGeometrySource === "GENERATOR_8_PRINCIPLED_ARCHITECTURE"
    || hole?.courseGeneratorVersion === 9
    && hole.canonicalGeometrySource === "GENERATOR_9_COASTAL_LINKS";
}

/** Persisted family provenance keeps old Woodland (Heathland) distinct from new Woodland (Parkland). */
export function isParklandFamily(hole: HoleGeom | null): boolean {
  return hole?.courseFamily === "PARKLAND"
    || (!hole?.courseFamily && (hole?.canonicalBiome === "PARKLAND"
      || (!hole?.canonicalBiome && resolveBiome(hole?.courseType) === "parkland")));
}

export function isWoodlandVariation(hole: HoleGeom | null): boolean {
  return isParklandFamily(hole) && hole?.canonicalBiome === "WOODLAND";
}

/** Premium art is enabled only for supported live Generator 8 geometry; other saves retain Current. */
export function defaultPlayPresentation(hole: HoleGeom | null): "polished" | "premium" {
  return hole
    && ((isParklandFamily(hole)
      && (hole.courseFamily === "PARKLAND" || hole.canonicalBiome === "PARKLAND")
      && resolveBiome(hole.courseType) === "parkland")
      || (hole.courseFamily === "LINKS" && resolveBiome(hole.courseType) === "links")
      || (hole.courseFamily === "HEATHLAND" && hole.canonicalBiome === "HEATHLAND"
        && resolveBiome(hole.courseType) === "heathland"
        && hole.geometry?.regions.some((region) => region.surface === "GREEN" && region.boundary.length >= 3))
      || (hole.courseFamily === "DESERT" && hole.canonicalBiome === "DESERT"
        && resolveBiome(hole.courseType) === "desert"
        && hole.geometry?.regions.some((region) => region.surface === "GREEN" && region.boundary.length >= 3)))
    && hasPrincipledGeometryProvenance(hole)
    && !!hole.geometry
    && hole.geometry.playableBoundary.length >= 3
    ? "premium" : "polished";
}

/** The existing development comparison may override the normal selection. */
export function playPresentation(hole: HoleGeom | null, comparison: PlayPresentation | null,
  showComparison: boolean): PlayPresentation {
  return showComparison && comparison ? comparison : defaultPlayPresentation(hole);
}
