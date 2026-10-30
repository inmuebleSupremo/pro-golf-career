import type { CanonicalPlayingGeometry, CanonicalRenderModel } from "@/lib/play/canonical-geometry";
import type { Point } from "@/lib/play/hole-geometry";
import type { DesertCorridor } from "@/lib/play/premium-desert-corridor";
import { premiumDesertNeighborCorridor } from "@/lib/play/premium-desert-neighbor-corridor";
import type { PropertyPlan } from "@/lib/play/premium-parkland-property";
import { routeSamples } from "@/lib/play/premium-parkland-property";
import { seededRandom } from "@/lib/play/premium-parkland-decoration";
import { StageSideFocusGradient } from "./stage-side-focus";

const f = (n: number) => n.toFixed(1);
type Region = Readonly<{ surface: string; path: string }>;
type Protected = Readonly<{ points: readonly Point[]; minX: number; maxX: number; minY: number; maxY: number }>;
function protect(points: readonly Point[]): Protected {
  return { points, minX: Math.min(...points.map((p) => p.x)), maxX: Math.max(...points.map((p) => p.x)),
    minY: Math.min(...points.map((p) => p.y)), maxY: Math.max(...points.map((p) => p.y)) };
}
function inside(p: Point, polygon: readonly Point[]): boolean {
  let hit = false;
  for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
    const a = polygon[i], b = polygon[j];
    if ((a.y > p.y) !== (b.y > p.y) && p.x < (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x) hit = !hit;
  }
  return hit;
}
function distance(p: Point, a: Point, b: Point): number {
  const dx = b.x - a.x, dy = b.y - a.y, length = dx * dx + dy * dy;
  const t = length ? Math.max(0, Math.min(1, ((p.x - a.x) * dx + (p.y - a.y) * dy) / length)) : 0;
  return Math.hypot(p.x - a.x - t * dx, p.y - a.y - t * dy);
}
/** Midpoint quadratics from the approved SVG's organic() construction. */
function organic(x: number, y: number, rx: number, ry: number, seed: number, wiggle = .16, n = 20): string {
  const points = Array.from({ length: n }, (_, i) => {
    const t = Math.PI * 2 * i / n;
    const k = 1 + wiggle * Math.sin(3 * t + seed * .63)
      + wiggle * .45 * Math.sin(7 * t + seed * 1.12) + wiggle * .22 * Math.cos(11 * t + seed * .77);
    return { x: x + rx * k * Math.cos(t), y: y + ry * k * Math.sin(t) };
  });
  const mid = (a: Point, b: Point) => `${f((a.x + b.x) / 2)} ${f((a.y + b.y) / 2)}`;
  return `M${mid(points.at(-1)!, points[0])} ${points.map((p, i) =>
    `Q${f(p.x)} ${f(p.y)} ${mid(p, points[(i + 1) % n])}`).join(" ")} Z`;
}

export type DesertDecoration = Readonly<{ kind: "gravel" | "wash" | "shelf" | "rock" | "shrub" | "cactus" | "mesquite" | "grit" | "canonical-tree";
  x: number; y: number; radius: number; seed: number }>;

