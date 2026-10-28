import type { CanonicalPlayingGeometry, CanonicalRenderModel } from "./canonical-geometry";
import type { Point } from "./hole-geometry";
import { composeGlimpse, neighborRoute, pathFromPoints, premiumPropertyPlan, routeSamples,
  type PropertyGlimpse, type PropertyPath, type PropertyPlan } from "./premium-parkland-property";
import sourceHoles from "./premium-heathland-neighbor-geometry.json";
import { premiumHeathlandCorridor } from "./premium-heathland-corridor";
import { premiumHeathlandNeighborCorridor } from "./premium-heathland-neighbor-corridor";
import { seededRandom } from "./premium-parkland-decoration";

const blockedSurfaces = new Set(["TEE_BOX", "FIRST_CUT", "FAIRWAY", "FRINGE", "GREEN",
  "BUNKER", "WASTE_AREA", "WATER"]);

function pointInPolygon(point: Point, polygon: readonly Point[]): boolean {
  let inside = false;
  for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
    const a = polygon[i], b = polygon[j];
    if ((a.y > point.y) !== (b.y > point.y)
      && point.x < (b.x - a.x) * (point.y - a.y) / (b.y - a.y) + a.x) inside = !inside;
  }
  return inside;
}

function distanceToSegment(p: Point, a: Point, b: Point): number {
  const dx = b.x - a.x, dy = b.y - a.y;
  const length = dx * dx + dy * dy;
  const t = length ? Math.max(0, Math.min(1, ((p.x - a.x) * dx + (p.y - a.y) * dy) / length)) : 0;
  return Math.hypot(p.x - a.x - t * dx, p.y - a.y - t * dy);
}

function cross(a: Point, b: Point, c: Point): number {
  return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
}

function segmentsIntersect(a: Point, b: Point, c: Point, d: Point): boolean {
  return cross(a, b, c) * cross(a, b, d) <= 0 && cross(c, d, a) * cross(c, d, b) <= 0;
}

function segmentDistance(a: Point, b: Point, c: Point, d: Point): number {
  return segmentsIntersect(a, b, c, d) ? 0 : Math.min(distanceToSegment(a, c, d),
    distanceToSegment(b, c, d), distanceToSegment(c, a, b), distanceToSegment(d, a, b));
}

function segmentClear(a: Point, b: Point, polygon: readonly Point[], clearance: number): boolean {
  if (pointInPolygon(a, polygon) || pointInPolygon(b, polygon)) return false;
  for (let i = 0; i < polygon.length; i += 1) {
    if (segmentDistance(a, b, polygon[i], polygon[(i + 1) % polygon.length]) < clearance) return false;
  }
  return true;
}

export function heathlandRouteBlockers(geometry: CanonicalPlayingGeometry, model: CanonicalRenderModel,
  glimpses: readonly PropertyGlimpse[], neighborOuters: readonly (readonly Point[])[] = []): readonly (readonly Point[])[] {
  return [geometry.playableBoundary.map(model.project),
    ...geometry.regions.filter((r) => blockedSurfaces.has(r.surface)).map((r) => r.boundary.map(model.project)),
    ...glimpses.flatMap((g) => g.regions.filter((r) => blockedSurfaces.has(r.surface)).map((r) => r.points)),
    ...neighborOuters]
    .filter((polygon) => polygon.length >= 3);
}

/** Checks path stroke clearance against every actual projected active and neighbouring polygon edge. */
export function isHeathlandRouteClear(path: PropertyPath, blockers: readonly (readonly Point[])[],
  stage: { left: number; right: number; height: number }, extra = 2, minVisible = 16): boolean {
  // Fallback access tracks are literal line segments; source-planner routes are cubics.
  const samples = path.d.includes("C") ? routeSamples(path) : path.points;
  const clearance = path.width / 2 + extra;
  let visible = 0;
  for (let i = 0; i + 1 < samples.length; i += 1) {
    const a = samples[i], b = samples[i + 1];
    if (Math.max(a.y, b.y) < 0 || Math.min(a.y, b.y) > stage.height
      || Math.max(a.x, b.x) < stage.left || Math.min(a.x, b.x) > stage.right) continue;
    visible += 1;
    if (a.x < stage.left + 3 || a.x > stage.right - 3
      || b.x < stage.left + 3 || b.x > stage.right - 3) return false;
    if (blockers.some((polygon) => !segmentClear(a, b, polygon, clearance))) return false;
  }
  return visible >= minVisible;
}

