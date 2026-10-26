import type { Point } from "./hole-geometry";
import { pointInPolygon } from "./canonical-hole-decoration";
import type { CanonicalPlayingGeometry, CanonicalRenderModel } from "./canonical-geometry";
import neighborGeometry from "./premium-parkland-neighbor-geometry.json";
import { seededRandom, type PremiumTree } from "./premium-parkland-decoration";

/** Rounded yard-space surface polygons from checked-in Parkland preview fixtures (seeds 103 and 887). */
type SourceHole = Readonly<{
  source: string; par: number; tee: readonly number[]; cup: readonly number[];
  regions: readonly Readonly<{ surface: string; points: readonly (readonly number[])[] }>[];
}>;
const sourceHoles: readonly SourceHole[] = neighborGeometry;

export type PropertyRegion = Readonly<{ surface: string; path: string; points: readonly Point[] }>;
export type PropertyGlimpse = Readonly<{
  source: string; sourcePar: number; side: -1 | 1;
  orientation: "reverse" | "diagonal"; angle: number; pixelsPerYard: number;
  tee: Point; cup: Point; regions: readonly PropertyRegion[];
  clearing: readonly (readonly Point[])[];
}>;
export type PropertyPath = Readonly<{ d: string; width: number; side: -1 | 1; points: readonly Point[] }>;
export type PropertyBuilding = Readonly<{
  x: number; y: number; width: number; depth: number; side: -1 | 1;
  kind: "clubhouse" | "halfway";
}>;
export type PropertyPlan = Readonly<{
  glimpses: readonly PropertyGlimpse[];
  paths: readonly PropertyPath[];
  building: PropertyBuilding | null;
}>;

/** Scenic SVG space is never a valid aim surface, even when it depicts turf. */
export function isPremiumAimCoordinate(x: number, y: number, height: number): boolean {
  return x >= 0 && x <= 220 && y >= 0 && y <= height;
}

/** The cosmetic cart track itself never becomes an aiming surface. */
export function isPropertyPathCoordinate(x: number, y: number, paths: readonly PropertyPath[]): boolean {
  const point = { x, y };
  return paths.some((path) => {
    const samples = routeSamples(path);
    return samples.some((start, index) => index < samples.length - 1
      && distanceToSegment(point, start, samples[index + 1]) <= path.width / 2 + 1);
  });
}

function distanceToSegment(point: Point, start: Point, end: Point): number {
  const dx = end.x - start.x;
  const dy = end.y - start.y;
  const lengthSquared = dx * dx + dy * dy;
  const t = lengthSquared === 0 ? 0 : Math.max(0, Math.min(1,
    ((point.x - start.x) * dx + (point.y - start.y) * dy) / lengthSquared));
  return Math.hypot(point.x - start.x - t * dx, point.y - start.y - t * dy);
}

function cubicPoint(points: readonly Point[], t: number): Point {
  const u = 1 - t;
  return {
    x: u ** 3 * points[0].x + 3 * u ** 2 * t * points[1].x
      + 3 * u * t ** 2 * points[2].x + t ** 3 * points[3].x,
    y: u ** 3 * points[0].y + 3 * u ** 2 * t * points[1].y
      + 3 * u * t ** 2 * points[2].y + t ** 3 * points[3].y,
  };
}

function cubicControls(points: readonly Point[], index: number): readonly Point[] {
  const previous = points[Math.max(0, index - 1)];
  const start = points[index];
  const end = points[index + 1];
  const next = points[Math.min(points.length - 1, index + 2)];
  return [start,
    { x: start.x + (end.x - previous.x) / 6, y: start.y + (end.y - previous.y) / 6 },
    { x: end.x - (next.x - start.x) / 6, y: end.y - (next.y - start.y) / 6 }, end];
}

export function routeSamples(path: PropertyPath): Point[] {
  return path.points.slice(0, -1).flatMap((_, index) => {
    const controls = cubicControls(path.points, index);
    return Array.from({ length: 8 }, (_, step) => cubicPoint(controls, step / 8));
  }).concat(path.points[path.points.length - 1]);
}

