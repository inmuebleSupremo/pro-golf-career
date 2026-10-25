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
export type PropertyPath = Readonly<{ d: string; width: number }>;
export type PropertyPlan = Readonly<{
  glimpses: readonly PropertyGlimpse[];
  paths: readonly PropertyPath[];
  building: Readonly<{ x: number; y: number; scale: number }> | null;
}>;

/** Scenic SVG space is never a valid aim surface, even when it depicts turf. */
export function isPremiumAimCoordinate(x: number, y: number, height: number): boolean {
  return x >= 0 && x <= 220 && y >= 0 && y <= height;
}

function distanceToSegment(point: Point, start: Point, end: Point): number {
  const dx = end.x - start.x;
  const dy = end.y - start.y;
  const lengthSquared = dx * dx + dy * dy;
  const t = lengthSquared === 0 ? 0 : Math.max(0, Math.min(1,
    ((point.x - start.x) * dx + (point.y - start.y) * dy) / lengthSquared));
  return Math.hypot(point.x - start.x - t * dx, point.y - start.y - t * dy);
}

/** Only newly generated outer crowns are excluded from the neighbouring maintained corridor. */
export function inPropertyClearing(x: number, y: number, treeSize: number, glimpse: PropertyGlimpse): boolean {
  const point = { x, y };
  return glimpse.clearing.some((polygon) => pointInPolygon(point, polygon)
    || polygon.some((vertex, index) => distanceToSegment(point, vertex, polygon[(index + 1) % polygon.length])
      < treeSize * 1.3 + 3));
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
  const pathSide: -1 | 1 = glimpses.length > 0 ? (glimpses[0].side === -1 ? 1 : -1)
    : room[preferred[0] < 0 ? "left" : "right"] > 95 ? preferred[0] : preferred[1];
  if (room[pathSide < 0 ? "left" : "right"] > 95) {
    const desiredX = pathSide < 0 ? -34 - Math.min(room.left * .3, 75) : 254 + Math.min(room.right * .3, 75);
    const sweep = pathSide * (22 + random() * 27);
    const pathWidth = 2.8 + random() * 1.5;
    const clearance = Math.abs(sweep) + pathWidth / 2 + 4;
    const x = pathSide < 0 ? Math.min(desiredX, coreLeft - clearance)
      : Math.max(desiredX, coreRight + clearance);
    paths.push({ d: `M${x - sweep * .35} ${height + 12} C${x + sweep} ${height * .75} ${x - sweep} ${height * .44} ${x + sweep * .35} ${-12}`,
      width: pathWidth });
  }

  const buildingSide: -1 | 1 = room.left > room.right ? -1 : 1;
  const building = (holeNumber === 9 || holeNumber === 18)
    && room[buildingSide < 0 ? "left" : "right"] > 180 && random() > .38
    ? { x: buildingSide < 0 ? left + 88 : right - 88,
      y: height * (.13 + random() * .17), scale: .65 + random() * .25 } : null;

  return { glimpses, paths, building };
}
