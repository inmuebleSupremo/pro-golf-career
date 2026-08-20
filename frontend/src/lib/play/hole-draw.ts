/**
 * Pure SVG-path builders for the painterly 2D hole (spec: web-hole-visualization). No JSX and no rendering —
 * these turn the hole's geometry into organic path `d` strings that the component fills. Deterministic when fed
 * a seeded RNG, so a hole's cosmetic shapes are reproducible. Ported from the approved biome prototypes in
 * `docs/course-holes/prototypes/`.
 */

import type { Point } from "@/lib/play/hole-geometry";

const clamp = (v: number, lo: number, hi: number) => Math.max(lo, Math.min(hi, v));
const f1 = (x: number) => x.toFixed(2);

/** A smooth closed curve through a ring of points (midpoint + quadratic technique). */
export function smoothClosed(pts: readonly Point[]): string {
  const n = pts.length;
  if (n < 3) return "";
  const mid = (a: Point, b: Point): Point => ({ x: (a.x + b.x) / 2, y: (a.y + b.y) / 2 });
  const start = mid(pts[0], pts[n - 1]);
  let d = `M${f1(start.x)} ${f1(start.y)}`;
  for (let i = 0; i < n; i++) {
    const cur = pts[i];
    const m = mid(cur, pts[(i + 1) % n]);
    d += ` Q${f1(cur.x)} ${f1(cur.y)} ${f1(m.x)} ${f1(m.y)}`;
  }
  return `${d} Z`;
}

export interface RibbonOpts {
  /** Taper only at the tee and hold full width into the green (so the fairway forms a continuous flow). */
  readonly holdTop?: boolean;
  /** Fraction range [t0, t1] of the spine to cover (for carry-split island segments). Defaults to the whole hole. */
  readonly t0?: number;
  readonly t1?: number;
  readonly samples?: number;
}

/**
 * An organic filled corridor of half-width `half` around the fairway spine, offset along the local normal (so it
 * tracks doglegs) with a little edge `jitter`. Tapers to rounded ends unless `holdTop` keeps full width at the green.
 */
export function ribbonPath(
  sample: (t: number) => Point,
  half: number,
  jitter: number,
  rng: () => number,
  opts: RibbonOpts = {},
): string {
  const { holdTop = false, t0 = 0, t1 = 1, samples = 24 } = opts;
  const left: Point[] = [];
  const right: Point[] = [];
  for (let i = 0; i <= samples; i++) {
    const t = t0 + (t1 - t0) * (i / samples);
    const p = sample(t);
    const a = sample(Math.max(0, t - 0.01));
    const b = sample(Math.min(1, t + 0.01));
    const dx = b.x - a.x;
    const dy = b.y - a.y;
    const len = Math.hypot(dx, dy) || 1;
    const nx = -dy / len;
    const ny = dx / len;
    const local = (t - t0) / (t1 - t0);
    const taper = holdTop
      ? Math.pow(clamp(local / 0.16, 0, 1), 0.6)
      : Math.pow(Math.sin(Math.PI * clamp(local, 0, 1)), 0.55);
    const hh = half * (0.3 + 0.7 * taper) + (rng() * 2 - 1) * jitter * taper;
    left.push({ x: p.x + nx * hh, y: p.y + ny * hh });
    right.push({ x: p.x - nx * hh, y: p.y - ny * hh });
  }
  return smoothClosed(left.concat(right.reverse()));
}

/** An organic blob (green, bunker, water, apron) — a lobed closed curve around a centre. */
export function blobPath(
  cx: number,
  cy: number,
  rx: number,
  ry: number,
  rng: () => number,
  lobes = 11,
  rough = 0.16,
  rot = 0,
): string {
  const pts: Point[] = [];
  for (let i = 0; i < lobes; i++) {
    const a = rot + (i / lobes) * Math.PI * 2;
    const rr = 1 + (rng() * 2 - 1) * rough;
    pts.push({ x: cx + Math.cos(a) * rx * rr, y: cy + Math.sin(a) * ry * rr });
  }
  return smoothClosed(pts);
}
