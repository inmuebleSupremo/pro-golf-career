import type { CanonicalPlayingGeometry, CanonicalRenderModel } from "./canonical-geometry";
import type { Point } from "./hole-geometry";
import { premiumHeathlandCorridor, type HeathlandCorridor } from "./premium-heathland-corridor";
import type { PropertyGlimpse } from "./premium-parkland-property";

export type HeathlandNeighborCorridor = Readonly<{
  bands: HeathlandCorridor["bands"];
  outerBoundary: readonly Point[];
  transform: string;
  fadeFrom: Point;
  fadeTo: Point;
}>;

/** Use the active Heathland cap in the neighbour's tee-to-green frame, then rotate it back to the property. */
export function premiumHeathlandNeighborCorridor(glimpse: PropertyGlimpse): HeathlandNeighborCorridor | null {
  const dx = glimpse.cup.x - glimpse.tee.x, dy = glimpse.cup.y - glimpse.tee.y;
  const length = Math.hypot(dx, dy);
  if (length < 65) return null;
  const forward = { x: dx / length, y: dy / length };
  const across = { x: forward.y, y: -forward.x };
  const margin = 34;
  const local = (point: Point): Point => {
    const x = point.x - glimpse.tee.x, y = point.y - glimpse.tee.y;
    return { x: x * across.x + y * across.y,
      y: length + margin - (x * forward.x + y * forward.y) };
  };
  const world = (point: Point): Point => ({
    x: glimpse.cup.x + across.x * point.x - forward.x * (point.y - margin),
    y: glimpse.cup.y + across.y * point.x - forward.y * (point.y - margin),
  });
  const firstCut = glimpse.regions.filter((region) => region.surface === "FIRST_CUT")
    .sort((a, b) => b.points.length - a.points.length)[0];
  if (!firstCut) return null;
  const geometry: CanonicalPlayingGeometry = {
    tee: local(glimpse.tee), cup: local(glimpse.cup),
    playableBoundary: firstCut.points.map(local),
    regions: glimpse.regions.map((region) => ({ surface: region.surface, boundary: region.points.map(local) })),
  };
  const model: CanonicalRenderModel = {
    project: (point) => point, unproject: (point) => point, playableBoundaryPath: "", terrain: [],
  };
  const corridor = premiumHeathlandCorridor(geometry, model, length + 2 * margin);
  if (!corridor) return null;
  return {
    bands: corridor.bands,
    outerBoundary: corridor.bands[0].boundary.map(world),
    transform: `matrix(${across.x} ${across.y} ${-forward.x} ${-forward.y} `
      + `${glimpse.cup.x + forward.x * margin} ${glimpse.cup.y + forward.y * margin})`,
    fadeFrom: world({ x: 0, y: corridor.greenBounds.maxY + 12 }),
    fadeTo: world({ x: 0, y: corridor.greenBounds.maxY + 67 }),
  };
}