/** Reserve legal golf and walking corridors first, then fit every raised object to open terrain. */
export function planDesertDecoration(geometry: CanonicalPlayingGeometry, model: CanonicalRenderModel,
  corridor: DesertCorridor | null, property: PropertyPlan | null, seed: string,
  left: number, width: number, height: number): readonly DesertDecoration[] {
  const blockedSurfaces = new Set(["TEE_BOX", "FIRST_CUT", "FAIRWAY", "FRINGE", "GREEN",
    "BUNKER", "WASTE_AREA", "WATER", "TREES"]);
  const blocked = [protect(geometry.playableBoundary.map(model.project)),
    ...geometry.regions.filter((r) => blockedSurfaces.has(r.surface)).map((r) => protect(r.boundary.map(model.project))),
    ...(corridor ? [protect(corridor.bands[0].boundary)] : []),
    ...(property?.glimpses.flatMap((g) => {
      const outer = premiumDesertNeighborCorridor(g)?.outerBoundary;
      return [...g.regions.filter((r) => blockedSurfaces.has(r.surface)).map((r) => protect(r.points)),
        ...(outer ? [protect(outer)] : [])];
    }) ?? [])];
  const routes = property?.paths.flatMap((path) => {
    const points = path.d.includes("C") ? routeSamples(path) : path.points;
    return points.slice(0, -1).map((a, i) => [a, points[i + 1], path.width / 2 + 4] as const);
  }) ?? [];
  const clear = (p: Point, r: number) => blocked.every((region) => {
    if (p.x < region.minX - r || p.x > region.maxX + r || p.y < region.minY - r || p.y > region.maxY + r) return true;
    return !inside(p, region.points) && region.points.every((a, i) =>
      distance(p, a, region.points[(i + 1) % region.points.length]) > r);
  }) && routes.every(([a, b, half]) => distance(p, a, b) > r + half);
  const random = seededRandom(`${seed}:approved-desert-terrain`);
  const result: DesertDecoration[] = [];
  const scale = Math.min(width / 800, height / 440);
  const place = (kind: DesertDecoration["kind"], attempts: number, cap: number,
    minRadius: number, span: number, margin: number) => {
    for (let i = 0, count = 0; i < attempts && count < cap; i += 1) {
      const p = { x: left + random() * width, y: random() * height };
      const radius = (minRadius + random() * span) * scale;
      if (!clear(p, radius * 1.42 + margin)) continue;
      result.push({ kind, ...p, radius, seed: i + result.length * 17 });
      count += 1;
    }
  };
  place("gravel", 90, 12, 25, 38, 6);
  place("wash", 80, 5, 26, 28, 7);
  place("shelf", 90, 12, 17, 26, 9);
  place("rock", 460, 115, 2.5, 6, 4);
  place("shrub", 300, 70, 3.5, 7, 5);
  place("mesquite", 120, 15, 9, 6, 7);
  place("cactus", 130, 22, 4, 4, 8);
  place("grit", 1100, 360, .7, 1.4, 3);
  const trees = geometry.regions.filter((r) => r.surface === "TREES" && r.boundary.length >= 3)
    .map((r) => protect(r.boundary.map(model.project)));
  const playable = geometry.regions.filter((r) => blockedSurfaces.has(r.surface) && r.surface !== "TREES")
    .map((r) => protect(r.boundary.map(model.project)));
  for (const [index, tree] of trees.entries()) {
    for (let i = 0, count = 0; i < 18 && count < 2; i += 1) {
      const p = { x: tree.minX + random() * (tree.maxX - tree.minX),
        y: tree.minY + random() * (tree.maxY - tree.minY) };
      const radius = 3.2 + random() * 1.8;
      if (!inside(p, tree.points) || playable.some((r) => inside(p, r.points)
        || r.points.some((a, j) => distance(p, a, r.points[(j + 1) % r.points.length]) < radius + 1))) continue;
      result.push({ kind: "canonical-tree", ...p, radius, seed: index * 37 + i });
      count += 1;
    }
  }
  return result;
}