function pathFromPoints(points: readonly Point[], width: number, side: -1 | 1): PropertyPath {
  const d = [`M${points[0].x.toFixed(1)} ${points[0].y.toFixed(1)}`];
  for (let index = 0; index < points.length - 1; index += 1) {
    const [, first, second, end] = cubicControls(points, index);
    d.push(`C${first.x.toFixed(1)} ${first.y.toFixed(1)} ${second.x.toFixed(1)} ${second.y.toFixed(1)} ${end.x.toFixed(1)} ${end.y.toFixed(1)}`);
  }
  return { points, width, side, d: d.join(" ") };
}

function distanceToPolygon(point: Point, polygon: readonly Point[]): number {
  if (pointInPolygon(point, polygon)) return 0;
  return Math.min(...polygon.map((vertex, index) =>
    distanceToSegment(point, vertex, polygon[(index + 1) % polygon.length])));
}

function maintainedConflict(point: Point, glimpses: readonly PropertyGlimpse[], clearance: number): boolean {
  return glimpses.some((glimpse) => glimpse.regions.some((region) =>
    distanceToPolygon(point, region.points) < clearance));
}

function protectedTreeConflict(point: Point, trees: readonly PremiumTree[], clearance: number): boolean {
  return trees.some((tree) => Math.hypot(point.x - tree.x, point.y - tree.y)
    < tree.size * .85 + clearance);
}

/** Keep complete crowns off the played surfaces, while letting woodland reach the first-cut edge. */
export function inPropertyClearing(x: number, y: number, treeSize: number, glimpse: PropertyGlimpse): boolean {
  const point = { x, y };
  return glimpse.regions.some((region) => {
    if (!["FIRST_CUT", "FAIRWAY", "FRINGE", "GREEN", "BUNKER"].includes(region.surface)) return false;
    const polygon = region.points;
    if (pointInPolygon(point, polygon)) return true;
    const crownClearance = region.surface === "FIRST_CUT" ? treeSize * .55 + 1
      : treeSize * 1.7 + 2;
    return polygon.some((vertex, index) => distanceToSegment(point, vertex, polygon[(index + 1) % polygon.length])
      < crownClearance);
  });
}

function bounds(points: readonly Point[]) {
  return {
    minX: Math.min(...points.map((point) => point.x)), maxX: Math.max(...points.map((point) => point.x)),
    minY: Math.min(...points.map((point) => point.y)), maxY: Math.max(...points.map((point) => point.y)),
  };
}

function polygonPath(points: readonly Point[]): string {
  return points.map((point, index) => `${index === 0 ? "M" : "L"}${point.x.toFixed(1)} ${point.y.toFixed(1)}`).join(" ") + " Z";
}

function conflictsWithProtectedScenery(glimpse: PropertyGlimpse, trees: readonly PremiumTree[],
  coreLeft: number, coreRight: number, activeBoundary: readonly Point[]): boolean {
  const maintained = glimpse.regions.map((region) => region.points);
  const points = maintained.flat();
  // The original scenic region owns its grassland, tree crowns and shadows.
  if (glimpse.side < 0 ? Math.max(...points.map((point) => point.x)) > coreLeft - 16
    : Math.min(...points.map((point) => point.x)) < coreRight + 16) return true;
  if (maintained.some((polygon) => polygon.some((point) => pointInPolygon(point, activeBoundary)))) return true;
  return trees.some((tree) => maintained.some((polygon) => pointInPolygon(tree, polygon)
    || polygon.some((point, index) => distanceToSegment(tree, point, polygon[(index + 1) % polygon.length])
      < tree.size * 1.3 + 4)));
}