function boundaryEdgeAtY(boundary: readonly Point[], y: number, side: -1 | 1): number | null {
  const crossings = boundary.flatMap((point, index) => {
    const next = boundary[(index + 1) % boundary.length];
    return (point.y <= y && next.y > y) || (next.y <= y && point.y > y)
      ? [point.x + (y - point.y) * (next.x - point.x) / (next.y - point.y)] : [];
  });
  return crossings.length ? side < 0 ? Math.min(...crossings) : Math.max(...crossings) : null;
}

function outsideRoute(boundary: readonly Point[], side: -1 | 1, offset: number,
  height: number): PropertyPath | null {
  const ys = boundary.map((p) => p.y);
  const minY = Math.min(...ys), maxY = Math.max(...ys);
  const points = Array.from({ length: 65 }, (_, index) => {
    const y = height + 14 - (height + 28) * index / 64;
    const edge = boundaryEdgeAtY(boundary, Math.max(minY + .01, Math.min(maxY - .01, y)), side);
    return edge === null ? null : { x: edge + side * offset, y };
  });
  if (points.some((p) => p === null)) return null;
  const placed = points as Point[];
  return { d: placed.map((p, i) => `${i ? "L" : "M"}${p.x.toFixed(1)} ${p.y.toFixed(1)}`).join(" "),
    points: placed, width: 2.8, side };
}

export type HeathlandPropertyAudit = Readonly<{ attempted: number; rejectedVisibility: number;
  rejectedOverlap: number; rejectedRounding: number; rejectedRoute: number; rejectedJunction: number }>;
export type HeathlandPropertyPlan = PropertyPlan & Readonly<{ audit: HeathlandPropertyAudit }>;

function pathSamples(path: PropertyPath): readonly Point[] {
  return path.d.includes("C") ? routeSamples(path) : path.points;
}

function bounds(points: readonly Point[]) {
  return { minX: Math.min(...points.map((p) => p.x)), maxX: Math.max(...points.map((p) => p.x)),
    minY: Math.min(...points.map((p) => p.y)), maxY: Math.max(...points.map((p) => p.y)) };
}

function overlaps(first: readonly Point[], second: readonly Point[], clearance: number): boolean {
  const a = bounds(first), b = bounds(second);
  if (a.maxX + clearance < b.minX || b.maxX + clearance < a.minX
    || a.maxY + clearance < b.minY || b.maxY + clearance < a.minY) return false;
  return first.some((p) => pointInPolygon(p, second)) || second.some((p) => pointInPolygon(p, first))
    || first.some((p, i) => second.some((q, j) =>
      segmentDistance(p, first[(i + 1) % first.length], q, second[(j + 1) % second.length]) < clearance));
}

/** Parkland's on-stage network-junction search, checked against Heathland's full polygon and stroke clearance. */
function connectingJunction(active: PropertyPath, neighbor: PropertyPath, side: -1 | 1,
  blockers: readonly (readonly Point[])[], stage: { left: number; right: number; height: number }): PropertyPath | null {
  const activeSamples = pathSamples(active).filter((_, i) => i % 3 === 0);
  const neighborSamples = pathSamples(neighbor).filter((_, i) => i % 3 === 0);
  const pairs = activeSamples.flatMap((a) => neighborSamples.map((b) => ({ a, b,
    distance: Math.hypot(a.x - b.x, a.y - b.y) })))
    .filter(({ a, b, distance }) => distance > 8 && distance < 155
      && Math.abs(a.y - b.y) < 85
      && a.y > 12 && a.y < stage.height - 12 && b.y > 12 && b.y < stage.height - 12
      && a.x > stage.left + 8 && a.x < stage.right - 8
      && b.x > stage.left + 8 && b.x < stage.right - 8)
    .sort((a, b) => a.distance - b.distance).slice(0, 48);
  for (const { a, b } of pairs) {
    const bendY = Math.min(a.y, b.y) - Math.min(10, Math.abs(a.x - b.x) * .09);
    const junction = pathFromPoints([a,
      { x: a.x * .68 + b.x * .32, y: bendY },
      { x: a.x * .32 + b.x * .68, y: bendY }, b], 2.3, side, "junction");
    if (isHeathlandRouteClear(junction, blockers, stage, 2.2, 8)) return junction;
  }
  return null;
}

