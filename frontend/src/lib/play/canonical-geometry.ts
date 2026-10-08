import type { Point } from "@/lib/play/hole-geometry";

/** The complete gameplay-bearing geometry supplied by the PlayingHole API. */
export interface CanonicalPlayingGeometry {
  readonly tee: Point;
  readonly cup: Point;
  readonly playableBoundary: readonly Point[];
  readonly regions: readonly { readonly surface: string; readonly boundary: readonly Point[] }[];
}

export interface CanonicalRenderModel {
  readonly project: (point: Point) => Point;
  readonly unproject: (point: Point) => Point;
  readonly playableBoundaryPath: string;
  readonly terrain: readonly { readonly surface: string; readonly path: string }[];
}

/** Projects a canonical flow vector into a unit SVG direction; canonical +Y is screen-up. */
export function projectCanonicalFlow(
  origin: Point,
  flow: Point,
  project: (point: Point) => Point,
): Point {
  const from = project(origin);
  const toward = project({ x: origin.x + flow.x, y: origin.y + flow.y });
  const length = Math.hypot(toward.x - from.x, toward.y - from.y) || 1;
  return { x: (toward.x - from.x) / length, y: (toward.y - from.y) / length };
}

/** Builds the canonical-only terrain model used by the SVG renderer. No seed or local landform generation enters this path. */
export function canonicalRenderModel(
  geometry: CanonicalPlayingGeometry,
  viewport = { width: 220, height: 440, padding: 16 },
): CanonicalRenderModel {
  const points = [...geometry.playableBoundary, geometry.tee, geometry.cup];
  const xs = points.map((point) => point.x);
  const ys = points.map((point) => point.y);
  const minX = Math.min(...xs);
  const maxX = Math.max(...xs);
  const minY = Math.min(...ys);
  const maxY = Math.max(...ys);
  const scale = Math.min(
    (viewport.width - viewport.padding * 2) / Math.max(1, maxX - minX),
    (viewport.height - viewport.padding * 2) / Math.max(1, maxY - minY),
  );
  const project = (point: Point): Point => ({
    x: viewport.padding + (point.x - minX) * scale,
    y: viewport.height - viewport.padding - (point.y - minY) * scale,
  });
  const unproject = (point: Point): Point => ({
    x: minX + (point.x - viewport.padding) / scale,
    y: minY + (viewport.height - viewport.padding - point.y) / scale,
  });

  return {
    project,
    unproject,
    playableBoundaryPath: polygonPath(geometry.playableBoundary, project),
    terrain: geometry.regions.map((region) => ({ surface: region.surface, path: polygonPath(region.boundary, project) })),
  };
}

function polygonPath(points: readonly Point[], project: (point: Point) => Point): string {
  return points
    .map((point, index) => {
      const projected = project(point);
      return `${index === 0 ? "M" : "L"}${projected.x.toFixed(1)} ${projected.y.toFixed(1)}`;
    })
    .join(" ") + " Z";
}
