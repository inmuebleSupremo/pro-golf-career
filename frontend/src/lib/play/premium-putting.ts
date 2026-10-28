import type { HoleGeom, Point } from "./hole-geometry";
import { isParklandFamily } from "./play-presentation";
import type { PhysicalPuttingProjector } from "./physical-putting";

export type PuttingFamily = "parkland" | "links" | "heathland" | "desert";

/** A presentation choice only. The physical-putting route and solver remain unchanged. */
export function premiumPuttingFamily(hole: HoleGeom, presentation: string): PuttingFamily | null {
  if (presentation !== "premium" || hole.puttingPolicy !== "PHYSICAL_V1" || !hole.geometry || !hole.physicalGreen) return null;
  const green = hole.geometry.regions.find((region) => region.surface === "GREEN")?.boundary;
  const fringe = hole.geometry.regions.find((region) => region.surface === "FRINGE")?.boundary;
  if (!validPolygon(green) || !validPolygon(fringe) || !validPoint(hole.geometry.cup) || !validPoint(hole.ball?.position)) return null;
  if (isParklandFamily(hole) && (hole.courseType === "PARKLAND" || hole.courseType === "WOODLAND")) return "parkland";
  if (hole.courseFamily === "LINKS" && hole.courseType === "LINKS") return "links";
  if (hole.courseFamily === "HEATHLAND" && hole.canonicalBiome === "HEATHLAND") return "heathland";
  if (hole.courseFamily === "DESERT" && hole.canonicalBiome === "DESERT") return "desert";
  return null;
}

export function validPoint(point: Point | undefined): point is Point {
  return !!point && Number.isFinite(point.x) && Number.isFinite(point.y);
}

export function validPolygon(points: readonly Point[] | undefined): points is readonly Point[] {
  if (!points || points.length < 3 || !points.every(validPoint)) return false;
  return Math.abs(points.reduce((area, point, i) => {
    const next = points[(i + 1) % points.length];
    return area + point.x * next.y - next.x * point.y;
  }, 0)) > 0.01;
}

/** Approved Catmull–Rom construction, with restrained tangents around canonical boundary vertices. */
export function puttingPolygonPath(points: readonly Point[], projector: PhysicalPuttingProjector): string {
  const screen = points.map(projector.project);
  const fmt = (n: number) => n.toFixed(2);
  return `M${fmt(screen[0].x)} ${fmt(screen[0].y)} ` + screen.map((point, i) => {
    const before = screen[(i + screen.length - 1) % screen.length];
    const next = screen[(i + 1) % screen.length];
    const after = screen[(i + 2) % screen.length];
    const c1 = { x: point.x + (next.x - before.x) / 10, y: point.y + (next.y - before.y) / 10 };
    const c2 = { x: next.x - (after.x - point.x) / 10, y: next.y - (after.y - point.y) / 10 };
    return `C${fmt(c1.x)} ${fmt(c1.y)} ${fmt(c2.x)} ${fmt(c2.y)} ${fmt(next.x)} ${fmt(next.y)}`;
  }).join(" ") + " Z";
}

export function pointInPolygon(point: Point, polygon: readonly Point[]): boolean {
  let inside = false;
  for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
    const a = polygon[i], b = polygon[j];
    if ((a.y > point.y) !== (b.y > point.y) && point.x < (b.x - a.x) * (point.y - a.y) / (b.y - a.y) + a.x) inside = !inside;
  }
  return inside;
}

export function distanceToPolygon(point: Point, polygon: readonly Point[]): number {
  if (pointInPolygon(point, polygon)) return 0;
  return Math.min(...polygon.map((a, i) => {
    const b = polygon[(i + 1) % polygon.length];
    const dx = b.x - a.x, dy = b.y - a.y;
    const t = Math.max(0, Math.min(1, ((point.x - a.x) * dx + (point.y - a.y) * dy) / (dx * dx + dy * dy || 1)));
    return Math.hypot(point.x - a.x - t * dx, point.y - a.y - t * dy);
  }));
}

/** One deterministic plan per green/camera. Objects are excluded from physical surfaces. */
export function puttingDecoration(hole: HoleGeom, projector: PhysicalPuttingProjector, family: PuttingFamily) {
  const regions = hole.geometry!.regions;
  const protectedAreas = regions.filter((r) => ["GREEN", "FRINGE", "BUNKER", "WATER", "FAIRWAY"].includes(r.surface) && validPolygon(r.boundary));
  const treeAreas = regions.filter((r) => r.surface === "TREES" && validPolygon(r.boundary));
  const playable = hole.geometry!.playableBoundary;
  let state = [...`${hole.layoutSeed}:${hole.holeNumber}:${family}`].reduce((hash, char) => Math.imul(hash ^ char.charCodeAt(0), 16777619), 2166136261) >>> 0;
  const random = () => ((state = (Math.imul(state, 1664525) + 1013904223) >>> 0) / 4294967296);
  const objects: { x: number; y: number; radius: number; kind: "tree" | "heather" | "gorse" | "fescue" | "rock" | "shrub" | "cactus" }[] = [];
  const count = family === "parkland" ? hole.canonicalBiome === "WOODLAND" ? 155 : 130 : family === "heathland" ? 105 : 95;
  for (let i = 0; i < count * 8 && objects.length < count; i++) {
    const x = random() * projector.width, y = random() * projector.height;
    const world = projector.unproject({ x, y });
    const radius = family === "parkland" ? 5 + random() * 8 : 2 + random() * 6;
    const worldRadius = radius / projector.pixelsPerYard;
    if (protectedAreas.some((r) => distanceToPolygon(world, r.boundary) < worldRadius + 0.7)) continue;
    const inTrees = treeAreas.some((r) => pointInPolygon(world, r.boundary));
    const onPlayableGround = pointInPolygon(world, playable);
    if (objects.some((other) => Math.hypot(other.x - x, other.y - y) < (other.radius + radius) * .75)) continue;
    const choice = random();
    const woodlandEdge = x < 70 || x > projector.width - 70 || y < 40 || y > projector.height - 40;
    const kind = family === "parkland" ? (!onPlayableGround || inTrees || woodlandEdge && distanceToPolygon(world, regions.find((r) => r.surface === "FRINGE")!.boundary) > worldRadius + 3 ? "tree" : "shrub")
      : family === "links" ? (choice < .84 ? "fescue" : "rock")
      : family === "heathland" ? (choice < .56 ? "heather" : choice < .82 ? "gorse" : choice < .89 && (!onPlayableGround || inTrees) ? "tree" : "rock")
      : (choice < .52 ? "shrub" : choice < .70 ? "cactus" : "rock");
    objects.push({ x, y, radius, kind });
  }
  return objects;
}
