import type { CanonicalPlayingGeometry, CanonicalRenderModel } from "./canonical-geometry";
import type { Point } from "./hole-geometry";

type Band = Readonly<{ path: string; boundary: readonly Point[]; shoulderWidth: number; capTop: number }>;
export type DesertCorridor = Readonly<{ bands: readonly Band[]; green: readonly Point[]; greenBounds: Bounds }>;
type Bounds = Readonly<{ minX: number; maxX: number; minY: number; maxY: number }>;

function bounds(points: readonly Point[]): Bounds {
  return { minX: Math.min(...points.map((p) => p.x)), maxX: Math.max(...points.map((p) => p.x)),
    minY: Math.min(...points.map((p) => p.y)), maxY: Math.max(...points.map((p) => p.y)) };
}

function intervalsAtY(points: readonly Point[], y: number): readonly [number, number][] {
  const crossings = points.flatMap((p, i) => {
    const q = points[(i + 1) % points.length];
    return (p.y <= y && q.y > y) || (q.y <= y && p.y > y)
      ? [p.x + (y - p.y) * (q.x - p.x) / (q.y - p.y)] : [];
  }).sort((a, b) => a - b);
  const intervals: [number, number][] = [];
  for (let i = 0; i + 1 < crossings.length; i += 2) intervals.push([crossings[i], crossings[i + 1]]);
  return intervals;
}

function sampleRegion(regions: readonly (readonly Point[])[], y: number, guide: number): { x: number; half: number } | null {
  const intervals = regions.flatMap((points) => intervalsAtY(points, y));
  if (!intervals.length) return null;
  const chosen = intervals.reduce((best, interval) =>
    Math.abs((interval[0] + interval[1]) / 2 - guide)
      < Math.abs((best[0] + best[1]) / 2 - guide) ? interval : best);
  return { x: (chosen[0] + chosen[1]) / 2, half: (chosen[1] - chosen[0]) / 2 };
}

function interpolate(values: readonly number[], t: number): number {
  const i = Math.min(values.length - 2, Math.floor(t));
  const u = Math.max(0, Math.min(1, t - i));
  const p0 = values[Math.max(0, i - 1)], p1 = values[i];
  const p2 = values[i + 1], p3 = values[Math.min(values.length - 1, i + 2)];
  return .5 * ((2 * p1) + (-p0 + p2) * u + (2 * p0 - 5 * p1 + 4 * p2 - p3) * u * u
    + (-p0 + 3 * p1 - 3 * p2 + p3) * u * u * u);
}

const f = (value: number) => value.toFixed(1);
function cubic(a: Point, b: Point, c: Point, d: Point, t: number): Point {
  const u = 1 - t;
  return { x: u ** 3 * a.x + 3 * u ** 2 * t * b.x + 3 * u * t ** 2 * c.x + t ** 3 * d.x,
    y: u ** 3 * a.y + 3 * u ** 2 * t * b.y + 3 * u * t ** 2 * c.y + t ** 3 * d.y };
}

/** Port of create_desert_svg.py's capped_ribbon: normal-offset sides share a smooth cubic rear closure. */
function cappedRibbon(center: (y: number) => number, width: (y: number) => number,
  bottomY: number, shoulderY: number, capTop: number, noseX: number): Band {
  const left: Point[] = [], right: Point[] = [];
  for (let i = 0; i < 72; i += 1) {
    const y = bottomY + (shoulderY - bottomY) * i / 71;
    const x = center(y);
    // The source centre spline is gentle. Generated doglegs can have sharper sampling transitions;
    // bound the tangent so normal-offset edges cannot fold back into sharp triangular spikes.
    const dx = Math.max(-.35, Math.min(.35, (center(y + 1) - center(y - 1)) / 2));
    const normal = Math.hypot(1, dx);
    const half = width(y);
    // Keep each cross-section monotone in screen Y. The source's full normal Y offset
    // can fold over when generated hole widths and dogleg tangents change sharply.
    left.push({ x: x - half / normal, y });
    right.push({ x: x + half / normal, y });
  }
  const l = left.at(-1)!, r = right.at(-1)!;
  const shoulderWidth = width(shoulderY);
  const rise = Math.max(12, shoulderY - capTop);
  const nose = { x: noseX, y: capTop };
  const firstCap = [l, { x: l.x - 2, y: shoulderY - rise * .6 },
    { x: noseX - shoulderWidth * .86, y: capTop }, nose] as const;
  const secondCap = [nose, { x: noseX + shoulderWidth * .86, y: capTop },
    { x: r.x + 2, y: shoulderY - rise * .6 }, r] as const;
  const d = [`M${f(left[0].x)} ${f(left[0].y)}`,
    ...left.slice(1).map((p) => `L${f(p.x)} ${f(p.y)}`),
    `C${f(l.x - 2)} ${f(shoulderY - rise * .6)} ${f(noseX - shoulderWidth * .86)} ${f(capTop)} ${f(noseX)} ${f(capTop)}`,
    `C${f(noseX + shoulderWidth * .86)} ${f(capTop)} ${f(r.x + 2)} ${f(shoulderY - rise * .6)} ${f(r.x)} ${f(r.y)}`,
    ...right.slice(0, -1).reverse().map((p) => `L${f(p.x)} ${f(p.y)}`), "Z"].join(" ");
  const capPoints = [firstCap, secondCap].flatMap((control) => Array.from({ length: 17 }, (_, i) =>
    cubic(control[0], control[1], control[2], control[3], (i + 1) / 17)));
  return { path: d, boundary: [...left, ...capPoints, ...right.slice(0, -1).reverse()], shoulderWidth, capTop };
}

