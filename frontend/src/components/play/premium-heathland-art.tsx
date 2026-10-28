import type { CanonicalPlayingGeometry, CanonicalRenderModel } from "@/lib/play/canonical-geometry";
import type { Point } from "@/lib/play/hole-geometry";
import type { HeathlandCorridor } from "@/lib/play/premium-heathland-corridor";
import { premiumHeathlandNeighborCorridor } from "@/lib/play/premium-heathland-neighbor-corridor";
import type { PropertyPlan } from "@/lib/play/premium-parkland-property";
import { routeSamples } from "@/lib/play/premium-parkland-property";
import { seededRandom } from "@/lib/play/premium-parkland-decoration";

const f = (n: number) => n.toFixed(1);

/** The source generator's gently broken quadratic contour, scaled by the live scene. */
function contour(x: number, y: number, rx: number, ry: number, seed: number, count = 14, wiggle = .19): string {
  const points = Array.from({ length: count }, (_, i) => {
    const t = Math.PI * 2 * i / count;
    const k = 1 + wiggle * Math.sin(3 * t + seed * .9)
      + wiggle * .55 * Math.cos(5 * t + seed * .67) + wiggle * .23 * Math.sin(8 * t + seed);
    return { x: x + rx * k * Math.cos(t), y: y + ry * k * Math.sin(t) };
  });
  const mid = (a: Point, b: Point) => `${f((a.x + b.x) / 2)} ${f((a.y + b.y) / 2)}`;
  return `M${mid(points.at(-1)!, points[0])} ${points.map((p, i) =>
    `Q${f(p.x)} ${f(p.y)} ${mid(p, points[(i + 1) % points.length])}`).join(" ")} Z`;
}

