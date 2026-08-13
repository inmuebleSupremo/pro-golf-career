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
  /** The shot was holed — the ball rests in the cup (on the pin) and the playback should drop it in. */
  readonly holed?: boolean;
}

const VIEW_W = 220;
const VIEW_H = 440;
const PAD_TOP = 40;
const PAD_BOT = 30;
const X_SCALE = 1.6;
const DOGLEG_MAX = 40;
const ROUGH_EXTRA = 24;
const TREE_BASE_COUNT = 11;

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
  const yScale = usable / playLen; // always fill the stage vertically, tee to green
  const holeLen = usable;
  // Altitude zoom: neutral at REF_LEN, larger (closer) on short holes, smaller (higher) on long ones.
  const zoom = clamp(Math.sqrt(REF_LEN / playLen), ZOOM_MIN, ZOOM_MAX);
  const xScale = X_SCALE * zoom; // lateral px per yard, scaled by the zoom

  const bx = VIEW_W / 2;
  const teeY = VIEW_H - PAD_BOT;
  const grnY = teeY - holeLen;
  const dog = (rng() * 2 - 1) * DOGLEG_MAX;
  const ctrlX = bx + dog;
  const ctrlY = (teeY + grnY) / 2;
  const grnX = bx + dog * 0.55;
  const frame: Frame = { bx, ctrlX, ctrlY, grnX, grnY, teeY, playLen, yScale, xScale };

  const centerline = `M${bx} ${teeY} Q ${ctrlX} ${ctrlY} ${grnX} ${grnY}`;
  const fairwayWidth = hole.fairwayHalfWidth * xScale * 2;
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

  const bunkers: Bunker[] = [];
  if (hole.hasGreensideBunker) {
    // Greenside bunker on the flank opposite the water, then a fairway bunker on long holes.
    bunkers.push({
      cx: grnX - flank * (grx + 8 * zoom),
      cy: grnY + 7 * zoom,
      rx: (kit.potBunkers ? 6 : 8) * zoom,
      ry: (kit.potBunkers ? 6 : 5.4) * zoom,
      pot: kit.potBunkers,
    });
    if (hole.par >= 4) {
      const t = 0.55;
      const p = bezier(frame, t);
      bunkers.push({
        cx: p.x + flank * (fairwayWidth / 2 - 1),
        cy: p.y,
        rx: (kit.potBunkers ? 5 : 6.5) * zoom,
        ry: (kit.potBunkers ? 5 : 4.5) * zoom,
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
        x: p.x + side * (fairwayWidth / 2 + (8 + rng() * 11) * zoom),
        y: p.y,
        scale: (0.85 + rng() * 0.6) * zoom,
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
