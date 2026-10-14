import type { CanonicalPlayingGeometry } from "@/lib/play/canonical-geometry";
import type { Point } from "@/lib/play/hole-geometry";

export interface CanopyPlacement {
  readonly x: number;
  readonly y: number;
  /** Presentation-only scale in the SVG's projected coordinate system. */
  readonly scale: number;
  readonly variant: 0 | 1;
}

export interface TreeCanopyPlan {
  readonly regionIndex: number;
  readonly placements: readonly CanopyPlacement[];
}

/**
 * Produces stable, bounded deciduous-canopy placements from actual canonical tree regions only. The caller
 * projects the points and clips the symbols again, so decoration never creates a new obstacle footprint.
 */
export function planTreeCanopies(
  geometry: CanonicalPlayingGeometry,
  layoutSeed: string,
): readonly TreeCanopyPlan[] {
  return geometry.regions.flatMap((region, regionIndex) => {
    if (region.surface !== "TREES" || region.boundary.length < 3) return [];
    const polygon = region.boundary;
    const bounds = polygonBounds(polygon);
    const target = Math.max(1, Math.min(4, Math.round(Math.abs(polygonArea(polygon)) / 430)));
    const random = mulberry32(hash(`${layoutSeed}:trees:${regionIndex}:${polygonFingerprint(polygon)}`));
    const placements: CanopyPlacement[] = [];

    for (let attempt = 0; attempt < target * 40 && placements.length < target; attempt += 1) {
      const x = bounds.minX + random() * (bounds.maxX - bounds.minX);
      const y = bounds.minY + random() * (bounds.maxY - bounds.minY);
      const radius = 4 + random() * 2.5;
      if (!pointInPolygon({ x, y }, polygon)) continue;
      if (placements.some((prior) => Math.hypot(prior.x - x, prior.y - y) < radius * 1.5)) continue;
      placements.push({ x, y, scale: 0.15 + random() * 0.06, variant: random() < 0.5 ? 0 : 1 });
    }
    return [{ regionIndex, placements }];
  });
}

/** Inclusive enough for cosmetic placement; final SVG clipping is the visual safety boundary. */
export function pointInPolygon(point: Point, polygon: readonly Point[]): boolean {
  let inside = false;
  for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i, i += 1) {
    const a = polygon[i];
    const b = polygon[j];
    const intersects = ((a.y > point.y) !== (b.y > point.y))
      && point.x < ((b.x - a.x) * (point.y - a.y)) / (b.y - a.y) + a.x;
    if (intersects) inside = !inside;
  }
  return inside;
}

export function decorationId(layoutSeed: string, geometry: CanonicalPlayingGeometry): string {
  const fingerprint = geometry.regions.map((region) => `${region.surface}:${polygonFingerprint(region.boundary)}`).join("|");
  return `hole-art-${hash(`${layoutSeed}:${fingerprint}`).toString(36)}`;
}

function polygonBounds(polygon: readonly Point[]) {
  return polygon.reduce((bounds, point) => ({
    minX: Math.min(bounds.minX, point.x),
    maxX: Math.max(bounds.maxX, point.x),
    minY: Math.min(bounds.minY, point.y),
    maxY: Math.max(bounds.maxY, point.y),
  }), { minX: Infinity, maxX: -Infinity, minY: Infinity, maxY: -Infinity });
}

function polygonArea(polygon: readonly Point[]): number {
  return polygon.reduce((area, point, index) => {
    const next = polygon[(index + 1) % polygon.length];
    return area + point.x * next.y - next.x * point.y;
  }, 0) / 2;
}

function polygonFingerprint(polygon: readonly Point[]): string {
  return polygon.map((point) => `${point.x.toFixed(2)},${point.y.toFixed(2)}`).join(";");
}

function hash(value: string): number {
  let result = 2166136261;
  for (let i = 0; i < value.length; i += 1) {
    result ^= value.charCodeAt(i);
    result = Math.imul(result, 16777619);
  }
  return result >>> 0;
}

function mulberry32(seed: number): () => number {
  let state = seed;
  return () => {
    state += 0x6d2b79f5;
    let value = state;
    value = Math.imul(value ^ (value >>> 15), value | 1);
    value ^= value + Math.imul(value ^ (value >>> 7), value | 61);
    return ((value ^ (value >>> 14)) >>> 0) / 4294967296;
  };
}
