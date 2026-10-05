/**
 * Pure geometry + fidelity layer for the 2D hole schematic (spec: web-hole-visualization).
 *
 * This module is the retained legacy/fallback layout helper. Canonical play rendering uses
 * `canonical-geometry.ts`, which derives every gameplay terrain path from the API's `geometry.regions` and
 * `geometry.playableBoundary`. The seed-derived landforms below must never be used when canonical geometry is
 * present; any remaining seed use is decorative fallback only. No rendering happens here (kept JSX-free).
 */

import type { Biome, BiomeKit, Vegetation } from "@/lib/play/biomes";

/** The hole geometry the layout needs — structurally the `playingHole` query result. */
export interface HoleGeom {
  readonly holeNumber: number;
  readonly par: number;
  readonly length: number;
  readonly fairwayHalfWidth: number;
  readonly greenHalfWidth: number;
  readonly greenDepth: number;
  readonly elevationDelta: number;
  readonly hasGreensideBunker: boolean;
  readonly hasWater: boolean;
  readonly hasTrees: boolean;
  readonly pinLateral: number;
  readonly pinDepth: number;
  readonly courseType: string;
  readonly layoutSeed: string;
  readonly geometry?: {
    readonly tee: Point;
    readonly cup: Point;
    readonly playableBoundary: readonly Point[];
    readonly regions: readonly { readonly surface: string; readonly boundary: readonly Point[] }[];
  };
  readonly ball?: { readonly position: Point; readonly lie: string };
}

/** A resolved shot's load-bearing facts, from the `ShotOutcome` mutation result. */
export interface ResolvedShot {
  readonly finalSurface: string;
  readonly carry: number;
  readonly lateral: number;
  readonly distanceRemaining: number;
  readonly settlement?: {
    readonly contact: { readonly position: Point; readonly surface: string };
    readonly recoveryPosition?: Point | null;
    readonly recoveryKind: string;
    readonly ball: { readonly position: Point; readonly lie: string };
  } | null;
}

export interface Point {
  readonly x: number;
  readonly y: number;
}

export interface PlacedScatter {
  readonly x: number;
  readonly y: number;
  readonly scale: number;
  readonly kind: Vegetation;
}

export interface WaterHazard {
  readonly d: string;
  readonly x: number;
  readonly y: number;
}

export interface Bunker {
  readonly cx: number;
  readonly cy: number;
  readonly rx: number;
  readonly ry: number;
  readonly pot: boolean;
}

/** A land stretch of the corridor between water carries, as a spine fraction range. `capEnd` rounds the far shore. */
export interface LandSegment {
  readonly t0: number;
  readonly t1: number;
  readonly capEnd: boolean;
}

/** A forced-carry water gap across the corridor, as a spine fraction range (tropical only). */
export interface Carry {
  readonly t0: number;
  readonly t1: number;
}

/** The parametric frame — a cubic fairway spine (tee → green), enough to re-derive any point for ball placement. */
interface Frame {
  readonly p0: Point;
  readonly c1: Point;
  readonly c2: Point;
  readonly p3: Point;
  readonly playLen: number;
  readonly xScale: number;
}

export interface HoleLayout {
  readonly width: number;
  readonly height: number;
  readonly centerline: string;
  readonly fairwayWidth: number;
  readonly roughWidth: number;
  readonly tee: Point;
  readonly green: { readonly cx: number; readonly cy: number; readonly rx: number; readonly ry: number };
  readonly pin: Point;
  readonly water: WaterHazard | null;
  readonly bunkers: readonly Bunker[];
  readonly scatter: readonly PlacedScatter[];
  /** Forced-carry water gaps across the corridor (tropical); empty on holes with a continuous corridor. */
  readonly carries: readonly Carry[];
  /** The land stretches to draw between the carries — a single full-length segment when there are no carries. */
  readonly landSegments: readonly LandSegment[];
  readonly flank: -1 | 1;
  readonly elevation: { readonly light: boolean; readonly alpha: number } | null;
  readonly walls: WallParams | null;
  /** Links coastal margin: -1 sea on the left, +1 on the right, 0 none. */
  readonly coast: -1 | 0 | 1;
  readonly frame: Frame;
}

