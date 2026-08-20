/**
 * Pure geometry + fidelity layer for the 2D hole schematic (spec: web-hole-visualization).
 *
 * `projectHole` turns a hole's sim numbers + its biome kit into a complete, render-ready `HoleLayout`: a
 * fairway corridor, green, tee, pin (on its real `pinLateral` side), and seed-derived cosmetic placement
 * (dogleg lean, hazard flanks, tree scatter). `ballPosition` resolves a shot's resting spot on that layout
 * under the fidelity rule — load-bearing facts (surface, distance) are honoured literally; the exact pixel is
 * flavor. No rendering here (kept JSX-free); the component in the play layer draws from this data.
 */

import type { BiomeKit, Vegetation } from "@/lib/play/biomes";

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

  // Biome scatter — the cohesive plant/rock family, placed deterministically and NEVER overlapping each other,
  // the corridor, the green complex, the water, or a bunker. `forest` fills the surround densely, `scatter` fills
  // it at medium density, `edge` lines the corridor shoulders sparsely. Drawn over bunkers / under the green.
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
        const d = nearCL(x, y);
        if (kit.scatterMode === "edge") {
          if (d < fairHalf + 3 || d > roughHalf + 18) continue; // just off the corridor
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