/** Visual envelopes follow backend corridor widths. The authoritative fairway, green and hazards draw above them. */
export function premiumDesertCorridor(geometry: CanonicalPlayingGeometry, model: CanonicalRenderModel,
  height: number): DesertCorridor | null {
  const greenRegion = geometry.regions.find((r) => r.surface === "GREEN" && r.boundary.length >= 3);
  if (!greenRegion) return null;
  const green = greenRegion.boundary.map(model.project);
  const greenBounds = bounds(green);
  const tee = model.project(geometry.tee), cup = model.project(geometry.cup);
  const shoulderY = Math.max(greenBounds.minY + 5, Math.min(greenBounds.maxY - 4, cup.y));
  const bottomY = height + 18;
  if (bottomY - shoulderY < 65) return null;
  const fairways = geometry.regions.filter((r) => r.surface === "FAIRWAY").map((r) => r.boundary.map(model.project));
  const firstCuts = geometry.regions.filter((r) => r.surface === "FIRST_CUT").map((r) => r.boundary.map(model.project));
  const boundary = geometry.playableBoundary.map(model.project);
  const anchors = Array.from({ length: 29 }, (_, index) => {
    const y = Math.min(tee.y, bottomY - 18) + (shoulderY - Math.min(tee.y, bottomY - 18)) * index / 28;
    const t = index / 28;
    const guide = tee.x * (1 - t) + cup.x * t;
    const fairway = sampleRegion(fairways, y, guide);
    const firstCut = sampleRegion(firstCuts, y, fairway?.x ?? guide);
    const playable = sampleRegion([boundary], y, fairway?.x ?? firstCut?.x ?? guide);
    const greenHalf = (greenBounds.maxX - greenBounds.minX) / 2;
    const fadePosition = Math.max(0, Math.min(1, (t - .45) / .55));
    const fade = fadePosition * fadePosition * (3 - 2 * fadePosition);
    const x = (fairway?.x ?? firstCut?.x ?? playable?.x ?? guide) * (1 - fade) + cup.x * fade;
    const fairHalf = (fairway?.half ?? firstCut?.half ?? greenHalf) * (1 - fade)
      + Math.max(8, greenHalf * .92) * fade;
    const greenFirst = Math.max(fairHalf + 3, greenHalf * 1.22);
    const firstHalf = Math.max(fairHalf + 3,
      (firstCut?.half ?? fairHalf + 4) * (1 - fade) + greenFirst * fade);
    const greenOuter = Math.max(firstHalf + 6, greenHalf * 1.55);
    const outerHalf = Math.max(firstHalf + 6,
      (playable?.half ?? firstHalf + 7) * (1 - fade) + greenOuter * fade);
    return { y, x, widths: [outerHalf + 12, outerHalf + 5, outerHalf,
      Math.min(outerHalf - 2, firstHalf + 2), Math.min(firstHalf, fairHalf + 2)] };
  });
  const at = (y: number, select: (anchor: typeof anchors[number]) => number): number => {
    const t = (y - anchors[0].y) / (anchors.at(-1)!.y - anchors[0].y) * (anchors.length - 1);
    return interpolate(anchors.map(select), Math.max(0, Math.min(anchors.length - 1, t)));
  };
  const center = (y: number) => at(y, (a) => a.x);
  const noseX = (greenBounds.minX + greenBounds.maxX) / 2;
  const top = Math.max(2, greenBounds.minY - Math.max(9,
    (greenBounds.maxY - greenBounds.minY) * .48));
  const bands = [0, 1, 2, 3, 4].map((index) => cappedRibbon(center,
    (y) => Math.max(3, at(y, (a) => a.widths[index])), bottomY, shoulderY,
    top + index * 2.8, noseX));
  return { bands, green, greenBounds };
}

