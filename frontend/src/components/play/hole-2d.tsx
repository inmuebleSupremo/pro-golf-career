"use client";

import { useEffect, useId, useMemo, useRef, useState } from "react";

import { BIOME_KITS, resolveBiome, SYMBOL_SCALE, VEG_SYMBOLS, type SvgPrim, type Vegetation } from "@/lib/play/biomes";
import { blobPath, ribbonPath } from "@/lib/play/hole-draw";
import {
  ballPosition,
  coastShoreX,
  pointAt,
  projectHole,
  seededRng,
  wallEdgeX,
  type HoleGeom,
  type HoleLayout,
  type Point,
  type ResolvedShot,
} from "@/lib/play/hole-geometry";

/** A landing reaction's ring colour per surface effect. */
const REACTION_COLOR: Record<"splash" | "sand" | "roll", string> = {
  splash: "#dff1ff",
  sand: "#efe3bf",
  roll: "#ffffff",
};

const FLIGHT_MS = 1000;
const REACTION_MS = 500;

/** Tropical island rim colours (beach sand + turquoise shallows). */
const BEACH = "#f4eccf";
const SHALLOW = "#4bc6e0";

/** True when the viewer asked for reduced motion — playback then places the ball without animating. */
function usePrefersReducedMotion(): boolean {
  const [reduced, setReduced] = useState(false);
  useEffect(() => {
    const query = window.matchMedia("(prefers-reduced-motion: reduce)");
    const sync = () => setReduced(query.matches);
    sync();
    query.addEventListener("change", sync);
    return () => query.removeEventListener("change", sync);
  }, []);
  return reduced;
}

/** A gently bowed flight arc from `a` to `b` (the bow suggests height in the top-down view). */
function arcPath(a: Point, b: Point): string {
  const mx = (a.x + b.x) / 2;
  const my = (a.y + b.y) / 2;
  const len = Math.hypot(b.x - a.x, b.y - a.y) || 1;
  const lift = Math.min(30, len * 0.2);
  const cx = mx - ((b.y - a.y) / len) * lift * 0.4;
  const cy = my - lift;
  return `M${a.x.toFixed(1)} ${a.y.toFixed(1)} Q ${cx.toFixed(1)} ${cy.toFixed(1)} ${b.x.toFixed(1)} ${b.y.toFixed(1)}`;
}

export interface Hole2dProps {
  readonly hole: HoleGeom;
  /** The last resolved shot, placed on the hole under the fidelity rule (animated by the playback layer). */
  readonly ball?: ResolvedShot | null;
  readonly className?: string;
}

/** One primitive of a ported foliage symbol. */
function Prim({ p }: { p: SvgPrim }) {
  if (p.t === "path") return <path d={p.d} fill={p.fill} opacity={p.opacity} />;
  if (p.t === "circle") return <circle cx={p.cx ?? 0} cy={p.cy ?? 0} r={p.r} fill={p.fill} opacity={p.opacity} />;
  if (p.t === "ellipse") return <ellipse cx={p.cx ?? 0} cy={p.cy ?? 0} rx={p.rx} ry={p.ry} fill={p.fill} opacity={p.opacity} />;
  return <path d={p.d} stroke={p.stroke} strokeWidth={p.width} fill="none" strokeLinecap="round" />;
}

/** Scatter kinds that stand tall enough to cast a canopy shadow (vs. low brush/rock). */
const CANOPY = new Set<Vegetation>(["deciduous", "pine", "palm", "birch", "saguaro"]);

/**
 * The parametric 2D hole schematic (spec: web-hole-visualization): draws a hole purely from its sim geometry
 * and biome kit — rough corridor, striped fairway, green, tee, seed-placed hazards and vegetation, and the pin
 * on its real side — with an optional truthful reach overlay and resolved-ball marker. Illustration colours
 * come from the biome kit; the surrounding chrome uses the app's tokens.
 */