/** How a shot's landing should be presented on the hole. */
export interface BallPlacement {
  readonly x: number;
  readonly y: number;
  readonly effect: "splash" | "sand" | "roll";
  /** The shot was holed — the ball rests in the cup (on the pin) and the playback should drop it in. */
  readonly holed?: boolean;
}

const VIEW_W = 220;
const VIEW_H = 440;
const PAD_TOP = 40;
const PAD_BOT = 30;
const X_SCALE = 1.6;
const DOGLEG_MIN_AMP = 70; // lateral pull of the elbow for the gentlest dogleg…
const DOGLEG_AMP_SPAN = 58; // …up to DOGLEG_MIN_AMP + this for the sharpest (blind) ones
const ROUGH_EXTRA = 24;
/** Approx rendered footprint radius (px, at scale 1) per scatter kind — for non-overlap spacing. */
const SCATTER_BASE_R: Record<Vegetation, number> = {
  deciduous: 7,
  pine: 6,
  palm: 8,
  yucca: 6,
  saguaro: 7,
  barrel: 5,
  scrub: 4,
  rock: 6,
  heather: 7,
  gorse: 4,
  birch: 7,
};
const scatterRadius = (kind: Vegetation, scale: number) => SCATTER_BASE_R[kind] * scale;

// Dynamic viewport zoom (spec: fill the stage regardless of hole length). Every hole now fills the tee→green
// span vertically; a bounded "altitude" zoom then sizes the objects so distance still reads: short holes render
// as low-altitude close-ups (bigger trees/hazards/green), long holes as high-altitude overviews (smaller ones).
// `REF_LEN` is the length that renders at neutral zoom (1.0); the sqrt softens the ramp and the clamp keeps
// laterals from overflowing the frame on very short holes.
const REF_LEN = 480;
const ZOOM_MIN = 0.74;
const ZOOM_MAX = 1.7;

const clamp = (v: number, lo: number, hi: number) => Math.max(lo, Math.min(hi, v));

