/**
 * Pure geometry + fidelity layer for the 2D hole schematic (spec: web-hole-visualization).
 *
 * `projectHole` turns a hole's sim numbers + its biome kit into a complete, render-ready `HoleLayout`: a
 * fairway corridor, green, tee, pin (on its real `pinLateral` side), and seed-derived cosmetic placement
 * (dogleg lean, hazard flanks, tree scatter). `ballPosition` resolves a shot's resting spot on that layout
 * under the fidelity rule — load-bearing facts (surface, distance) are honoured literally; the exact pixel is
 * flavor. No rendering here (kept JSX-free); the component in the play layer draws from this data.
 */

import type { BiomeKit } from "@/lib/play/biomes";

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
}

/** A resolved shot's load-bearing facts, from the `ShotOutcome` mutation result. */
export interface ResolvedShot {
  readonly finalSurface: string;
  readonly carry: number;
  readonly lateral: number;
  readonly distanceRemaining: number;
}

export interface Point {
  readonly x: number;
  readonly y: number;
}

export interface PlacedTree {
  readonly x: number;
  readonly y: number;
  readonly scale: number;
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

/** The parametric frame — enough to re-derive any point along the hole for ball placement. */
interface Frame {
  readonly bx: number;
  readonly ctrlX: number;
  readonly ctrlY: number;
  readonly grnX: number;
  readonly grnY: number;
  readonly teeY: number;
  readonly playLen: number;
  readonly yScale: number;
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
  readonly trees: readonly PlacedTree[];
  readonly flank: -1 | 1;
  readonly elevation: { readonly light: boolean; readonly alpha: number } | null;
  readonly frame: Frame;
}

/** How a shot's landing should be presented on the hole. */
export interface BallPlacement {
  readonly x: number;
  readonly y: number;
  readonly effect: "splash" | "sand" | "roll";
}

const VIEW_W = 220;
const VIEW_H = 440;
const PAD_TOP = 40;
const PAD_BOT = 30;
const X_SCALE = 1.6;
const Y_SCALE_CAP = 0.6;
const DOGLEG_MAX = 40;
const ROUGH_EXTRA = 24;
const TREE_BASE_COUNT = 11;

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

/** The point on the fairway centerline at fraction `t` of the tee→green line. */
function bezier(f: Frame, t: number): Point {
  const u = 1 - t;
  return {
    x: u * u * f.bx + 2 * u * t * f.ctrlX + t * t * f.grnX,
    y: u * u * f.teeY + 2 * u * t * f.ctrlY + t * t * f.grnY,
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

/** Projects a hole's geometry + biome into a complete render-ready layout. Deterministic from the layout seed. */
export function projectHole(hole: HoleGeom, kit: BiomeKit): HoleLayout {
  const rng = mulberry32(seedInt(hole.layoutSeed));
  const usable = VIEW_H - PAD_TOP - PAD_BOT;
  const playLen = Math.max(1, hole.length + hole.pinDepth); // tee-to-pin
  const yScale = Math.min(Y_SCALE_CAP, usable / playLen);
  const holeLen = playLen * yScale;

  const bx = VIEW_W / 2;
  const teeY = VIEW_H - PAD_BOT;
  const grnY = teeY - holeLen;
  const dog = (rng() * 2 - 1) * DOGLEG_MAX;
  const ctrlX = bx + dog;
  const ctrlY = (teeY + grnY) / 2;
  const grnX = bx + dog * 0.55;
  const frame: Frame = { bx, ctrlX, ctrlY, grnX, grnY, teeY, playLen, yScale, xScale: X_SCALE };

  const centerline = `M${bx} ${teeY} Q ${ctrlX} ${ctrlY} ${grnX} ${grnY}`;
  const fairwayWidth = hole.fairwayHalfWidth * X_SCALE * 2;
  const roughWidth = fairwayWidth + ROUGH_EXTRA;
  const flank: -1 | 1 = rng() < 0.5 ? -1 : 1;

  const grx = hole.greenHalfWidth * X_SCALE;
  const gry = Math.max(10, hole.greenDepth * yScale * 1.9);
  // The pin sits on its real lateral side (load-bearing); clamped to stay on its own green.
  const pinX = grnX + clamp(hole.pinLateral, -hole.greenHalfWidth, hole.greenHalfWidth) * X_SCALE;
  const pinY = grnY - gry * 0.25;

  let water: WaterHazard | null = null;
  if (hole.hasWater && kit.water) {
    const big = kit.biome === "tropical" ? 1.6 : 1;
    const wx = grnX + flank * (grx + 12);
    const wy = grnY + 16;
    water = { d: waterBlob(wx, wy, 13 * big, 36 * big), x: wx, y: wy };
  }

  const bunkers: Bunker[] = [];
  if (hole.hasGreensideBunker) {
    // Greenside bunker on the flank opposite the water, then a fairway bunker on long holes.
    bunkers.push({
      cx: grnX - flank * (grx + 8),
      cy: grnY + 7,
      rx: kit.potBunkers ? 6 : 8,
      ry: kit.potBunkers ? 6 : 5.4,
      pot: kit.potBunkers,
    });
    if (hole.par >= 4) {
      const t = 0.55;
      const p = bezier(frame, t);
      bunkers.push({
        cx: p.x + flank * (fairwayWidth / 2 - 1),
        cy: p.y,
        rx: kit.potBunkers ? 5 : 6.5,
        ry: kit.potBunkers ? 5 : 4.5,
        pot: kit.potBunkers,
      });
    }
  }

  const trees: PlacedTree[] = [];
  if (hole.hasTrees || kit.vegetation === "yucca") {
    const count = Math.round(kit.vegetationDensity * TREE_BASE_COUNT);
    for (let i = 0; i < count; i++) {
      const t = 0.1 + rng() * 0.82;
      const side = rng() < 0.5 ? -1 : 1;
      const p = bezier(frame, t);
      trees.push({
        x: p.x + side * (fairwayWidth / 2 + 8 + rng() * 11),
        y: p.y,
        scale: 0.85 + rng() * 0.6,
      });
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
    tee: { x: bx, y: teeY },
    green: { cx: grnX, cy: grnY, rx: grx, ry: gry },
    pin: { x: pinX, y: pinY },
    water,
    bunkers,
    trees,
    flank,
    elevation,
    frame,
  };
}

/**
 * Resolves where a shot's ball rests on the layout, under the fidelity rule. A resolved hazard surface always
 * wins: WATER snaps into the rendered water, BUNKER into the nearest rendered bunker — the picture can never
 * disagree with the surface, distance, or penalty the sim reported. Non-hazard lies place along the centerline
 * at the fraction implied by the remaining distance, offset by the shot's real signed lateral.
 */
export function ballPosition(layout: HoleLayout, shot: ResolvedShot): BallPlacement {
  const surface = shot.finalSurface.toUpperCase();
  const f = layout.frame;

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

  const x = clamp(along.x + shot.lateral * f.xScale, 6, layout.width - 6);
  return { x, y: along.y, effect: "roll" };
}

/** A point on the fairway centerline at fraction `t` (0 = tee, 1 = green) — for overlays and playback. */
export function centerlinePoint(layout: HoleLayout, t: number): Point {
  return bezier(layout.frame, t);
}

/**
 * The centerline fractions a shot could carry to, from the current lie: the reachable landing stretch. Maps the
 * shot's carry window (`minReach`..`maxReach`, relative to the ball `distanceToPin` from the pin) to `t` in
 * [0,1] along the hole. Truthful and longitudinal only — it claims no lateral hazard sides.
 */
export function reachStretch(
  layout: HoleLayout,
  distanceToPin: number,
  minReach: number,
  maxReach: number,
): { readonly tMin: number; readonly tMax: number } {
  const f = layout.frame;
  const fromTee = f.playLen - distanceToPin;
  const map = (carry: number) => clamp((fromTee + carry) / f.playLen, 0, 1);
  return { tMin: map(minReach), tMax: map(maxReach) };
}