export function Hole2d({ hole, ball, className }: Hole2dProps) {
  const kit = BIOME_KITS[resolveBiome(hole.courseType)];
  const layout: HoleLayout = useMemo(() => projectHole(hole, kit), [hole, kit]);

  // Namespace all defs ids so multiple instances never collide (SVG-safe: strip non-alphanumerics).
  const uid = useId().replace(/[^a-zA-Z0-9]/g, "");
  const id = (name: string) => `${uid}-${name}`;
  const url = (name: string) => `url(#${id(name)})`;

  const { width, height, fairwayWidth, roughWidth, green, pin, water, bunkers, scatter } = layout;
  const scatterKinds = Array.from(new Set(kit.scatter.map((s) => s.kind)));
  const roughFill = kit.roughPattern ? url(kit.roughPattern) : kit.rough;
  // Only links' fescue tiles the whole surround; heathland keeps a solid khaki frame with heather only in the rough.
  const outFill = kit.roughPattern === "fescue" ? url("fescue") : kit.out;

  // Samples the cubic fairway spine for the organic ribbon/scatter layers. A fresh seeded RNG per shape keeps
  // each hole's cosmetic edges reproducible.
  const sample = (t: number) => pointAt(layout, t);
  const seed = hole.layoutSeed;
  const crinkle = kit.roughPattern === "fescue"; // links reads the crinkliest

  // Playback: when a new resolved shot arrives, animate the ball from its previous rest (or the tee) to the
  // fidelity-resolved landing. Refs track the last rest and hole so each new shot replays once.
  const reduced = usePrefersReducedMotion();
  const lastRest = useRef<Point | null>(null);
  const lastHole = useRef(hole.holeNumber);
  const lastBall = useRef<ResolvedShot | null>(null);
  const seq = useRef(0);
  const [flight, setFlight] = useState<{ from: Point; to: Point; effect: "splash" | "sand" | "roll"; holed: boolean; seq: number } | null>(null);
  const [animating, setAnimating] = useState(false);

  useEffect(() => {
    if (lastHole.current !== hole.holeNumber) {
      lastHole.current = hole.holeNumber;
      lastRest.current = null;
      lastBall.current = null;
      setFlight(null);
      setAnimating(false);
    }
    if (!ball || ball === lastBall.current) return;
    lastBall.current = ball;
    const to = ballPosition(layout, ball);
    const from = lastRest.current ?? layout.tee;
    lastRest.current = { x: to.x, y: to.y };
    seq.current += 1;
    setFlight({ from, to, effect: to.effect, holed: to.holed ?? false, seq: seq.current });
    if (reduced) return;
    setAnimating(true);
    const timer = setTimeout(() => setAnimating(false), FLIGHT_MS + REACTION_MS);
    return () => clearTimeout(timer);
  }, [ball, hole.holeNumber, layout, reduced]);

  // The ball at rest — shown once the flight lands (or immediately under reduced motion). A holed shot has no
  // resting ball: it is in the cup.
  const rest = flight && !flight.holed && (!animating || reduced) ? flight.to : null;

  return (
    <svg
      viewBox={`0 0 ${width} ${height}`}
      className={className}
      role="img"
      aria-label={`Hole ${hole.holeNumber}, par ${hole.par}, ${Math.round(hole.length)} yards`}
      preserveAspectRatio="xMidYMid meet"
    >
      <defs>
        <filter id={id("canopyShadow")} x="-40%" y="-40%" width="180%" height="180%">
          <feDropShadow dx="1.4" dy="2.2" stdDeviation="1.6" floodColor="#0e3a11" floodOpacity="0.55" />
        </filter>
        <filter id={id("waterShadow")} x="-30%" y="-30%" width="160%" height="160%">
          <feDropShadow dx="0" dy="1.5" stdDeviation="1.4" floodColor="#001833" floodOpacity="0.5" />
        </filter>
        {kit.mowKind === "horizontal" && (
          <pattern id={id("mow")} width="10" height="14" patternUnits="userSpaceOnUse">
            <rect width="10" height="7" fill="rgba(255,255,255,0.10)" />
            <rect y="7" width="10" height="7" fill="rgba(0,0,0,0.06)" />
          </pattern>
        )}
        {kit.mowKind === "diagonal" && (
          <pattern id={id("mow")} width="14" height="14" patternUnits="userSpaceOnUse" patternTransform="rotate(34)">
            <rect width="14" height="7" fill="rgba(255,255,255,0.12)" />
            <rect y="7" width="14" height="7" fill="rgba(0,0,0,0.07)" />
          </pattern>
        )}
        {kit.mowKind === "premium" && (
          <pattern id={id("mow")} width="14" height="18" patternUnits="userSpaceOnUse">
            <rect width="14" height="9" fill="rgba(255,255,255,0.14)" />
            <rect y="9" width="14" height="9" fill="rgba(0,0,0,0.08)" />
          </pattern>
        )}
        {kit.roughPattern === "fescue" && (
          <pattern id={id("fescue")} width="16" height="16" patternUnits="userSpaceOnUse">
            <rect width="16" height="16" fill={kit.rough} />
            <path d="M3 13 L4 8 M5 13 L7 6 M9 13 L11 8 M12 13 L13 7" stroke="#9c8a52" strokeWidth="0.9" strokeLinecap="round" />
          </pattern>
        )}
        {kit.roughPattern === "heather" && (
          // Sparse heather flecks over the olive rough — small purple/green arcs and buds (docs/course-holes/heathland001.svg).
          <pattern id={id("heather")} width="22" height="22" patternUnits="userSpaceOnUse">
            <rect width="22" height="22" fill={kit.rough} />
            <path d="M4 6 Q6 2 8 6" stroke="#6a1b9a" strokeWidth="1.1" fill="none" opacity="0.5" strokeLinecap="round" />
            <path d="M15 16 Q17 12 19 16" stroke="#4a235a" strokeWidth="1.1" fill="none" opacity="0.4" strokeLinecap="round" />
            <path d="M9 18 Q11 15 13 18" stroke="#3f6b32" strokeWidth="1.1" fill="none" opacity="0.5" strokeLinecap="round" />
            <circle cx="5" cy="6" r="0.9" fill="#8e44ad" opacity="0.55" />
            <circle cx="18" cy="9" r="1" fill="#2e7d32" opacity="0.5" />
          </pattern>
        )}
        {scatterKinds.map((k) => (
          <g key={k} id={id(`sym-${k}`)}>
            {VEG_SYMBOLS[k].map((p, i) => (
              <Prim key={i} p={p} />
            ))}
          </g>
        ))}
        {kit.dryGreen && (
          <radialGradient id={id("dryGreen")} cx="50%" cy="44%" r="60%">
            <stop offset="0%" stopColor="#4f8a34" />
            <stop offset="72%" stopColor="#71a84a" />
            <stop offset="100%" stopColor="#bcd28a" />
          </radialGradient>
        )}
        {kit.grain && (
          <pattern id={id("grain")} width="9" height="9" patternUnits="userSpaceOnUse">
            <circle cx="2" cy="3" r="0.6" fill="#cbb489" />
            <circle cx="6" cy="7" r="0.5" fill="#cbb489" />
            <circle cx="7" cy="2" r="0.45" fill="#cbb489" />
          </pattern>
        )}
        {kit.ocean && (
          <pattern id={id("waves")} width="46" height="24" patternUnits="userSpaceOnUse">
            <path d="M0 12 Q11 7 23 12 T46 12" stroke="#6fd6ec" strokeWidth="1" fill="none" opacity="0.16" />
            <path d="M-23 22 Q-11 17 0 22 T23 22" stroke="#6fd6ec" strokeWidth="1" fill="none" opacity="0.12" />
          </pattern>
        )}
      </defs>

      {/* Out-of-play surround (ocean for tropical) + faint wave texture */}
      <rect x="4" y="0" width={width - 8} height={height} rx="14" fill={outFill} />
      {kit.ocean && <rect x="4" y="0" width={width - 8} height={height} rx="14" fill={url("waves")} />}
      {layout.elevation && (
        <rect
          x="4"
          y="0"
          width={width - 8}
          height={height}
          rx="14"
          fill={layout.elevation.light ? "#ffffff" : "#000000"}
          opacity={layout.elevation.alpha}
        />
      )}
      {/* Mountain: slate cliffs framing the valley (base + lit facet + striations + snow ridge + scree) */}
      {layout.walls &&
        (() => {
          const w = layout.walls;
          const wr = seededRng(seed, "wall");
          const wall = (side: -1 | 1) => {
            const outer = side < 0 ? 3 : width - 3;
            let base = `M ${outer} 0 L ${outer} ${height}`;
            for (let y = height; y >= 0; y -= 18) base += ` L ${wallEdgeX(w, side, y).toFixed(1)} ${y}`;
            let fac = `M ${outer} 40`;
            for (let y = 60; y <= height; y += 26) fac += ` L ${(wallEdgeX(w, side, y) - 8).toFixed(1)} ${y}`;
            fac += ` L ${outer} ${height} Z`;
            const stri: string[] = [];
            for (let y = 30; y < height; y += 46) {
              const x0 = side < 0 ? outer : wallEdgeX(w, side, y);
              const x1 = side < 0 ? wallEdgeX(w, side, y) : outer;
              stri.push(`M${x0.toFixed(1)} ${y} L${x1.toFixed(1)} ${(y + 16).toFixed(1)}`);
            }
            let snow = `M ${wallEdgeX(w, side, 0).toFixed(1)} 0`;
            for (let y = 12; y <= 150; y += 16) snow += ` L ${(wallEdgeX(w, side, y) - 2).toFixed(1)} ${y}`;
            for (let y = 150; y >= 0; y -= 16) snow += ` L ${(wallEdgeX(w, side, y) - 9 - Math.sin(y * 0.1) * 4).toFixed(1)} ${y}`;
            const scree: [number, number, number][] = [];
            for (let i = 0; i < 7; i++) {
              const y = 60 + wr() * 340;
              scree.push([wallEdgeX(w, side, y) + 2 + wr() * 6, y, 1 + wr() * 1.6]);
            }
            return (
              <g key={side}>
                <path d={`${base} Z`} fill="#4f606b" />
                <path d={fac} fill={side < 0 ? "#748592" : "#37474f"} opacity="0.5" />
                {stri.map((d, i) => (
                  <path key={i} d={d} stroke="#2f3d45" strokeWidth="1" opacity="0.35" />
                ))}
                <path d={`${snow} Z`} fill="#eef3f6" opacity="0.9" />
                {scree.map(([x, y, r], i) => (
                  <circle key={i} cx={x.toFixed(1)} cy={y.toFixed(1)} r={r.toFixed(1)} fill="#6b7480" opacity="0.7" />
                ))}
              </g>
            );
          };
          return (
            <>
              {wall(-1)}
              {wall(1)}
              {kit.waterfall &&
                (() => {
                  const wf = seededRng(seed, "waterfall");
                  if (wf() < 0.5) return null; // roughly half the holes carry a cliff waterfall
                  const side: -1 | 1 = wf() < 0.5 ? -1 : 1;
                  const fx = (y: number) => wallEdgeX(w, side, y) + (side < 0 ? -2 : 2);
                  let fall = `M ${(fx(20) - 3).toFixed(1)} 20`;
                  for (let y = 30; y <= 250; y += 14) fall += ` L ${(fx(y) - 3 + Math.sin(y * 0.3) * 1.5).toFixed(1)} ${y}`;
                  for (let y = 250; y >= 20; y -= 14) fall += ` L ${(fx(y) + 3 + Math.sin(y * 0.3) * 1.5).toFixed(1)} ${y}`;
                  return (
                    <g>
                      <ellipse cx={fx(252).toFixed(1)} cy="258" rx="15" ry="7" fill="#3b86ab" />
                      <path d={`${fall} Z`} fill="#eef6fb" opacity="0.92" />
                      <ellipse cx={fx(252).toFixed(1)} cy="256" rx="12" ry="5" fill="#eef6fb" opacity="0.45" />
                    </g>
                  );
                })()}
            </>
          );
        })()}
      {/* Desert: sand grain + a meandering dry wash (arroyo) across the surround */}
      {kit.grain && <rect x="4" y="0" width={width - 8} height={height} rx="14" fill={url("grain")} opacity="0.5" />}
      {kit.arroyo &&
        (() => {
          const ar = seededRng(seed, "arroyo");
          const yy = 120 + ar() * 220;
          let d = `M4 ${yy.toFixed(0)}`;
          for (let x = 20; x <= width; x += 24) d += ` L ${x} ${(yy + Math.sin(x * 0.05 + yy) * 14).toFixed(1)}`;
          return (
            <>
              <path d={d} fill="none" stroke="#d3bd8b" strokeWidth="13" strokeLinecap="round" opacity="0.7" />
              <path d={d} fill="none" stroke="#e7d6ac" strokeWidth="5" strokeLinecap="round" opacity="0.6" />
            </>
          );
        })()}

      {/* Links coastal margin — the sea down one edge with a pale beach and foam line */}
      {layout.coast !== 0 &&
        (() => {
          const side = layout.coast as -1 | 1;
          const edge = side < 0 ? 3 : width - 3;
          let sp = `M ${coastShoreX(side, 0).toFixed(1)} 0`;
          for (let y = 20; y <= height; y += 20) sp += ` L ${coastShoreX(side, y).toFixed(1)} ${y}`;
          let sea = `M ${edge} 0 L ${edge} ${height}`;
          for (let y = height; y >= 0; y -= 20) sea += ` L ${coastShoreX(side, y).toFixed(1)} ${y}`;
          return (
            <>
              <path d={`${sea} Z`} fill="#5a86a0" />
              <path d={sp} fill="none" stroke="#ece0c0" strokeWidth="12" strokeLinecap="round" />
              <path d={sp} fill="none" stroke="#eef6f7" strokeWidth="1.4" opacity="0.6" />
            </>
          );
        })()}

      {/* Organic rough corridor + its apron around the green (a concentric green complex — the rough wraps the
          whole thing, so the green is never a bare head), then the striped fairway + its own apron. `holdTop`
          keeps the fairway full width into the green so it flows around it rather than pinching to a lollipop. */}
      {/* Tropical island rims — turquoise shallows then a white beach ring the land (corridor + green complex) */}
      {kit.island && (
        <>
          <path d={ribbonPath(sample, roughWidth / 2 + 10, 2, seededRng(seed, "sh"), { holdTop: true })} fill={SHALLOW} />
          <path d={ribbonPath(sample, roughWidth / 2 + 5, 2, seededRng(seed, "be"), { holdTop: true })} fill={BEACH} />
          <path d={blobPath(green.cx, green.cy, green.rx + 30, green.ry + 30, seededRng(seed, "shg"), 12, 0.1)} fill={SHALLOW} />
          <path d={blobPath(green.cx, green.cy, green.rx + 25, green.ry + 25, seededRng(seed, "beg"), 12, 0.1)} fill={BEACH} />
        </>
      )}
      <path d={ribbonPath(sample, roughWidth / 2, crinkle ? 7 : 4, seededRng(seed, "r"), { holdTop: true })} fill={roughFill} />
      <path d={blobPath(green.cx, green.cy, green.rx + 20, green.ry + 20, seededRng(seed, "rap"), 12, 0.1)} fill={roughFill} />
      {kit.waste && (
        <path d={ribbonPath(sample, fairwayWidth / 2 + 5, 3, seededRng(seed, "waste"), { holdTop: true })} fill="#eef0e4" opacity="0.35" />
      )}
      <path d={ribbonPath(sample, fairwayWidth / 2, crinkle ? 6 : 3.2, seededRng(seed, "f"), { holdTop: true })} fill={kit.fairway} />
      {kit.mowKind && (
        <path d={ribbonPath(sample, fairwayWidth / 2, crinkle ? 6 : 3.2, seededRng(seed, "f"), { holdTop: true })} fill={url("mow")} opacity="0.45" />
      )}
      <path d={blobPath(green.cx, green.cy, green.rx + 9, green.ry + 9, seededRng(seed, "ap"), 12, 0.1)} fill={kit.fairway} />
      {kit.mowKind && (
        <path d={blobPath(green.cx, green.cy, green.rx + 9, green.ry + 9, seededRng(seed, "ap"), 12, 0.1)} fill={url("mow")} opacity="0.45" />
      )}

      {/* Water hazard */}
      {water && <path d={water.d} fill={kit.water ?? "#2f7fb5"} filter={url("waterShadow")} />}

      {/* Bunkers — organic sand blobs; pot bunkers stay small and ringed */}
      {bunkers.map((b, i) =>
        b.pot ? (
          <ellipse key={i} cx={b.cx} cy={b.cy} rx={b.rx} ry={b.ry} fill={kit.sand} stroke={kit.sandStroke} strokeWidth="2" />
        ) : (
          <path
            key={i}
            d={blobPath(b.cx, b.cy, b.rx, b.ry, seededRng(seed, `b${i}`), 9, 0.24)}
            fill={kit.sand}
            stroke={kit.sandStroke}
            strokeWidth="0.9"
          />
        ),
      )}

      {/* Scatter — biome plant/rock family, over the bunkers and under the green (drawn next) */}
      {scatter.map((t, i) => (
        <use
          key={i}
          href={`#${id(`sym-${t.kind}`)}`}
          transform={`translate(${t.x.toFixed(1)} ${t.y.toFixed(1)}) scale(${(t.scale * SYMBOL_SCALE[t.kind]).toFixed(3)})`}
          filter={CANOPY.has(t.kind) ? url("canopyShadow") : undefined}
        />
      ))}

      {/* Green complex — drawn after vegetation so trees never sit on the putting surface. Two organic layers
          (subtle fringe + green) that blend into the fairway apron: no dark ring, no sand collar. */}
      <path d={blobPath(green.cx, green.cy, green.rx + 3.2, green.ry + 3.2, seededRng(seed, "gf"), 12, 0.08)} fill={kit.fringe} />
      <path
        d={blobPath(green.cx, green.cy, green.rx, green.ry, seededRng(seed, "g2"), 12, 0.12)}
        fill={kit.dryGreen ? url("dryGreen") : kit.green}
      />
      {/* Mountain tiered green — a contour ring + a raised, offset upper tier to read the elevation change */}
      {kit.tiered && (
        <>
          <path
            d={blobPath(green.cx, green.cy, green.rx * 0.7, green.ry * 0.7, seededRng(seed, "gc"), 12, 0.14)}
            fill="none"
            stroke="#5aa863"
            strokeWidth="0.8"
            opacity="0.5"
          />
          <path
            d={blobPath(green.cx + green.rx * 0.24, green.cy - green.ry * 0.22, green.rx * 0.5, green.ry * 0.46, seededRng(seed, "g3"), 9, 0.16)}
            fill="#96d79a"
            opacity="0.5"
          />
        </>
      )}

      {/* Tee — a turf pad with a pair of markers in this course's signature colour */}
      <rect x={layout.tee.x - 8} y={layout.tee.y - 4} width="16" height="8" rx="2.5" fill="#3f5730" opacity="0.9" />
      <rect x={layout.tee.x - 6} y={layout.tee.y - 2.4} width="5" height="4.8" rx="1.6" fill={kit.tee} stroke="#0d2010" strokeWidth="0.4" />
      <rect x={layout.tee.x + 1} y={layout.tee.y - 2.4} width="5" height="4.8" rx="1.6" fill={kit.tee} stroke="#0d2010" strokeWidth="0.4" />

      {/* Pin (flag on its real side) */}
      <circle cx={pin.x} cy={pin.y} r="2" fill="#111111" />
      <line x1={pin.x} y1={pin.y} x2={pin.x} y2={pin.y - 14} stroke="#111111" strokeWidth="1" />
      <path d={`M${pin.x} ${pin.y - 14} l7 2.5 -7 2.5 z`} fill="#e23b3b" />

      {/* Shot playback: flight arc + trail + travelling ball + landing reaction (skipped for reduced motion) */}
      {flight && animating && !reduced && (
        <g key={flight.seq}>
          <path id={id(`flight${flight.seq}`)} d={arcPath(flight.from, flight.to)} fill="none" stroke="#ffffff" strokeWidth="1.4" strokeDasharray="3 3" opacity="0">
            <animate attributeName="stroke-dashoffset" from="140" to="0" dur={`${FLIGHT_MS}ms`} begin="0s" fill="freeze" />
            <animate attributeName="opacity" from="0" to="0.85" dur="200ms" begin="0s" fill="freeze" />
          </path>
          <circle r="3" fill="#ffffff" stroke="#c99" strokeWidth="0.6">
            <animateMotion dur={`${FLIGHT_MS}ms`} begin="0s" fill="freeze">
              <mpath href={`#${id(`flight${flight.seq}`)}`} />
            </animateMotion>
            <animate attributeName="r" values="3;4.4;3" dur={`${FLIGHT_MS}ms`} begin="0s" fill="freeze" />
            {/* Holed: after landing on the cup, the ball drops in (shrinks to nothing over the pin). */}
            {flight.holed && (
              <animate attributeName="r" from="3" to="0" dur={`${REACTION_MS}ms`} begin={`${FLIGHT_MS}ms`} fill="freeze" />
            )}
          </circle>
          {flight.holed ? (
            // Hole-out celebration: two success rings ripple out from the cup as the ball drops.
            <>
              <circle cx={flight.to.x} cy={flight.to.y} r="0" fill="none" stroke="#ffffff" strokeWidth="1.6" opacity="0">
                <animate attributeName="r" values="1;12" dur={`${REACTION_MS}ms`} begin={`${FLIGHT_MS}ms`} fill="freeze" />
                <animate attributeName="opacity" values="0.95;0" dur={`${REACTION_MS}ms`} begin={`${FLIGHT_MS}ms`} fill="freeze" />
              </circle>
              <circle cx={flight.to.x} cy={flight.to.y} r="0" fill="none" stroke="#ffffff" strokeWidth="1" opacity="0">
                <animate attributeName="r" values="1;18" dur={`${REACTION_MS}ms`} begin={`${FLIGHT_MS + 120}ms`} fill="freeze" />
                <animate attributeName="opacity" values="0.7;0" dur={`${REACTION_MS}ms`} begin={`${FLIGHT_MS + 120}ms`} fill="freeze" />
              </circle>
            </>
          ) : (
            <circle cx={flight.to.x} cy={flight.to.y} r="0" fill="none" stroke={REACTION_COLOR[flight.effect]} strokeWidth="1.6" opacity="0">
              <animate attributeName="r" values="0;10" dur={`${REACTION_MS}ms`} begin={`${FLIGHT_MS}ms`} fill="freeze" />
              <animate attributeName="opacity" values="0.9;0" dur={`${REACTION_MS}ms`} begin={`${FLIGHT_MS}ms`} fill="freeze" />
            </circle>
          )}
        </g>
      )}

      {/* Ball at rest */}
      {rest && (
        <>
          {flight?.effect === "splash" && (
            <circle cx={rest.x} cy={rest.y} r="6" fill="none" stroke="#ffffff" strokeWidth="1.4" opacity="0.7" />
          )}
          <circle cx={rest.x} cy={rest.y} r="3" fill="#ffffff" stroke="#c99" strokeWidth="0.6" />
        </>
      )}
    </svg>
  );
}