/** Generated-hole polygons projected at the active hole's pixels per yard, then rotated and translated. */
function composeGlimpse(source: SourceHole, side: -1 | 1, model: CanonicalRenderModel, activeTee: Point,
  left: number, right: number, height: number, attempt: number, random: () => number): PropertyGlimpse | null {
  const orientation = random() < .35 ? "reverse" : "diagonal";
  const angle = orientation === "reverse" ? 165 + random() * 30
    : (random() < .5 ? -1 : 1) * (27 + random() * 13);
  const radians = angle * Math.PI / 180;
  const cosine = Math.cos(radians);
  const sine = Math.sin(radians);
  const projectedTee = model.project(activeTee);
  const pixelsPerYard = Math.abs(model.project({ x: activeTee.x + 1, y: activeTee.y }).x - projectedTee.x);
  const centerX = (source.tee[0] + source.cup[0]) / 2;
  const centerY = (source.tee[1] + source.cup[1]) / 2;
  const rotate = (point: readonly number[]): Point => {
    const x = (point[0] - centerX) * pixelsPerYard;
    const y = -(point[1] - centerY) * pixelsPerYard;
    return { x: x * cosine - y * sine, y: x * sine + y * cosine };
  };
  const localRegions = source.regions.map((region) => ({ surface: region.surface,
    points: region.points.map(rotate) }));
  const localBounds = bounds(localRegions.flatMap((region) => region.points));
  const gap = 24 + random() * 24 + attempt * 24;
  const offsetX = side < 0 ? -gap - localBounds.maxX : 220 + gap - localBounds.minX;
  const localTee = rotate(source.tee);
  const edgeCrop = 25 + random() * 55;
  const offsetY = orientation === "reverse" ? -localTee.y - edgeCrop : height - localTee.y + edgeCrop;
  const place = (point: Point): Point => ({ x: point.x + offsetX, y: point.y + offsetY });
  const order = (surface: string) => surface === "FIRST_CUT" ? 0 : surface === "FAIRWAY" ? 1
    : surface === "FRINGE" ? 2 : surface === "GREEN" ? 3 : 4;
  const regions = localRegions.map((region) => {
    const points = region.points.map(place);
    return { surface: region.surface, points, path: polygonPath(points) };
  }).sort((a, b) => order(a.surface) - order(b.surface));
  const placedBounds = bounds(regions.flatMap((region) => region.points));
  if (Math.min(placedBounds.maxX, right) - Math.max(placedBounds.minX, left) < 65
    || Math.min(placedBounds.maxY, height) - Math.max(placedBounds.minY, 0) < 90) return null;
  return {
    source: source.source, sourcePar: source.par, side, orientation, angle, pixelsPerYard,
    tee: place(rotate(source.tee)), cup: place(rotate(source.cup)), regions,
    clearing: regions.filter((region) => ["FIRST_CUT", "FAIRWAY", "FRINGE", "GREEN", "BUNKER"].includes(region.surface))
      .map((region) => region.points),
  };
}

const routeBlockers = new Set(["TEE_BOX", "FAIRWAY", "FIRST_CUT", "FRINGE", "GREEN",
  "BUNKER", "WASTE_AREA", "WATER"]);

function corridorEdgeAtY(regions: readonly PropertyRegion[], y: number, guideX: number, side: -1 | 1): number | null {
  const intervals = regions.filter((region) => ["FIRST_CUT", "FAIRWAY", "FRINGE", "GREEN", "TEE_BOX"]
    .includes(region.surface)).flatMap((region) => {
    const crossings = region.points.flatMap((point, index) => {
      const next = region.points[(index + 1) % region.points.length];
      return (point.y <= y && next.y > y) || (next.y <= y && point.y > y)
        ? [point.x + (y - point.y) * (next.x - point.x) / (next.y - point.y)] : [];
    }).sort((a, b) => a - b);
    const pairs: { left: number; right: number }[] = [];
    for (let index = 0; index + 1 < crossings.length; index += 2) {
      pairs.push({ left: crossings[index], right: crossings[index + 1] });
    }
    return pairs;
  }).filter((interval) => Math.abs((interval.left + interval.right) / 2 - guideX) < 85);
  if (intervals.length === 0) return null;
  return side < 0 ? Math.min(...intervals.map((interval) => interval.left))
    : Math.max(...intervals.map((interval) => interval.right));
}