export function PremiumDesertDefs({ id }: { id: string }) {
  return <defs>
    <linearGradient id={`${id}-desert-ground`} x1="0" y1="0" x2="1" y2=".65"><stop stopColor="#aa8357" /><stop offset=".38" stopColor="#ca9d62" /><stop offset=".65" stopColor="#bc8957" /><stop offset="1" stopColor="#9a714e" /></linearGradient>
    <linearGradient id={`${id}-desert-gravel`} x1="0" y1="0" x2=".65" y2="1"><stop stopColor="#d4ac72" /><stop offset=".4" stopColor="#b99160" /><stop offset="1" stopColor="#94724b" /></linearGradient>
    <linearGradient id={`${id}-desert-rock`} x1="0" y1="0" x2=".7" y2="1"><stop stopColor="#c88e60" /><stop offset=".4" stopColor="#aa6d49" /><stop offset="1" stopColor="#654636" /></linearGradient>
    <linearGradient id={`${id}-desert-turf`} x1="0" y1="0" x2="1" y2=".24"><stop stopColor="#659946" /><stop offset=".23" stopColor="#89bf5a" /><stop offset=".51" stopColor="#a9d66d" /><stop offset=".78" stopColor="#92c460" /><stop offset="1" stopColor="#649849" /></linearGradient>
    <linearGradient id={`${id}-desert-green`} x1="0" y1="0" x2="1" y2=".18"><stop stopColor="#8fca70" /><stop offset=".52" stopColor="#b9e790" /><stop offset="1" stopColor="#85ba61" /></linearGradient>
    <linearGradient id={`${id}-desert-sand`} x1="0" y1="0" x2=".65" y2="1"><stop stopColor="#fae8b9" /><stop offset=".56" stopColor="#e7cb94" /><stop offset="1" stopColor="#b99a66" /></linearGradient>
    <linearGradient id={`${id}-desert-shrub`} x1="0" y1="0" x2=".8" y2="1"><stop stopColor="#75965e" /><stop offset=".38" stopColor="#58794a" /><stop offset="1" stopColor="#294d36" /></linearGradient>
    <StageSideFocusGradient id={`${id}-desert-light`} shade="#1b170e" />
    <linearGradient id={`${id}-desert-toplight`} x1="0" y1="0" x2="0" y2="1"><stop stopColor="#0a1410" stopOpacity=".18" /><stop offset=".18" stopColor="#000" stopOpacity="0" /><stop offset=".84" stopColor="#000" stopOpacity="0" /><stop offset="1" stopColor="#07110d" stopOpacity=".1" /></linearGradient>
    <pattern id={`${id}-desert-turf-grain`} width="19" height="23" patternUnits="userSpaceOnUse"><path d="M0 6l3-1m6 10 4-1" fill="none" stroke="#e8efad" strokeWidth=".6" opacity=".17" /></pattern>
    <pattern id={`${id}-desert-sand-grain`} width="12" height="10" patternUnits="userSpaceOnUse"><path d="M2 3l2-1m4 5 2-1" fill="none" stroke="#fff2cb" strokeWidth=".6" opacity=".44" /></pattern>
  </defs>;
}

export function PremiumDesertBunkerMask({ id, left, width, height, bunkers }: {
  id: string; left: number; width: number; height: number; bunkers: readonly string[];
}) {
  return <defs><mask id={`${id}-desert-bunker-cutouts`} maskUnits="userSpaceOnUse"
    x={left} y={0} width={width} height={height}>
    <rect x={left} y={0} width={width} height={height} fill="white" />
    {bunkers.map((path, i) => <path key={i} d={path} fill="black" stroke="black" strokeWidth="1.2" />)}
  </mask></defs>;
}

