"use client";

import { useEffect, useId, useMemo, useRef, useState } from "react";

import { BIOME_KITS, resolveBiome, SYMBOL_SCALE, VEG_SYMBOLS, type SvgPrim } from "@/lib/play/biomes";
import { blobPath, ribbonPath } from "@/lib/play/hole-draw";
import {
  ballPosition,
  pointAt,
  projectHole,
  seededRng,
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
  if (p.t === "path") return <path d={p.d} fill={p.fill} />;
  if (p.t === "circle") return <circle r={p.r} fill={p.fill} />;
  return <path d={p.d} stroke={p.stroke} strokeWidth={p.width} fill="none" strokeLinecap="round" />;
}

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

  const { width, height, fairwayWidth, roughWidth, green, pin, water, bunkers, trees } = layout;
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
        {kit.mowStripe && (
          <pattern id={id("mow")} width="10" height="14" patternUnits="userSpaceOnUse">
            <rect width="10" height="7" fill={kit.mowStripe[0]} />
            <rect y="7" width="10" height="7" fill={kit.mowStripe[1]} />
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
        <g id={id("veg")}>
          {VEG_SYMBOLS[kit.vegetation].map((p, i) => (
            <Prim key={i} p={p} />
          ))}
        </g>
      </defs>

      {/* Out-of-play surround */}
      <rect x="4" y="0" width={width - 8} height={height} rx="14" fill={outFill} />
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
      {kit.rock && (
        <>
          <polygon points="8,20 44,10 66,34 34,46 8,40" fill="#546e7a" />
          <polygon points="8,20 44,10 52,24 22,32" fill="#78909c" />
          <polygon points={`${width - 8},26 ${width - 48},14 ${width - 62},40 ${width - 26},50`} fill="#455a64" />
        </>
      )}

      {/* Organic rough corridor + its apron around the green (a concentric green complex — the rough wraps the
          whole thing, so the green is never a bare head), then the striped fairway + its own apron. `holdTop`
          keeps the fairway full width into the green so it flows around it rather than pinching to a lollipop. */}
      <path d={ribbonPath(sample, roughWidth / 2, crinkle ? 7 : 4, seededRng(seed, "r"), { holdTop: true })} fill={roughFill} />
      <path d={blobPath(green.cx, green.cy, green.rx + 20, green.ry + 20, seededRng(seed, "rap"), 12, 0.1)} fill={roughFill} />
      {kit.waste && (
        <path d={ribbonPath(sample, fairwayWidth / 2 + 5, 3, seededRng(seed, "waste"), { holdTop: true })} fill="#eef0e4" opacity="0.35" />
      )}
      <path d={ribbonPath(sample, fairwayWidth / 2, crinkle ? 6 : 3.2, seededRng(seed, "f"), { holdTop: true })} fill={kit.fairway} />
      {kit.mowStripe && (
        <path d={ribbonPath(sample, fairwayWidth / 2, crinkle ? 6 : 3.2, seededRng(seed, "f"), { holdTop: true })} fill={url("mow")} opacity="0.45" />
      )}
      <path d={blobPath(green.cx, green.cy, green.rx + 9, green.ry + 9, seededRng(seed, "ap"), 12, 0.1)} fill={kit.fairway} />
      {kit.mowStripe && (
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

      {/* Vegetation */}
      {trees.map((t, i) => (
        <use
          key={i}
          href={`#${id("veg")}`}
          transform={`translate(${t.x.toFixed(1)} ${t.y.toFixed(1)}) scale(${(t.scale * SYMBOL_SCALE[kit.vegetation]).toFixed(3)})`}
          filter={kit.vegetation === "yucca" ? undefined : url("canopyShadow")}
        />
      ))}

      {/* Green complex — drawn after vegetation so trees never sit on the putting surface. Two organic layers
          (subtle fringe + green) that blend into the fairway apron: no dark ring, no sand collar. */}
      <path d={blobPath(green.cx, green.cy, green.rx + 3.2, green.ry + 3.2, seededRng(seed, "gf"), 12, 0.08)} fill={kit.fringe} />
      <path d={blobPath(green.cx, green.cy, green.rx, green.ry, seededRng(seed, "g2"), 12, 0.12)} fill={kit.green} />

      {/* Tee */}
      <rect x={layout.tee.x - 7} y={layout.tee.y - 3} width="14" height="6" rx="3" fill="#d8d2c0" opacity="0.85" />

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