function activeRoute(geometry: CanonicalPlayingGeometry, model: CanonicalRenderModel,
  side: -1 | 1, offset: number, height: number): PropertyPath | null {
  const tee = model.project(geometry.tee);
  const cup = model.project(geometry.cup);
  if (tee.y - cup.y < 95) return null;
  const regions = geometry.regions.filter((region) => routeBlockers.has(region.surface))
    .map((region) => ({ surface: region.surface, points: region.boundary.map(model.project), path: "" }));
  if (!regions.some((region) => region.surface === "FAIRWAY")) return null;
  const anchors = Array.from({ length: 9 }, (_, index) => {
    const t = index / 8;
    const y = tee.y + (cup.y - tee.y) * t;
    const guideX = tee.x + (cup.x - tee.x) * t;
    const edge = corridorEdgeAtY(regions, y, guideX, side);
    return { x: (edge ?? guideX) + side * offset, y };
  });
  if (anchors.some((point, index) => index > 0 && Math.abs(point.x - anchors[index - 1].x) > 55)) return null;
  const first = anchors[0], second = anchors[1], last = anchors[anchors.length - 1];
  const beforeLast = anchors[anchors.length - 2];
  const points = [
    { x: first.x + (first.x - second.x) * .28, y: Math.max(height + 12, tee.y + 22) },
    ...anchors,
    { x: last.x + (last.x - beforeLast.x) * .28, y: Math.min(-12, cup.y - 22) },
  ];
  return pathFromPoints(points, 2.9, side);
}

function routeQuality(path: PropertyPath, geometry: CanonicalPlayingGeometry, model: CanonicalRenderModel,
  glimpses: readonly PropertyGlimpse[], trees: readonly PremiumTree[], left: number, right: number,
  height: number): number | null {
  const blockers = geometry.regions.filter((region) => routeBlockers.has(region.surface))
    .map((region) => region.boundary.map(model.project));
  const visible = routeSamples(path).filter((point) => point.y >= 0 && point.y <= height);
  if (visible.length < 24 || visible.some((point) => point.x < left + 6 || point.x > right - 6
    || blockers.some((polygon) => distanceToPolygon(point, polygon) < path.width / 2 + 2.5)
    || maintainedConflict(point, glimpses, path.width / 2 + 3))) return null;
  let hits = 0;
  let run = 0;
  let longestRun = 0;
  for (const point of visible) {
    if (protectedTreeConflict(point, trees, path.width / 2 + 1)) {
      hits += 1;
      run += 1;
      longestRun = Math.max(longestRun, run);
    } else run = 0;
  }
  if (hits > visible.length * .38 || longestRun > 13) return null;
  return hits * 2 + Math.abs(path.points[1].x - model.project(geometry.tee).x) * .05;
}

function clearBuilding(building: PropertyBuilding, side: -1 | 1, left: number, right: number,
  height: number, coreLeft: number, coreRight: number, glimpses: readonly PropertyGlimpse[],
  protectedTrees: readonly PremiumTree[]): boolean {
  if (building.x - building.width / 2 < left + 20 || building.x + building.width / 2 > right - 20
    || building.y - building.depth / 2 < 20 || building.y + building.depth / 2 > height - 20
    || (side < 0 ? building.x + building.width / 2 + 15 >= coreLeft
      : building.x - building.width / 2 - 15 <= coreRight)) return false;
  const footprint = [-1, 0, 1].flatMap((dx) => [-1, 0, 1].map((dy) => ({
    x: building.x + dx * (building.width / 2 + 5),
    y: building.y + dy * (building.depth / 2 + 5),
  })));
  return footprint.every((point) => !maintainedConflict(point, glimpses, 8)
    && !protectedTreeConflict(point, protectedTrees, 5));
}