function DesertObject({ item, id }: { item: DesertDecoration; id: string }) {
  const { x, y, radius: r, seed, kind } = item;
  if (kind === "gravel") return <g>
    <path d={organic(x, y, r, r * .65, seed, .1, 28)} fill="#d4ad75" opacity=".37" />
    <path d={organic(x + 2, y - 2, r * .74, r * .48, seed + 3, .12, 25)} fill={`url(#${id}-desert-gravel)`} opacity=".3" />
  </g>;
  if (kind === "wash") return <g data-desert-dry-wash="true">
    <path d={organic(x, y, r * 1.25, r * .58, seed, .13, 30)} fill="#624d39" opacity=".12" />
    <path d={organic(x - 1, y - 1, r * 1.06, r * .44, seed + 3, .13, 30)} fill="#e4bf8c" opacity=".31" />
    <path d={organic(x - 2, y - 2, r * .82, r * .27, seed + 7, .11, 28)} fill="#f1d5a0" opacity=".21" />
  </g>;
  if (kind === "shelf") return <g data-desert-rock-shelf="true">
    <path d={organic(x + 2, y + 3, r * 1.12, r * .8, seed, .17, 28)} fill="#523f32" opacity=".26" />
    <path d={organic(x, y, r, r * .68, seed + 3, .18, 30)} fill={`url(#${id}-desert-rock)`} opacity=".76" />
    <path d={organic(x - 2, y - 2, r * .72, r * .42, seed + 7, .12, 25)} fill="#e0b17d" opacity=".35" />
    <path d={`M${f(x-r*.38)} ${f(y+r*.2)}q${f(r*.2)} ${f(r*.13)} ${f(r*.56)} ${f(-r*.1)}`} fill="none" stroke="#503a30" strokeWidth="1" opacity=".3" />
  </g>;
  if (kind === "rock") return <g>
    <path d={organic(x + 1.4, y + 2, r * 1.2, r * .8, seed, .2, 12)} fill="#3f362c" opacity=".28" />
    <path d={organic(x, y, r * 1.1, r * .66, seed + 4, .25, 13)} fill={["#97724e", "#ad8255", "#c49864", "#cda270", "#876449"][seed % 5]} opacity=".9" />
    <path d={`M${f(x-r*.6)} ${f(y-r*.2)}q${f(r*.45)} ${f(-r*.5)} ${f(r)} ${f(-r*.1)}`} fill="none" stroke="#f2c99a" strokeWidth=".8" opacity=".36" />
  </g>;
  if (kind === "cactus") return <g>
    <ellipse cx={x+3} cy={y+3} rx={r*.96} ry={r*.68} fill="#433e2d" opacity=".36" />
    <ellipse cx={x} cy={y} rx={r*.76} ry={r*.71} fill="#52764b" opacity=".94" />
    <ellipse cx={x-1} cy={y-1.2} rx={r*.32} ry={r*.29} fill="#8ca16c" opacity=".64" />
    {seed % 3 === 0 ? <ellipse cx={x+r*.65} cy={y+r*.18} rx={r*.42} ry={r*.37} fill="#486c45" opacity=".87" /> : null}
  </g>;
  if (kind === "shrub" || kind === "mesquite" || kind === "canonical-tree") return <g data-desert-canonical-tree={kind === "canonical-tree" || undefined}>
    <path d={organic(x+2, y+2.5, r, r*.75, seed, .2, 14)} fill="#423b2d" opacity=".3" />
    <path d={organic(x, y, r, r*.78, seed+9, .22, 16)} fill="#294c39" opacity=".9" />
    <path d={organic(x-1, y-1.3, r*.82, r*.62, seed+31, .18, 16)} fill={`url(#${id}-desert-shrub)`} opacity=".92" />
    <ellipse cx={x-r*.21} cy={y-r*.22} rx={r*.25} ry={r*.14} fill="#9fa46c" opacity=".47" />
  </g>;
  return null;
}

export function PremiumDesertLandscape({ id, left, width, height, decoration }: {
  id: string; left: number; width: number; height: number; decoration: readonly DesertDecoration[];
}) {
  const grit = ["#e8cb9c", "#5e5940", "#d4ac77"].map((color, tint) => ({ color,
    d: decoration.filter((v) => v.kind === "grit" && v.seed % 3 === tint)
      .map((v) => `M${f(v.x)} ${f(v.y)}l${f(v.radius * 2)} ${f(-v.radius)}`).join(" ") }));
  return <g pointerEvents="none" aria-hidden="true" data-desert-decoration={decoration.length}>
    <rect x={left} width={width} height={height} fill={`url(#${id}-desert-ground)`} />
    {decoration.filter((v) => v.kind !== "grit" && v.kind !== "canonical-tree")
      .map((v, i) => <DesertObject key={i} item={v} id={id} />)}
    {grit.map((s) => <path key={s.color} d={s.d} fill="none" stroke={s.color} strokeWidth=".7" opacity=".24" />)}
  </g>;
}

