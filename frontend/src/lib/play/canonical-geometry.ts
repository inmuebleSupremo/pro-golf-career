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
  readonly playableBoundaryPath: string;
  readonly terrain: readonly { readonly surface: string; readonly path: string }[];
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

  return {
    project,
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