/** Decorative property cues in SVG space. Maintained regions stay outside the active 0..220 projection. */
export function premiumPropertyPlan(
  geometry: CanonicalPlayingGeometry,
  model: CanonicalRenderModel,
  layoutSeed: string,
  par: number,
  holeNumber: number,
  left: number,
  width: number,
  height: number,
  protectedTrees: readonly PremiumTree[] = [],
  coreLeft = -105,
  coreRight = 325,
): PropertyPlan {
  const random = seededRandom(`${layoutSeed}:property-v2`);
  const neighborRandom = seededRandom(`${layoutSeed}:authentic-neighbor`);
  const right = left + width;
  const room = { left: Math.max(0, -left), right: Math.max(0, right - 220) };
  const tee = model.project(geometry.tee);
  const cup = model.project(geometry.cup);
  const activeBoundary = geometry.playableBoundary.map(model.project);
  const turn = Math.sign(cup.x - tee.x) || (random() < .5 ? -1 : 1);
  const preferred: (-1 | 1)[] = turn > 0 ? [-1, 1] : [1, -1];
  if (random() < .35) preferred.reverse();

  // The provisional par caps remain 1/2/2; this visual pass deliberately shows at most one.
  const cap = par === 3 ? 1 : 2;
  const requested = room.left + room.right < 310 || random() < (par === 3 ? .5 : .22)
    ? 0 : 1;
  const glimpses: PropertyGlimpse[] = [];
  for (const side of preferred) {
    if (glimpses.length >= Math.min(cap, requested)) break;
    const available = side < 0 ? room.left : room.right;
    if (available < 145) continue;
    const source = par === 3 ? sourceHoles[0] : sourceHoles[1 + Math.floor(neighborRandom() * 2)];
    for (let attempt = 0; attempt < 7; attempt += 1) {
      const glimpse = composeGlimpse(source, side, model, geometry.tee, left, right, height, attempt, neighborRandom);
      if (!glimpse || conflictsWithProtectedScenery(glimpse, protectedTrees, coreLeft, coreRight, activeBoundary)) continue;
      glimpses.push(glimpse);
      break;
    }
  }

  const paths: PropertyPath[] = [];
  if (width > 550) {
    const candidates = ([-1, 1] as const).flatMap((side) => [7, 10, 14, 18, 23, 29]
      .map((offset) => activeRoute(geometry, model, side, offset, height))
      .filter((route): route is PropertyPath => route !== null)
      .map((route) => ({ route, quality: routeQuality(route, geometry, model,
        glimpses, protectedTrees, left, right, height) }))
      .filter((candidate): candidate is { route: PropertyPath; quality: number } => candidate.quality !== null));
    candidates.sort((a, b) => a.quality - b.quality || a.route.side - b.route.side);
    if (candidates[0]) paths.push(candidates[0].route);
  }

  // A building is optional; the old edge-to-building line is deliberately gone.
  // Keep the existing roof artwork available, but omit a building without a short connection to this route.
  let building: PropertyBuilding | null = null;
  const buildingKind = holeNumber === 9 ? "halfway" : holeNumber === 1 || holeNumber === 18 ? "clubhouse" : null;
  const route = paths[0];
  if (route && buildingKind && random() < .56 && room[route.side < 0 ? "left" : "right"] > 175) {
    const side = route.side;
    for (let attempt = 0; attempt < 12; attempt += 1) {
      const buildingWidth = buildingKind === "clubhouse" ? 23 : 16;
      const depth = buildingKind === "clubhouse" ? 15 : 11;
      const distance = 46 + random() * Math.min(86, room[side < 0 ? "left" : "right"] - 90);
      const candidate: PropertyBuilding = { kind: buildingKind, width: buildingWidth, depth, side: -side as -1 | 1,
        x: side < 0 ? coreLeft - distance : coreRight + distance,
        y: height * (.18 + random() * .28) };
      if (!clearBuilding(candidate, side, left, right, height, coreLeft, coreRight,
        glimpses, protectedTrees)) continue;
      const nearest = routeSamples(route).reduce((best, point) =>
        Math.hypot(point.x - candidate.x, point.y - candidate.y)
          < Math.hypot(best.x - candidate.x, best.y - candidate.y) ? point : best);
      if (Math.hypot(nearest.x - candidate.x, nearest.y - candidate.y) > 55) continue;
      const door = { x: candidate.x - side * (candidate.width / 2 + 1), y: candidate.y };
      const spur = pathFromPoints([door,
        { x: door.x - side * 10, y: door.y },
        { x: nearest.x + side * 8, y: nearest.y }, nearest], 2.5, side);
      const blockers = geometry.regions.filter((region) => routeBlockers.has(region.surface))
        .map((region) => region.boundary.map(model.project));
      if (routeSamples(spur).slice(0, -1).some((point) =>
        blockers.some((polygon) => distanceToPolygon(point, polygon) < spur.width / 2 + 2)
        || protectedTreeConflict(point, protectedTrees, spur.width / 2 + 1))) continue;
      building = candidate;
      paths.push(spur);
      break;
    }
  }

  return { glimpses, paths, building };
}