/** Select only neighbour placements with a rounded complex and a clear physical connection. */
export function premiumHeathlandPropertyPlan(geometry: CanonicalPlayingGeometry, model: CanonicalRenderModel,
  layoutSeed: string, par: number, holeNumber: number, left: number, width: number, height: number): HeathlandPropertyPlan {
  const base = premiumPropertyPlan(geometry, model, layoutSeed, par, holeNumber, left, width,
    height, [], 0, 220, sourceHoles, true);
  const stage = { left, right: left + width, height };
  const activeOuter = premiumHeathlandCorridor(geometry, model, height)?.bands[0].boundary;
  const activeBlockers = [...heathlandRouteBlockers(geometry, model, []), ...(activeOuter ? [activeOuter] : [])];
  const sourceMain = base.paths.find((path) => !path.kind && isHeathlandRouteClear(path, activeBlockers, stage));
  const boundary = geometry.playableBoundary.map(model.project);
  const candidates = ([-1, 1] as const).flatMap((side) => [8, 13, 19, 27, 35, 45, 55, 68, 84]
    .map((offset) => outsideRoute(boundary, side, offset, height))
    .filter((route): route is PropertyPath => route !== null)
    .filter((route) => isHeathlandRouteClear(route, activeBlockers, stage)));
  const main = sourceMain ?? candidates.sort((a, b) => Math.abs(a.points[32].x - 110)
    - Math.abs(b.points[32].x - 110))[0];
  const paths: PropertyPath[] = main ? [main] : [];
  const glimpses: PropertyGlimpse[] = [];
  const outers: (readonly Point[])[] = [];
  const audit = { attempted: 0, rejectedVisibility: 0, rejectedOverlap: 0,
    rejectedRounding: 0, rejectedRoute: 0, rejectedJunction: 0 };
  if (!main || width < 540) return { glimpses, paths, building: null, audit };

  const random = seededRandom(`${layoutSeed}:heathland-connected-neighbors`);
  const cap = par === 3 ? 1 : 2;
  const sideOrder: readonly (-1 | 1)[] = [main.side, -main.side as -1 | 1];
  const sourceOrder = par === 3 ? [sourceHoles[0], sourceHoles[1], sourceHoles[2]]
    : [sourceHoles[1], sourceHoles[2], sourceHoles[0]];
  for (let slot = 0; slot < cap; slot += 1) {
    let accepted = false;
    for (const side of sideOrder) {
      if (accepted) break;
      const room = side < 0 ? -left : stage.right - 220;
      if (room < 145) continue;
      const firstBounds = glimpses.length ? bounds(glimpses[0].regions.flatMap((r) => r.points)) : null;
      const oppositeBand = firstBounds && (firstBounds.minY + firstBounds.maxY) / 2 < height / 2
        ? "lower" : "upper";
      for (let attempt = 0; attempt < 24; attempt += 1) {
        audit.attempted += 1;
        const source = sourceOrder[(attempt + slot) % sourceOrder.length];
        const mode = attempt < 8 ? "reverse" : attempt < 16 ? "return-diagonal" : "original";
        const glimpse = composeGlimpse(source, side, model, geometry.tee, left, stage.right,
          height, attempt % 8, random, mode, slot && side === glimpses[0]?.side ? oppositeBand : undefined, true);
        if (!glimpse) { audit.rejectedVisibility += 1; continue; }
        const corridor = premiumHeathlandNeighborCorridor(glimpse);
        if (!corridor) { audit.rejectedRounding += 1; continue; }
        if (overlaps(corridor.outerBoundary, activeOuter ?? boundary, 6)
          || outers.some((outer) => overlaps(corridor.outerBoundary, outer, 8))) {
          audit.rejectedOverlap += 1; continue;
        }
        const candidateBlockers = [...heathlandRouteBlockers(geometry, model, [...glimpses, glimpse],
          [...outers, corridor.outerBoundary]), ...(activeOuter ? [activeOuter] : [])];
        if (!isHeathlandRouteClear(main, candidateBlockers, stage)
          || paths.some((path) => !isHeathlandRouteClear(path, candidateBlockers, stage, 2, path.kind === "junction" ? 8 : 16))) {
          audit.rejectedOverlap += 1; continue;
        }
        const legalRoutes = ([-1, 1] as const).flatMap((routeSide) => [16, 23, 30, 38, 48, 60]
          .map((offset) => neighborRoute(glimpse, routeSide, offset))
          .filter((route): route is PropertyPath => route !== null)
          .filter((route) => isHeathlandRouteClear(route, candidateBlockers, stage)));
        if (!legalRoutes.length) { audit.rejectedRoute += 1; continue; }
        const connection = legalRoutes
          .map((route) => ({ route, junction: connectingJunction(main, route, side, candidateBlockers, stage) }))
          .find((pair) => pair.junction !== null);
        if (!connection?.junction) {
          audit.rejectedJunction += 1; continue;
        }
        glimpses.push(glimpse);
        outers.push(corridor.outerBoundary);
        paths.push(connection.route, connection.junction);
        accepted = true;
        break;
      }
    }
  }
  return { glimpses, paths, building: null, audit };
}