/** A stable 32-bit seed from the hole's string layout seed (survives 64-bit / negative values). */
function seedInt(layoutSeed: string): number {
  let h = 2166136261 >>> 0;
  for (let i = 0; i < layoutSeed.length; i++) {
    h ^= layoutSeed.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  return h >>> 0;
}

/** mulberry32 — a small, fast, deterministic PRNG seeded from the layout seed. */
function mulberry32(seed: number): () => number {
  let a = seed >>> 0;
  return () => {
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

/** The point on the fairway centerline at fraction `t` (0 = tee, 1 = green) — for drawing/placement layers. */
export function pointAt(layout: HoleLayout, t: number): Point {
  return bezier(layout.frame, t);
}

/** A deterministic PRNG seeded from a hole's layout seed (+ optional salt), for reproducible cosmetic drawing. */
export function seededRng(layoutSeed: string, salt = ""): () => number {
  return mulberry32(seedInt(layoutSeed + salt));
}

/** Slate-wall shape parameters for a mountain hole (the jagged inner edge of each cliff). */
export interface WallParams {
  readonly lW: number;
  readonly rW: number;
  readonly lp: number;
  readonly rp: number;
}

/** The inner-edge x of a mountain wall at height `y` (side -1 = left cliff, +1 = right cliff). */
export function wallEdgeX(w: WallParams, side: -1 | 1, y: number): number {
  return side < 0
    ? w.lW + Math.sin(y * 0.03 + w.lp) * 9 + Math.sin(y * 0.012) * 6
    : VIEW_W - (w.rW + Math.sin(y * 0.028 + w.rp) * 9 + Math.sin(y * 0.013) * 6);
}

/** The wavy shoreline x of a links coastal margin at height `y` (side -1 = sea on the left, +1 = on the right). */
export function coastShoreX(side: -1 | 1, y: number): number {
  const base = side > 0 ? VIEW_W - 3 - 56 : 3 + 56;
  return base - side * Math.sin(y * 0.02) * 8;
}

/** The point on the fairway centerline (a cubic bezier) at fraction `t` of the tee→green line. */
function bezier(f: Frame, t: number): Point {
  const u = 1 - t;
  return {
    x: u * u * u * f.p0.x + 3 * u * u * t * f.c1.x + 3 * u * t * t * f.c2.x + t * t * t * f.p3.x,
    y: u * u * u * f.p0.y + 3 * u * u * t * f.c1.y + 3 * u * t * t * f.c2.y + t * t * t * f.p3.y,
  };
}

/** An organic (pointy-corner-free) water blob path around an anchor. */
function waterBlob(x: number, y: number, rw: number, rh: number): string {
  return (
    `M${x} ${y - rh} ` +
    `C ${x + rw} ${y - rh * 0.6},${x + rw * 1.1} ${y + rh * 0.5},${x + rw * 0.3} ${y + rh} ` +
    `C ${x - rw * 0.6} ${y + rh * 1.1},${x - rw * 1.2} ${y + rh * 0.2},${x - rw * 0.4} ${y - rh * 0.7} Z`
  );
}

/** Per-biome championship bunkering: greenside/fairway count ranges, base radius, and size variance (px @ zoom 1). */
const BUNKER_TUNING: Record<Biome, { gs: [number, number]; fw: [number, number]; size: number; sizeVar: number }> = {
  parkland: { gs: [2, 3], fw: [1, 2], size: 8, sizeVar: 2.4 },
  links: { gs: [2, 3], fw: [2, 4], size: 6, sizeVar: 1.6 }, // pot bunkers — heavily bunkered, small and deep
  desert: { gs: [1, 2], fw: [1, 2], size: 11, sizeVar: 4.5 }, // fewer, but large sprawling waste
  alpine: { gs: [2, 3], fw: [1, 2], size: 8, sizeVar: 3 },
  heathland: { gs: [2, 3], fw: [1, 2], size: 8, sizeVar: 2.6 },
  tropical: { gs: [2, 3], fw: [0, 1], size: 8, sizeVar: 2 }, // water does most of the guarding here
};

interface BunkerCtx {
  readonly hole: HoleGeom;
  readonly kit: BiomeKit;
  readonly frame: Frame;
  readonly grnX: number;
  readonly grnY: number;
  readonly grx: number;
  readonly gry: number;
  readonly fairwayWidth: number;
  readonly flank: -1 | 1;
  readonly dir: -1 | 0 | 1;
  readonly zoom: number;
  readonly water: WaterHazard | null;
  readonly carries: readonly Carry[];
}

/**
 * Championship-style bunkering, tuned per biome: 2–3 greenside guards (front/side, biased off the water flank) plus
 * fairway bunkers on par-4s and -5s (landing-zone guards, biased to the inside of the dogleg). Sizes and shapes vary
 * (small flashed → large sprawling waste; pots stay small and round); nothing overlaps another bunker, the greenside
 * water, or a carry gap. Deterministic from its own seed stream, so it never shifts the main layout RNG.
 */
function planBunkers(ctx: BunkerCtx): Bunker[] {
  const { hole, kit, frame, grnX, grnY, grx, gry, fairwayWidth, flank, dir, zoom, water, carries } = ctx;
  const bunkers: Bunker[] = [];
  // Links greens are always potted, so a links hole is never left bunkerless even when the sim flags no greenside
  // bunker (placement is cosmetic — a resolved BUNKER lie still snaps to the nearest rendered bunker). Every other
  // biome keys off the sim signal: no flag, no bunkers.
  const alwaysBunkered = kit.biome === "links";
  if (!hole.hasGreensideBunker && !alwaysBunkered) return bunkers;

  const br = mulberry32(seedInt(hole.layoutSeed + "bunker"));
  const tune = BUNKER_TUNING[kit.biome];
  const pot = kit.potBunkers;
  const pick = ([lo, hi]: [number, number]) => lo + Math.floor(br() * (hi - lo + 1));

  const overlaps = (cx: number, cy: number, rr: number) =>
    bunkers.some((b) => Math.hypot(cx - b.cx, cy - b.cy) < Math.max(b.rx, b.ry) + rr + 2);
  const inWater = (cx: number, cy: number, rr: number) =>
    water !== null && Math.hypot(cx - water.x, cy - water.y) < 30 * zoom + rr;
  const inCarry = (t: number) => carries.some((c) => t >= c.t0 - 0.02 && t <= c.t1 + 0.02);
  const sizeOf = () => {
    const base = (tune.size + (br() * 2 - 1) * tune.sizeVar) * zoom;
    const rx = Math.max(3, base);
    const ry = pot ? rx : rx * (0.5 + br() * 0.35); // pots stay round; flashed bunkers flatten
    return { rx, ry };
  };

  // Greenside guards — placed around the front/side arc of the green ellipse (never straight behind), off the flank
  // the water sits on. y grows toward the tee, so sin(θ) > 0 is the front of the green.
  const gsN = pick(tune.gs);
  for (let i = 0; i < gsN; i++) {
    let placed = false;
    for (let a = 0; a < 14 && !placed; a++) {
      const theta = -0.12 * Math.PI + br() * 1.24 * Math.PI; // lower arc: both sides + front, sparing the back
      const { rx, ry } = sizeOf();
      const gap = (5 + br() * 4) * zoom + Math.max(rx, ry) * 0.4;
      const cx = grnX + Math.cos(theta) * (grx + gap);
      const cy = grnY + Math.sin(theta) * (gry + gap);
      // Bias off the water flank: reject a candidate on the water side unless we're struggling to place.
      if (a < 8 && water && Math.sign(cx - grnX) === flank) continue;
      if (overlaps(cx, cy, rx) || inWater(cx, cy, rx)) continue;
      bunkers.push({ cx, cy, rx, ry, pot });
      placed = true;
    }
  }

  // Fairway guards on the longer holes — landing-zone bunkers just off the fairway edge, biased inside the dogleg.
  if (hole.par >= 4) {
    const fwN = pick(tune.fw);
    for (let i = 0; i < fwN; i++) {
      let placed = false;
      for (let a = 0; a < 12 && !placed; a++) {
        const t = 0.42 + br() * 0.26;
        if (inCarry(t)) continue;
        const p = bezier(frame, t);
        const side = dir !== 0 ? dir : br() < 0.5 ? -1 : 1;
        const { rx, ry } = sizeOf();
        const cx = p.x + side * (fairwayWidth / 2 + (2 + br() * 3) * zoom);
        const cy = p.y;
        if (overlaps(cx, cy, rx) || inWater(cx, cy, rx)) continue;
        bunkers.push({ cx, cy, rx, ry, pot });
        placed = true;
      }
    }
  }
  return bunkers;
}

/** Projects a hole's geometry + biome into a complete render-ready layout. Deterministic from the layout seed. */
export function projectHole(hole: HoleGeom, kit: BiomeKit): HoleLayout {
  const rng = mulberry32(seedInt(hole.layoutSeed));
  const usable = VIEW_H - PAD_TOP - PAD_BOT;
  const playLen = Math.max(1, hole.length + hole.pinDepth); // tee-to-pin
  const holeLen = usable; // always fill the stage vertically, tee to green
  // Altitude zoom: neutral at REF_LEN, larger (closer) on short holes, smaller (higher) on long ones.
  const zoom = clamp(Math.sqrt(REF_LEN / playLen), ZOOM_MIN, ZOOM_MAX);
  const xScale = X_SCALE * zoom; // lateral px per yard, scaled by the zoom

  const bx = VIEW_W / 2;
  const teeY = VIEW_H - PAD_BOT;
  const grnY = teeY - holeLen;
  const hh = teeY - grnY;
  // Dogleg: ~a third of holes play straight; the rest bend left/right, the sharpest reading as blind. The tee is
  // offset OPPOSITE the bend and the green toward it (a compressed cubic elbow), so the hole swings across the
  // frame while both stay in view. Derived from the layout seed, so each hole is reproducible.
  const straight = rng() < 0.3;
  const dir = straight ? 0 : rng() < 0.5 ? -1 : 1;
  const amp = straight ? 0 : DOGLEG_MIN_AMP + rng() * DOGLEG_AMP_SPAN;
  const teeX = bx - (dir ? dir * Math.min(28, amp * 0.28) : 0);
  const grnX = bx + (dir ? dir * clamp(amp * 0.44, 0, 56) : (rng() * 2 - 1) * 6);
  const p0: Point = { x: teeX, y: teeY };
  const c1: Point = { x: teeX, y: teeY - 0.6 * hh };
  const c2: Point = { x: grnX, y: grnY + 0.55 * hh };
  const p3: Point = { x: grnX, y: grnY };
  const frame: Frame = { p0, c1, c2, p3, playLen, xScale };

  const centerline =
    `M${teeX.toFixed(1)} ${teeY} C ${c1.x.toFixed(1)} ${c1.y.toFixed(1)} ` +
    `${c2.x.toFixed(1)} ${c2.y.toFixed(1)} ${grnX.toFixed(1)} ${grnY.toFixed(1)}`;
  const widthMul = 0.9 + rng() * 0.5; // fairway-width variance (narrow to wide)
  const fairwayWidth = hole.fairwayHalfWidth * xScale * 2 * widthMul;
  const roughWidth = fairwayWidth + ROUGH_EXTRA * zoom;
  const flank: -1 | 1 = rng() < 0.5 ? -1 : 1;

  const grx = hole.greenHalfWidth * xScale;
  const gry = Math.max(10, hole.greenDepth * xScale * 1.05);
  // The pin sits on its real lateral side (load-bearing); clamped to stay on its own green.
  const pinX = grnX + clamp(hole.pinLateral, -hole.greenHalfWidth, hole.greenHalfWidth) * xScale;
  const pinY = grnY - gry * 0.25;

  let water: WaterHazard | null = null;
  if (hole.hasWater && kit.water) {
    const big = (kit.biome === "tropical" ? 1.6 : 1) * zoom;
    const wx = grnX + flank * (grx + 12 * zoom);
    const wy = grnY + 16 * zoom;
    water = { d: waterBlob(wx, wy, 13 * big, 36 * big), x: wx, y: wy };
  }

  // Tropical forced carries — one open-water gap across the corridor on water holes, sited between the tee shot and
  // the green. The land is drawn as the stretches around each gap; every other biome keeps one continuous segment.
  const carries: Carry[] = [];
  if (kit.island && hole.hasWater) {
    const cr = mulberry32(seedInt(hole.layoutSeed + "carry"));
    const center = 0.34 + cr() * 0.28;
    const half = 0.05 + cr() * 0.03;
    carries.push({ t0: center - half, t1: center + half });
  }
  const landSegments: LandSegment[] = [];
  let cursor = 0;
  for (const c of carries) {
    if (c.t0 > cursor) landSegments.push({ t0: cursor, t1: c.t0, capEnd: true });
    cursor = c.t1;
  }
  landSegments.push({ t0: cursor, t1: 1, capEnd: false });

  const bunkers = planBunkers({
    hole, kit, frame, grnX, grnY, grx, gry, fairwayWidth, flank, dir, zoom, water, carries,
  });

  // Biome scatter — the cohesive plant/rock family, placed deterministically and NEVER overlapping each other,
  // the corridor, the green complex, the water, or a bunker. `forest` fills the surround densely, `scatter` fills
  // it at medium density, `edge` lines the corridor shoulders sparsely. Drawn over bunkers / under the green.
  // Mountain slate walls frame the valley; their jagged inner edge is seeded here and reused for rendering.
  const walls: WallParams | null = kit.walls
    ? { lW: 24 + rng() * 12, rW: 24 + rng() * 12, lp: rng() * 6, rp: rng() * 6 }
    : null;
  // Links coastal margin — the sea runs down the side opposite the green, so the hole plays along the coast.
  const coast: -1 | 0 | 1 = kit.coastal && rng() < 0.55 ? (grnX >= bx ? -1 : 1) : 0;
  const onSea = (x: number, y: number) =>
    coast !== 0 && (coast > 0 ? x > coastShoreX(coast, y) : x < coastShoreX(coast, y));

  const scatter: PlacedScatter[] = [];
  if (kit.scatterMode !== "none" && kit.scatter.length > 0) {
    const sr = mulberry32(seedInt(hole.layoutSeed + "scatter"));
    const fairHalf = fairwayWidth / 2;
    const roughHalf = roughWidth / 2;
    const nearCL = (x: number, y: number) => {
      let m = Infinity;
      for (let i = 0; i <= 26; i++) {
        const p = bezier(frame, i / 26);
        const d = Math.hypot(p.x - x, p.y - y);
        if (d < m) m = d;
      }
      return m;
    };
    const complexClear = Math.max(grx, gry) * 0.4 + 22;
    const inGreen = (x: number, y: number) =>
      ((x - grnX) / (grx + complexClear)) ** 2 + ((y - grnY) / (gry + complexClear)) ** 2 < 1;
    const inWater = (x: number, y: number) => (water ? Math.hypot(x - water.x, y - water.y) < 26 * zoom : false);
    const inBunker = (x: number, y: number) =>
      bunkers.some((b) => Math.hypot(x - b.cx, y - b.cy) < Math.max(b.rx, b.ry) + 1);
    // At a carry gap the corridor land is absent, so a palm placed at the nominal shoulder would float in open
    // water. Exclude the whole longitudinal band the gap spans (plus a margin) from scatter.
    const carryBands = carries.map((c) => {
      const a = bezier(frame, c.t0).y;
      const b = bezier(frame, c.t1).y;
      return [Math.min(a, b) - 12 * zoom, Math.max(a, b) + 12 * zoom] as const;
    });
    const inCarryBand = (y: number) => carryBands.some(([lo, hi]) => y >= lo && y <= hi);
    // The corridor ribbon tapers to a point at the tee and rounds at every carry shoreline, so the ACTUAL land is
    // much narrower there than the nominal half-width. For tropical islands (where off-land is open ocean) a palm
    // must sit inside the real, tapered land — otherwise it floats in the water at the tee end or a carry edge.
    const nearestT = (x: number, y: number) => {
      let bt = 0;
      let m = Infinity;
      for (let i = 0; i <= 40; i++) {
        const t = i / 40;
        const p = bezier(frame, t);
        const dd = Math.hypot(p.x - x, p.y - y);
        if (dd < m) {
          m = dd;
          bt = t;
        }
      }
      return bt;
    };
    const segAt = (t: number) => landSegments.find((s) => t >= s.t0 && t <= s.t1);
    const roughHalfAt = (t: number, s: LandSegment) => {
      const local = (t - s.t0) / (s.t1 - s.t0 || 1);
      const startRamp = Math.pow(clamp(local / 0.16, 0, 1), 0.6);
      const endRamp = s.capEnd ? Math.pow(clamp((1 - local) / 0.16, 0, 1), 0.6) : 1;
      return roughHalf * (0.3 + 0.7 * (startRamp * endRamp));
    };
    const totalWeight = kit.scatter.reduce((s, k) => s + k.weight, 0);
    const pickKind = (): Vegetation => {
      let r = sr() * totalWeight;
      for (const k of kit.scatter) if ((r -= k.weight) <= 0) return k.kind;
      return kit.scatter[0].kind;
    };
    const step = kit.scatterMode === "forest" ? 17 : kit.scatterMode === "edge" ? 13 : 19;
    for (let gy = 4; gy <= VIEW_H - 4; gy += step) {
      for (let gx = 4; gx <= VIEW_W - 4; gx += step) {
        const x = gx + (sr() * 2 - 1) * 7;
        const y = gy + (sr() * 2 - 1) * 7;
        if (sr() < kit.scatterCull) continue;
        if (walls && (x < wallEdgeX(walls, -1, y) + 2 || x > wallEdgeX(walls, 1, y) - 2)) continue; // off the cliffs
        if (onSea(x, y)) continue; // never in the sea
        if (inCarryBand(y)) continue; // never in a forced-carry gap
        const d = nearCL(x, y);
        if (kit.scatterMode === "edge") {
          if (kit.island) {
            // Keep palms on the green shoulder of the ACTUAL (tapered) land — inside the beach/shallow rim, never
            // over open ocean at the tee end or a carry shoreline.
            const nt = nearestT(x, y);
            const seg = segAt(nt);
            if (!seg) continue; // over a carry gap
            if (d < fairHalf + 3 || d > roughHalfAt(nt, seg) - 5) continue;
          } else {
            // Links gorse — just off the corridor in the dune surround.
            if (d < fairHalf + 3 || d > roughHalf + 18) continue;
          }
        } else if (d < roughHalf + 3) {
          continue; // out in the surround, clear of the corridor
        }
        if (inGreen(x, y) || inWater(x, y) || inBunker(x, y)) continue;
        const kind = pickKind();
        const scale =
          kit.scatterMode === "forest"
            ? (0.7 + sr() * 0.7) * zoom
            : (kind === "pine" || kind === "birch" ? 0.5 + sr() * 0.28 : 0.82 + sr() * 0.5) * zoom;
        const r = scatterRadius(kind, scale);
        let ok = true;
        for (const p of scatter) {
          if (Math.hypot(x - p.x, y - p.y) < r + scatterRadius(p.kind, p.scale) + 1.5) {
            ok = false;
            break;
          }
        }
        if (ok) scatter.push({ x, y, scale, kind });
      }
    }
  }

  const elevation =
    kit.elevationShading && Math.abs(hole.elevationDelta) > 1
      ? { light: hole.elevationDelta > 0, alpha: Math.min(0.28, (Math.abs(hole.elevationDelta) / 25) * 0.28) }
      : null;

  return {
    width: VIEW_W,
    height: VIEW_H,
    centerline,
    fairwayWidth,
    roughWidth,
    tee: { x: teeX, y: teeY },
    green: { cx: grnX, cy: grnY, rx: grx, ry: gry },
    pin: { x: pinX, y: pinY },
    water,
    bunkers,
    scatter,
    carries,
    landSegments,
    walls,
    coast,
    flank,
    elevation,
    frame,
  };
}

/**
 * Resolves where a shot's ball rests on the layout, under the fidelity rule. A holed shot rests in the cup; a
 * resolved hazard surface always wins (WATER snaps into the rendered water, BUNKER into the nearest rendered
 * bunker); a ball on the GREEN/FRINGE is placed relative to the pin on a green-local scale so short putts read
 * as short putts; every other lie places along the centerline at the fraction implied by the remaining distance,
 * offset by the shot's real signed lateral. The picture can never disagree with the surface, distance, or
 * penalty the sim reported.
 */
export function ballPosition(layout: HoleLayout, shot: ResolvedShot): BallPlacement {
  const surface = shot.finalSurface.toUpperCase();
  const f = layout.frame;

  // Holed out — the ball is in the cup, on the pin, whatever surface label the sim attached.
  if (shot.distanceRemaining <= 0) {
    return { x: layout.pin.x, y: layout.pin.y, effect: "roll", holed: true };
  }

  if (surface === "WATER" && layout.water) {
    return { x: layout.water.x, y: layout.water.y, effect: "splash" };
  }

  const t = clamp(1 - shot.distanceRemaining / f.playLen, 0, 1);
  const along = bezier(f, t);

  if (surface === "BUNKER" && layout.bunkers.length > 0) {
    // The nearest rendered bunker to the ball's longitudinal position.
    const nearest = layout.bunkers.reduce((best, b) =>
      Math.abs(b.cy - along.y) < Math.abs(best.cy - along.y) ? b : best,
    );
    return { x: nearest.cx, y: nearest.cy, effect: "sand" };
  }

  // On (or fringing) the green: placing against the whole hole length would collapse every putt onto the pin —
  // a 10-yard putt and a 1-yard putt would sit pixels apart. Place relative to the cup on a green-local scale
  // instead, so the visual distance to the hole tracks the numeric distance to the pin.
  if (surface.includes("GREEN") || surface.includes("FRINGE")) {
    return greenBallPosition(layout, shot.distanceRemaining, shot.lateral);
  }

  const x = clamp(along.x + shot.lateral * f.xScale, 6, layout.width - 6);
  return { x, y: along.y, effect: "roll" };
}

/**
 * Places a ball resting on the green relative to the pin: `distanceRemaining` yards short of the cup along the
 * line of play, at a green-local pixels-per-yard scale, plus the shot's lateral miss. Capped just past the green
 * edge so an over-long "putt" (a long approach that trickled on) still reads on the putting surface.
 */
function greenBallPosition(layout: HoleLayout, distToPin: number, lateral: number): BallPlacement {
  const f = layout.frame;
  const pin = layout.pin;
  // Unit vector from the pin back up the line of play (toward the tee), from the centerline near the green.
  const back = bezier(f, 0.8);
  const len = Math.hypot(back.x - pin.x, back.y - pin.y) || 1;
  const ux = (back.x - pin.x) / len;
  const uy = (back.y - pin.y) / len;
  const off = Math.min(distToPin * f.xScale, Math.max(layout.green.rx, layout.green.ry) * 1.1);
  // Perpendicular for the shot's lateral miss (kept within the remaining distance so it stays plausible).
  const lat = clamp(lateral, -distToPin, distToPin) * f.xScale;
  const x = clamp(pin.x + ux * off - uy * lat, 6, layout.width - 6);
  const y = clamp(pin.y + uy * off + ux * lat, 6, layout.height - 6);
  return { x, y, effect: "roll" };
}