export function PremiumDesertSurface({ id, region, neighbor = false }: { id: string; region: Region; neighbor?: boolean }) {
  const { surface, path } = region;
  const bunker = surface === "BUNKER";
  const fill = surface === "FAIRWAY" ? `url(#${id}-desert-turf)`
    : surface === "GREEN" ? `url(#${id}-desert-green)`
      : bunker ? `url(#${id}-desert-sand)`
        : surface === "WASTE_AREA" ? "#d4ad75" : surface === "WATER" ? "#427487"
          : surface === "FIRST_CUT" || surface === "FRINGE" ? "#81ac55"
            : surface === "PRIMARY_ROUGH" ? "#658747"
              : surface === "DEEP_ROUGH" ? "#ae9565"
                : surface === "TEE_BOX" ? "#94bf61" : "#a48b5c";
  return <g pointerEvents="none" opacity={neighbor ? .78 : undefined}>
    {bunker ? <>
      <path d={path} fill="none" stroke="#8eab60" strokeWidth="4" opacity=".55" />
      <path d={path} fill="none" stroke="#705a3f" strokeWidth="2.3" opacity=".48" />
    </> : null}
    <path d={path} fill={fill} />
    {bunker ? <>
      <path d={path} fill={`url(#${id}-desert-sand-grain)`} />
      <path d={path} fill="none" stroke="#fff0c0" strokeWidth=".9" opacity=".48" />
    </> : null}
    {["FAIRWAY", "FIRST_CUT", "GREEN"].includes(surface)
      ? <path d={path} fill={`url(#${id}-desert-turf-grain)`} /> : null}
    {surface === "WATER" ? <path d={path} fill="none" stroke="#a8d7db" strokeWidth=".8" opacity=".4" /> : null}
  </g>;
}

export function PremiumDesertProperty({ id, plan }: { id: string; plan: PropertyPlan }) {
  return <g pointerEvents="none" aria-hidden="true">
    <g mask={`url(#${id}-desert-bunker-cutouts)`}>
      {plan.glimpses.map((glimpse, index) => {
        const corridor = premiumDesertNeighborCorridor(glimpse);
        return <g key={index} data-desert-neighbor={glimpse.source} data-desert-connected={!!corridor}>
          {corridor ? <g transform={corridor.transform} opacity=".78">
            {corridor.bands.map((band, i) => <path key={i} d={band.path}
              fill={i === 4 ? `url(#${id}-desert-turf)` : ["#b79768", "#4a7040", "#648a47", "#70a04b"][i]}
              data-desert-neighbor-band={i} />)}
          </g> : null}
          {glimpse.regions.filter((r) => !["DEEP_ROUGH", "PRIMARY_ROUGH", "FIRST_CUT", "FAIRWAY", "BUNKER", "WATER"].includes(r.surface))
            .map((r, i) => <PremiumDesertSurface key={i} id={id} region={r} neighbor />)}
        </g>;
      })}
    </g>
    {plan.glimpses.flatMap((g) => g.regions.filter((r) => r.surface === "BUNKER" || r.surface === "WATER"))
      .map((r, i) => <PremiumDesertSurface key={i} id={id} region={r} neighbor />)}
    {plan.paths.map((path, i) => <g key={i} data-desert-walking-track={path.kind ?? "active"}>
      <path d={path.d} fill="none" stroke="#87694b" strokeWidth={path.width + 5} opacity=".19" strokeLinecap="round" strokeLinejoin="round" />
      <path d={path.d} fill="none" stroke="#bd986a" strokeWidth={path.width + 2} opacity=".57" strokeLinecap="round" strokeLinejoin="round" />
      <path d={path.d} fill="none" stroke="#e5c28f" strokeWidth={path.width} opacity=".76" strokeLinecap="round" strokeLinejoin="round" />
      <path d={path.d} fill="none" stroke="#f4ddb0" strokeWidth=".5" opacity=".28" />
    </g>)}
  </g>;
}

export function PremiumDesertCanonicalTrees({ id, decoration }: { id: string; decoration: readonly DesertDecoration[] }) {
  return <g pointerEvents="none" aria-hidden="true">
    {decoration.filter((v) => v.kind === "canonical-tree").map((v, i) => <DesertObject key={i} id={id} item={v} />)}
  </g>;
}

export function PremiumDesertAtmosphere({ id, left, width, height }: { id: string; left: number; width: number; height: number }) {
  return <g pointerEvents="none" aria-hidden="true">
    <rect x={left} width={width} height={height} fill={`url(#${id}-desert-light)`} />
    <rect x={left} width={width} height={height} fill={`url(#${id}-desert-toplight)`} />
  </g>;
}