type Region = Readonly<{ surface: string; path: string }>;
type Protected = Readonly<{ points: readonly Point[]; minX: number; maxX: number; minY: number; maxY: number }>;
function protect(points: readonly Point[]): Protected {
  return { points, minX: Math.min(...points.map((p) => p.x)), maxX: Math.max(...points.map((p) => p.x)),
    minY: Math.min(...points.map((p) => p.y)), maxY: Math.max(...points.map((p) => p.y)) };
}
function pointInPolygon(p: Point, points: readonly Point[]): boolean {
  let inside = false;
  for (let i = 0, j = points.length - 1; i < points.length; j = i++) {
    const a = points[i], b = points[j];
    if ((a.y > p.y) !== (b.y > p.y) && p.x < (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x) inside = !inside;
  }
  return inside;
}
function distanceToSegment(p: Point, a: Point, b: Point): number {
  const dx = b.x - a.x, dy = b.y - a.y;
  const t = dx * dx + dy * dy ? Math.max(0, Math.min(1,
    ((p.x - a.x) * dx + (p.y - a.y) * dy) / (dx * dx + dy * dy))) : 0;
  return Math.hypot(p.x - a.x - t * dx, p.y - a.y - t * dy);
}

export type HeathlandDecoration = Readonly<{ kind: "heather" | "gorse" | "pine" | "canonical-pine" | "ground-patch" | "grass-stroke";
  x: number; y: number; radius: number; seed: number }>;

/** Full-shape clearance precedes construction; no painted-over vegetation or masking backstop. */
export function planHeathlandDecoration(geometry: CanonicalPlayingGeometry, model: CanonicalRenderModel,
  corridor: HeathlandCorridor | null, property: PropertyPlan | null, seed: string,
  left: number, width: number, height: number): readonly HeathlandDecoration[] {
  const blockedSurfaces = new Set(["TEE_BOX", "FIRST_CUT", "FAIRWAY", "FRINGE", "GREEN",
    "BUNKER", "WASTE_AREA", "WATER"]);
  const playable = geometry.regions.filter((r) => blockedSurfaces.has(r.surface))
    .map((r) => protect(r.boundary.map(model.project)));
  const neighbor = property?.glimpses.flatMap((g) => g.regions.filter((r) => blockedSurfaces.has(r.surface))
    .map((r) => protect(r.points))) ?? [];
  const neighborOuters = property?.glimpses.flatMap((g) => {
    const outer = premiumHeathlandNeighborCorridor(g)?.outerBoundary;
    return outer ? [protect(outer)] : [];
  }) ?? [];
  const blocked = [...playable, ...neighbor, ...neighborOuters,
    ...(corridor ? [protect(corridor.bands[0].boundary)] : [protect(geometry.playableBoundary.map(model.project))])];
  const routes = property?.paths.flatMap((path) => {
    const points = path.d.includes("C") ? routeSamples(path) : path.points;
    return points.slice(0, -1).map((a, i) => [a, points[i + 1], path.width / 2 + 2] as const);
  }) ?? [];
  const clear = (p: Point, radius: number, polygons: readonly Protected[] = blocked) =>
    polygons.every((region) => {
      if (p.x < region.minX - radius || p.x > region.maxX + radius
        || p.y < region.minY - radius || p.y > region.maxY + radius) return true;
      if (pointInPolygon(p, region.points)) return false;
      return region.points.every((a, i) => distanceToSegment(p, a, region.points[(i + 1) % region.points.length]) > radius);
    }) && routes.every(([a, b, half]) => distanceToSegment(p, a, b) > radius + half);
  const random = seededRandom(`${seed}:approved-heathland-vegetation`);
  const placed: HeathlandDecoration[] = [];
  const map = (x: number, y: number) => ({ x: left + x * width / 1600, y: y * height / 900 });
  const scale = Math.min(width / 1600, height / 900);
  // The approved source draws sandy clearings and tiny ground marks beneath its
  // fixed corridor. For generated corridors, reject their whole footprint first.
  for (let i = 0, count = 0; i < 90 && count < 6; i += 1) {
    const p = { x: left + random() * width, y: random() * height };
    const radius = (30 + random() * 52) * scale;
    if (clear(p, radius * 1.5 + 2)) {
      placed.push({ kind: "ground-patch", ...p, radius, seed: i + 5 });
      count += 1;
    }
  }
  for (let i = 0, count = 0; i < 600 && count < 180; i += 1) {
    const p = { x: left + random() * width, y: random() * height };
    const radius = (1.4 + random() * 1.6) * scale;
    if (clear(p, radius * 1.5 + 1)) {
      placed.push({ kind: "grass-stroke", ...p, radius, seed: i });
      count += 1;
    }
  }
  const sourceBanks = [
    [480, 145, 140, 70], [530, 315, 145, 115], [414, 570, 150, 137], [544, 805, 155, 100],
    [1075, 120, 126, 75], [1118, 304, 135, 142], [1120, 607, 180, 127], [1050, 820, 130, 90],
    [175, 735, 150, 120], [1440, 600, 150, 160], [1550, 220, 145, 125], [600, 58, 100, 48],
  ];
  for (const [bank, [cx, cy, rx, ry]] of sourceBanks.entries()) {
    for (let i = 0; i < Math.round(78 * Math.min(1, width / 700)); i += 1) {
      const theta = random() * Math.PI * 2, radial = Math.sqrt(random());
      const p = map(cx + rx * .87 * radial * Math.cos(theta), cy + ry * .82 * radial * Math.sin(theta));
      const radius = (5.5 + random() * 10.5) * scale;
      if (clear(p, radius * 1.45 + 2)) placed.push({ kind: "heather", ...p, radius, seed: bank * 109 + i * 11 });
    }
  }
  const gorseCentres = [[490, 230], [570, 435], [525, 600], [596, 822], [1100, 280],
    [1080, 610], [1108, 767], [1205, 110], [448, 735], [1428, 525]];
  for (const [bank, [cx, cy]] of gorseCentres.entries()) {
    for (let i = 0; i < 13; i += 1) {
      const p = map(cx + (random() * 110 - 55), cy + (random() * 100 - 50));
      const radius = (4 + random() * 7) * scale;
      if (clear(p, radius * 1.45 + 2)) placed.push({ kind: "gorse", ...p, radius, seed: bank * 29 + i });
    }
  }
  const stands = [[270, 148, 12], [328, 315, 16], [315, 595, 14], [380, 815, 13],
    [595, 130, 11], [650, 390, 14], [624, 680, 14], [1120, 220, 12], [1150, 535, 15],
    [1135, 820, 14], [1395, 250, 16], [1380, 690, 13], [710, 60, 11], [1063, 71, 11]];
  for (const [stand, [cx, cy, r]] of stands.entries()) {
    for (let i = 0; i < 5; i += 1) {
      const p = map(cx + random() * 106 - 53, cy + random() * 122 - 61);
      const radius = r * (.55 + random() * .75) * scale;
      if (clear(p, radius * 1.55 + 2)) placed.push({ kind: "pine", ...p, radius, seed: stand * 23 + i });
    }
  }
  for (let i = 0; i < 35; i += 1) {
    const p = { x: left + random() * width, y: random() * height };
    const radius = (5 + random() * 8) * scale;
    if (clear(p, radius * 1.55 + 3)) placed.push({ kind: "pine", ...p, radius, seed: i + 300 });
  }
  // Generator 8 TREES polygons are authoritative hazards; give them a distinct, sparse pine canopy.
  const treeRegions = geometry.regions.filter((r) => r.surface === "TREES" && r.boundary.length >= 3)
    .map((r) => protect(r.boundary.map(model.project)));
  for (const [index, region] of treeRegions.entries()) {
    for (let attempt = 0, count = 0; attempt < 18 && count < 3; attempt += 1) {
      const p = { x: region.minX + random() * (region.maxX - region.minX),
        y: region.minY + random() * (region.maxY - region.minY) };
      const radius = 2.5 + random() * 2;
      if (pointInPolygon(p, region.points) && clear(p, radius * 1.2, [...playable, ...neighbor])) {
        placed.push({ kind: "canonical-pine", ...p, radius, seed: index * 100 + attempt });
        count += 1;
      }
    }
  }
  return placed;
}

export function PremiumHeathlandDefs({ id }: { id: string }) {
  return <defs>
    <linearGradient id={`${id}-heath-ground`} x1="0" y1="0" x2=".85" y2="1"><stop stopColor="#4f6948" /><stop offset=".32" stopColor="#7b8259" /><stop offset=".57" stopColor="#98915b" /><stop offset=".79" stopColor="#728257" /><stop offset="1" stopColor="#4b6549" /></linearGradient>
    <linearGradient id={`${id}-heath-grassland`} x1="0" y1="0" x2="1" y2=".35"><stop stopColor="#657d51" /><stop offset=".45" stopColor="#89975f" /><stop offset="1" stopColor="#566e48" /></linearGradient>
    <linearGradient id={`${id}-heath-fairway`} x1="0" y1="0" x2="1" y2=".15"><stop stopColor="#7fa85a" /><stop offset=".27" stopColor="#a6ce74" /><stop offset=".58" stopColor="#b4db7d" /><stop offset=".78" stopColor="#97c668" /><stop offset="1" stopColor="#74a052" /></linearGradient>
    <linearGradient id={`${id}-heath-green`} x1="0" y1="0" x2="1" y2=".12"><stop stopColor="#83bd65" /><stop offset=".48" stopColor="#bce18b" /><stop offset="1" stopColor="#85b964" /></linearGradient>
    <linearGradient id={`${id}-heath-sand`} x1="0" y1="0" x2=".7" y2="1"><stop stopColor="#eee0a8" /><stop offset=".5" stopColor="#dac78f" /><stop offset="1" stopColor="#bca579" /></linearGradient>
    <linearGradient id={`${id}-heath-heather`} x1="0" y1="0" x2=".8" y2="1"><stop stopColor="#3f4639" /><stop offset=".25" stopColor="#625966" /><stop offset=".59" stopColor="#887184" /><stop offset="1" stopColor="#514e52" /></linearGradient>
    <linearGradient id={`${id}-heath-pine`} x1="0" y1="0" x2=".75" y2="1"><stop stopColor="#66845a" /><stop offset=".28" stopColor="#476c45" /><stop offset=".67" stopColor="#274d37" /><stop offset="1" stopColor="#152f2a" /></linearGradient>
    <linearGradient id={`${id}-heath-light`} x1="0" y1="0" x2="1" y2="0"><stop stopColor="#07100d" stopOpacity=".95" /><stop offset=".10" stopColor="#09140f" stopOpacity=".77" /><stop offset=".23" stopColor="#101913" stopOpacity=".46" /><stop offset=".36" stopColor="#132016" stopOpacity=".14" /><stop offset=".47" stopColor="#000" stopOpacity="0" /><stop offset=".60" stopColor="#000" stopOpacity="0" /><stop offset=".74" stopColor="#111c17" stopOpacity=".36" /><stop offset=".90" stopColor="#06120e" stopOpacity=".82" /><stop offset="1" stopColor="#030b0a" stopOpacity=".96" /></linearGradient>
    <linearGradient id={`${id}-heath-vertical`} x1="0" y1="0" x2="0" y2="1"><stop stopColor="#122518" stopOpacity=".21" /><stop offset=".17" stopColor="#000" stopOpacity="0" /><stop offset=".82" stopColor="#000" stopOpacity="0" /><stop offset="1" stopColor="#05100c" stopOpacity=".10" /></linearGradient>
    <pattern id={`${id}-heath-turf`} width="26" height="31" patternUnits="userSpaceOnUse"><path d="M-2 9q11-3 26 0M4 25q12-4 25-1" fill="none" stroke="#e8efad" strokeWidth=".8" opacity=".13" /></pattern>
    <pattern id={`${id}-heath-sand-grain`} width="12" height="10" patternUnits="userSpaceOnUse"><path d="M2 3l2-1m4 5 2-1" fill="none" stroke="#fff2cb" strokeWidth=".6" opacity=".44" /></pattern>
  </defs>;
}

function Pine({ x, y, r, seed, id, canonical = false }: { x: number; y: number; r: number; seed: number; id: string; canonical?: boolean }) {
  const lobes = Array.from({ length: 6 }, (_, i) => {
    const t = i * Math.PI / 3 + .26;
    return contour(x + Math.cos(t) * r * .48, y + Math.sin(t) * r * .42,
      r * (.27 + .035 * ((i + seed) % 3)), r * .23, seed + i + 9, 13, .25);
  });
  const needles = Array.from({ length: 12 }, (_, i) => {
    const t = (i * 2.399 + seed * .17) % (2 * Math.PI);
    const inner = r * (.18 + (i % 4) * .09);
    const px = x + Math.cos(t) * inner, py = y + Math.sin(t) * inner;
    return `M${f(px)} ${f(py)}l${f(Math.cos(t) * r * .17)} ${f(Math.sin(t) * r * .11)}`;
  }).join(" ");
  return <g data-heathland-canonical-tree={canonical || undefined}>
    <path d={contour(x + r * .4, y + r * .53, r * 1.02, r * .7, seed, 20, .18)} fill="#132a24" opacity=".39" />
    <path d={contour(x, y, r, r * .83, seed + 1, 23, .22)} fill="#193b31" opacity=".96" />
    <path d={contour(x - r * .12, y - r * .14, r * .81, r * .66, seed + 2, 21, .21)} fill={`url(#${id}-heath-pine)`} />
    {lobes.map((d, i) => <path key={i} d={d} fill={["#35563d", "#294f37", "#5a7747", "#345a3b", "#446a44", "#1b4533"][i]} opacity={i % 2 ? .58 : .72} />)}
    <path d={needles} fill="none" stroke="#7d995f" strokeWidth=".7" opacity=".3" />
  </g>;
}

function Heather({ x, y, r, seed, id }: { x: number; y: number; r: number; seed: number; id: string }) {
  const blossoms = Array.from({ length: 9 }, (_, i) => {
    const t = i * 2.4 + seed;
    const px = x + Math.cos(t) * r * .6, py = y + Math.sin(t) * r * .3;
    return contour(px, py, .45 + i % 3 * .12, .35, seed + i, 8, .16);
  }).join(" ");
  return <g>
    <path d={contour(x + 1, y + 1.5, r * 1.18, r * .76, seed, 14, .15)} fill="#23372e" opacity=".33" />
    <path d={contour(x, y, r, r * .67, seed + 2, 14, .19)} fill={`url(#${id}-heath-heather)`} opacity=".88" />
    <path d={contour(x - .5, y - .7, r * .78, r * .45, seed + 4, 14, .2)} fill={["#786378", "#826986", "#8b7191", "#8f748e", "#726171", "#9b8098", "#79697d"][seed % 7]} opacity=".79" />
    <path d={blossoms} fill="#bba0ae" opacity=".66" />
  </g>;
}

function Gorse({ x, y, r, seed }: { x: number; y: number; r: number; seed: number }) {
  const blossoms = Array.from({ length: 8 }, (_, i) => {
    const t = i * 2.4 + seed;
    return contour(x + Math.cos(t) * r * .65, y + Math.sin(t) * r * .34,
      .7 + i % 3 * .2, .55, seed + i * 7, 9, .17);
  }).join(" ");
  return <g>
    <path d={contour(x + 1, y + 1.5, r * 1.19, r * .75, seed, 15, .22)} fill="#2f3c29" opacity=".48" />
    <path d={contour(x, y, r * 1.06, r * .73, seed + 2, 15, .2)} fill="#43553a" opacity=".91" />
    {Array.from({ length: 5 }, (_, i) => <path key={i} d={contour(x + Math.cos(i * Math.PI * 2 / 5 + .32) * r * .48,
      y + Math.sin(i * Math.PI * 2 / 5 + .32) * r * .29, r * .31, r * .23, seed + i + 4, 11, .23)}
      fill={["#687547", "#879052", "#647a47", "#415a3c", "#7f8150"][i]} opacity=".72" />)}
    <path d={blossoms} fill="#d6be53" opacity=".7" />
  </g>;
}

export function PremiumHeathlandLandscape({ id, left, width, height, vegetation }: {
  id: string; left: number; width: number; height: number;
  vegetation: readonly HeathlandDecoration[];
}) {
  const patches = vegetation.filter((v) => v.kind === "ground-patch");
  // The source's FineStrokeBatch is retained as three compound paths.
  const strokes = ["#c5b883", "#354b39", "#ede1ac"].map((color, tint) => ({ color,
    d: vegetation.filter((v) => v.kind === "grass-stroke" && v.seed % 3 === tint)
      .map((v) => `M${f(v.x)} ${f(v.y)}l${f(v.radius * 1.3)} ${f(-v.radius)}`).join(" ") }));
  return <g pointerEvents="none" aria-hidden="true" data-heathland-vegetation={vegetation.length}>
    <rect x={left} width={width} height={height} fill={`url(#${id}-heath-ground)`} />
    {patches.map((p, i) => <g key={i}>
      <path d={contour(p.x, p.y, p.radius, p.radius * .65, p.seed, 40, .16)} fill="#8e8a5b" opacity=".36" />
      <path d={contour(p.x, p.y, p.radius * .88, p.radius * .57, p.seed + 7, 42, .13)} fill={`url(#${id}-heath-sand)`} opacity=".86" />
      <path d={contour(p.x - 2, p.y - 2, p.radius * .6, p.radius * .36, p.seed + 9, 40, .12)} fill="#efe0ae" opacity=".18" />
    </g>)}
    {strokes.map((s) => <path key={s.color} d={s.d} fill="none" stroke={s.color} strokeWidth=".7" opacity=".17" />)}
    {vegetation.filter((v) => v.kind === "heather").map((v, i) => <Heather key={`h${i}`} x={v.x} y={v.y} r={v.radius} seed={v.seed} id={id} />)}
    {vegetation.filter((v) => v.kind === "gorse").map((v, i) => <Gorse key={`g${i}`} x={v.x} y={v.y} r={v.radius} seed={v.seed} />)}
    {vegetation.filter((v) => v.kind === "pine").map((v, i) => <Pine key={`p${i}`} x={v.x} y={v.y} r={v.radius} seed={v.seed} id={id} />)}
  </g>;
}

export function PremiumHeathlandSurface({ id, region, neighbor = false }: { id: string; region: Region; neighbor?: boolean }) {
  const { surface, path } = region;
  const sand = surface === "BUNKER" || surface === "WASTE_AREA";
  const fill = surface === "FAIRWAY" ? `url(#${id}-heath-fairway)`
    : surface === "GREEN" ? `url(#${id}-heath-green)`
      : sand ? `url(#${id}-heath-sand)` : surface === "WATER" ? "#335e69"
        : surface === "FIRST_CUT" || surface === "FRINGE" ? "#8dae62"
          : surface === "PRIMARY_ROUGH" ? "#6a8050"
            : surface === "DEEP_ROUGH" || surface === "TREES" ? "#425d40"
              : surface === "TEE_BOX" ? "#88ad61" : "#748b54";
  return <g pointerEvents="none" opacity={neighbor ? .76 : undefined}>
    {sand ? <path d={path} fill="none" stroke="#45583b" strokeWidth="4" opacity=".42" /> : null}
    <path d={path} fill={fill} />
    {sand ? <><path d={path} fill={`url(#${id}-heath-sand-grain)`} />
      <path d={path} fill="none" stroke="#eee0a8" strokeWidth="1.1" opacity=".74" /></> : null}
    {["FAIRWAY", "FIRST_CUT", "GREEN"].includes(surface)
      ? <path d={path} fill={`url(#${id}-heath-turf)`} /> : null}
    {surface === "WATER" ? <>
      <path d={path} fill="none" stroke="#a6a07a" strokeWidth="5" opacity=".49" />
      <path d={path} fill="none" stroke="#3c5147" strokeWidth="3" opacity=".86" />
      <path d={path} fill="#558796" opacity=".15" />
      <path d={path} fill="none" stroke="#add0c7" strokeWidth=".8" opacity=".27" />
    </> : null}
  </g>;
}

export function PremiumHeathlandProperty({ id, plan }: { id: string; plan: PropertyPlan }) {
  return <g pointerEvents="none" aria-hidden="true">
    {plan.glimpses.map((glimpse, index) => {
      const corridor = premiumHeathlandNeighborCorridor(glimpse);
      return <g key={index} data-heathland-neighbor={glimpse.source} data-heathland-connected={!!corridor}>
        {corridor ? <g transform={corridor.transform} opacity=".76">
          {corridor.bands.map((band, i) => <path key={i} d={band.path}
            fill={i === 3 ? `url(#${id}-heath-fairway)` : ["#556a4c", "#687d50", "#8dae62"][i]}
            data-heathland-neighbor-band={i} />)}
        </g> : null}
        {glimpse.regions.map((region, i) => <PremiumHeathlandSurface key={i} id={id} region={region} neighbor />)}
      </g>;
    })}
    {plan.paths.map((path, i) => <g key={i} data-heathland-walking-track={path.kind ?? "main"}>
      <path d={path.d} fill="none" stroke="#3d4935" strokeWidth={path.width + 4} opacity=".3" />
      <path d={path.d} fill="none" stroke="#cdbb90" strokeWidth={path.width} opacity=".88" />
      <path d={path.d} fill="none" stroke="#f3dfab" strokeWidth=".8" opacity=".4" />
    </g>)}
  </g>;
}

export function PremiumHeathlandCanonicalPines({ id, vegetation }: { id: string; vegetation: readonly HeathlandDecoration[] }) {
  return <g pointerEvents="none" aria-hidden="true">
    {vegetation.filter((v) => v.kind === "canonical-pine").map((v, i) =>
      <Pine key={i} x={v.x} y={v.y} r={v.radius} seed={v.seed} id={id} canonical />)}
  </g>;
}

export function PremiumHeathlandAtmosphere({ id, left, width, height }: { id: string; left: number; width: number; height: number }) {
  return <g pointerEvents="none" aria-hidden="true">
    <rect x={left} width={width} height={height} fill={`url(#${id}-heath-light)`} />
    <rect x={left} width={width} height={height} fill={`url(#${id}-heath-vertical)`} />
  </g>;
}
