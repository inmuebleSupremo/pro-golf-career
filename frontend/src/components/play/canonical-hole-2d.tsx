"use client";

import { useEffect, useLayoutEffect, useMemo, useRef, useState, type PointerEvent } from "react";

import { BIOME_KITS, PARKLAND_MATERIAL, resolveBiome, VEG_SYMBOLS, type SvgPrim } from "@/lib/play/biomes";
import { canonicalRenderModel } from "@/lib/play/canonical-geometry";
import { decorationId, planTreeCanopies, type CanopyPlacement } from "@/lib/play/canonical-hole-decoration";
import type { Point } from "@/lib/play/hole-geometry";
import { feedbackAimPoint, isAimPointInteractionLocked, tracePlaybackDurationMs, tracePlaybackFrame, tracePlaybackStatus, transitionLabel } from "@/lib/play/trace-presentation";
import type { Hole2dProps } from "@/components/play/hole-2d";
import { WindDisplay } from "@/components/play/wind-display";

const W = 220;
const H = 440;
const PAD = 16;

const fills = (surface: string, kit: (typeof BIOME_KITS)[keyof typeof BIOME_KITS], artId: string) => {
  if (kit.biome === "parkland") {
    switch (surface) {
      case "FAIRWAY": return `url(#${artId}-fairway)`;
      case "FIRST_CUT": case "FRINGE": return `url(#${artId}-fringe)`;
      case "PRIMARY_ROUGH": case "DEEP_ROUGH": case "RECOVERY_AREA": return `url(#${artId}-rough)`;
      case "GREEN": return `url(#${artId}-green)`;
      case "BUNKER": case "WASTE_AREA": return `url(#${artId}-sand)`;
      case "WATER": return `url(#${artId}-water)`;
      case "TREES": return `url(#${artId}-trees)`;
      default: return "transparent";
    }
  }
  switch (surface) {
    case "FAIRWAY": return kit.fairway;
    case "FIRST_CUT": return kit.fringe;
    case "PRIMARY_ROUGH": return kit.rough;
    case "DEEP_ROUGH": return kit.out;
    case "GREEN": return kit.green;
    case "FRINGE": return kit.fringe;
    case "BUNKER": case "WASTE_AREA": return kit.sand;
    case "WATER": return kit.water ?? "#2f7fb5";
    case "TREES": return "#254d2b";
    case "RECOVERY_AREA": return kit.rough;
    default: return "transparent";
  }
};

function usePrefersReducedMotion(): boolean {
  const [reduced, setReduced] = useState(false);
  useLayoutEffect(() => {
    const query = window.matchMedia("(prefers-reduced-motion: reduce)");
    const sync = () => setReduced(query.matches);
    sync();
    query.addEventListener("change", sync);
    return () => query.removeEventListener("change", sync);
  }, []);
  return reduced;
}

/** Canonical renderer: terrain and every visible shot point come from backend yard-space facts. */
export function CanonicalHole2d({ hole, ball, className, aimPoint, onAimPoint, presentation = "polished" }: Hole2dProps) {
  const geometry = hole.geometry!;
  const kit = BIOME_KITS[resolveBiome(hole.courseType)];
  const model = useMemo(() => canonicalRenderModel(geometry, { width: W, height: H, padding: PAD }), [geometry]);
  const artId = useMemo(() => decorationId(hole.layoutSeed, geometry), [geometry, hole.layoutSeed]);
  const treeCanopies = useMemo(() => planTreeCanopies(geometry, hole.layoutSeed), [geometry, hole.layoutSeed]);
  const svgRef = useRef<SVGSVGElement>(null);
  const reduced = usePrefersReducedMotion();
  const trace = ball?.trace ?? null;
  const polishedParkland = kit.biome === "parkland" && presentation === "polished";
  const [progress, setProgress] = useState(1);

  useEffect(() => {
    if (!trace || reduced) {
      const frame = requestAnimationFrame(() => setProgress(1));
      return () => cancelAnimationFrame(frame);
    }
    let frame = 0;
    const start = (started: number) => {
      setProgress(0);
      const advance = (now: number) => {
      const next = Math.min(1, (now - started) / tracePlaybackDurationMs(trace));
      setProgress(next);
      if (next < 1) frame = requestAnimationFrame(advance);
      };
      frame = requestAnimationFrame(advance);
    };
    frame = requestAnimationFrame(start);
    return () => cancelAnimationFrame(frame);
  }, [trace, reduced]);

  const tee = model.project(geometry.tee);
  const cup = model.project(geometry.cup);
  const displayedAim = feedbackAimPoint(trace, aimPoint);
  const planningTarget = !trace && displayedAim ? model.project(displayedAim) : null;
  const traceTarget = trace ? model.project(displayedAim!) : null;
  const contact = trace ? model.project(trace.contact.position) : null;
  const finalPoint = trace ? model.project(trace.finalPoint) : model.project(hole.ball?.position ?? geometry.tee);
  const transitionFrom = trace?.transition ? model.project(trace.transition.from) : null;
  const transitionTo = trace?.transition ? model.project(trace.transition.to) : null;
  const rollFrom = trace?.roll ? model.project(trace.roll.from) : null;
  const rollTo = trace?.roll ? model.project(trace.roll.to) : null;
  const playback = trace ? tracePlaybackFrame(trace, progress) : null;
  const movingBall = playback && !playback.finalVisible ? model.project(playback.point) : null;
  const contactEffect = trace?.contact.surface === "WATER" ? (polishedParkland ? PARKLAND_MATERIAL.waterLight : "#dff1ff")
    : trace?.contact.surface === "BUNKER" ? kit.sandStroke : "#ffffff";
  const ballStroke = polishedParkland ? PARKLAND_MATERIAL.markerOutline : "#c99";
  const airbornePath = trace && contact
    ? (trace.airbornePath?.length ?? 0) > 0
      ? `M${trace.airbornePath!.map((point) => `${model.project(point.position).x} ${model.project(point.position).y}`).join(" L")}`
      : `M${model.project(trace.origin).x} ${model.project(trace.origin).y} L${contact.x} ${contact.y}`
    : null;
  const finalMatchesContact = trace != null && trace.finalPoint.x === trace.contact.position.x
    && trace.finalPoint.y === trace.contact.position.y;

  function selectTarget(event: PointerEvent<SVGSVGElement>) {
    if (!onAimPoint || !svgRef.current || isAimPointInteractionLocked(trace != null)) return;
    const rect = svgRef.current.getBoundingClientRect();
    onAimPoint(model.unproject({ x: (event.clientX - rect.left) * W / rect.width, y: (event.clientY - rect.top) * H / rect.height }));
  }

  const recoveryLabel = transitionLabel(trace);
  const playbackStatus = tracePlaybackStatus(trace, playback);
  return <svg ref={svgRef} viewBox={`0 0 ${W} ${H}`} className={className} role="img"
    aria-label={`Hole ${hole.holeNumber}, par ${hole.par}, canonical terrain${trace ? `, ${trace.club} shot result` : ""}`}
    onPointerDown={selectTarget}>
    {polishedParkland ? <ParklandDefs artId={artId} model={model} treeCanopies={treeCanopies} /> : null}
    <path d={model.playableBoundaryPath} fill={polishedParkland ? `url(#${artId}-out)` : kit.out} />
    {model.terrain.map((region, index) => <TerrainRegion key={`${region.surface}-${index}`} surface={region.surface}
      path={region.path} fill={fills(region.surface, kit, artId)} artId={artId} polishedParkland={polishedParkland} />)}
    {polishedParkland ? <ParklandCanopies artId={artId} model={model} treeCanopies={treeCanopies} /> : null}
    <path d={model.playableBoundaryPath} fill="none" stroke="rgba(255,255,255,.28)" strokeWidth="1" />
    <rect x={tee.x - 7} y={tee.y - 3} width="14" height="6" rx="2" fill={kit.tee}
      stroke={polishedParkland ? PARKLAND_MATERIAL.markerOutline : undefined} strokeWidth={polishedParkland ? ".7" : undefined} />
    <circle cx={cup.x} cy={cup.y} r="2" fill="#111" /><path d={`M${cup.x} ${cup.y} v-13 l7 2.5 -7 2.5z`} fill="#e23b3b" />
    <WindDisplay wind={hole.effectiveWind} model={model} tee={geometry.tee} />

    {traceTarget && contact ? <path d={`M${traceTarget.x} ${traceTarget.y} L${contact.x} ${contact.y}`} stroke="var(--accent)" strokeWidth="1" strokeDasharray="2 2" opacity=".75" /> : null}
    {traceTarget ? <TargetMarker point={traceTarget} label="Intended target" outlined={polishedParkland} /> : null}
    {planningTarget ? <TargetMarker point={planningTarget} label="Selected target" outlined={polishedParkland} /> : null}

    {trace && contact && airbornePath ? <>
      {polishedParkland ? <path d={airbornePath} stroke={PARKLAND_MATERIAL.traceShadow} strokeWidth="2.8" strokeDasharray="3 3" opacity=".5" /> : null}
      <path d={airbornePath} stroke="#ffffff" strokeWidth="1.1" strokeDasharray="3 3" opacity=".8" />
      {movingBall ? <circle cx={movingBall.x} cy={movingBall.y} r="3" fill="#fff" stroke={ballStroke} strokeWidth=".6" /> : null}
      <circle cx={contact.x} cy={contact.y} r="6" fill="none" stroke={contactEffect} strokeWidth="1.5" opacity=".9" />
      <circle cx={contact.x} cy={contact.y} r="2.4" fill="#fff" />
      <text x={contact.x + 5} y={contact.y - 5} fill="#fff" fontSize="5">{finalMatchesContact ? "Contact / ball" : "Contact"}</text>
    </> : null}

    {transitionFrom && transitionTo && recoveryLabel && (playback?.phase === "transition" || playback?.phase === "final") ? <>
      <path d={`M${transitionFrom.x} ${transitionFrom.y} L${transitionTo.x} ${transitionTo.y}`} stroke="#fff" strokeWidth="1.2" strokeDasharray="3 2" opacity=".9" />
      <text x={(transitionFrom.x + transitionTo.x) / 2 + 3} y={(transitionFrom.y + transitionTo.y) / 2 - 3} fill="#fff" fontSize="5">{recoveryLabel}</text>
    </> : null}

    {rollFrom && rollTo && (playback?.phase === "roll" || playback?.phase === "final") ? <>
      <path d={`M${rollFrom.x} ${rollFrom.y} L${rollTo.x} ${rollTo.y}`} stroke="var(--accent)" strokeWidth="1.3" strokeDasharray="2 1" opacity=".95" />
      <text x={(rollFrom.x + rollTo.x) / 2 + 3} y={(rollFrom.y + rollTo.y) / 2 - 3} fill="var(--accent)" fontSize="5">Roll</text>
    </> : null}

    {playbackStatus ? <text x={PAD} y={H - PAD} fill="var(--accent)" fontSize="6" fontWeight="700">
      {playbackStatus}
    </text> : null}

    {(!trace || playback?.finalVisible) && (!trace || !finalMatchesContact) ? <><circle cx={finalPoint.x} cy={finalPoint.y} r="3" fill="#fff" stroke={ballStroke} strokeWidth=".6" />
      {trace ? <text x={finalPoint.x + 5} y={finalPoint.y + 6} fill="#fff" fontSize="5">Ball</text> : null}</> : null}
  </svg>;
}

function TargetMarker({ point, label, outlined }: { point: Point; label: string; outlined: boolean }) {
  return <g aria-label={label}>{outlined ? <circle cx={point.x} cy={point.y} r="6.7" fill="none" stroke={PARKLAND_MATERIAL.traceShadow} strokeWidth="2.8" opacity=".55" /> : null}
    <circle cx={point.x} cy={point.y} r="6" fill="none" stroke="var(--accent)" strokeWidth="1.5" />
    <path d={`M${point.x - 9} ${point.y}h18M${point.x} ${point.y - 9}v18`} stroke="var(--accent)" strokeWidth="1" /></g>;
}

function TerrainRegion({
  surface, path, fill, artId, polishedParkland,
}: { surface: string; path: string; fill: string; artId: string; polishedParkland: boolean }) {
  const overlay = polishedParkland
    ? surface === "FAIRWAY" ? `${artId}-mow`
      : surface === "PRIMARY_ROUGH" || surface === "DEEP_ROUGH" || surface === "RECOVERY_AREA" ? `${artId}-grass`
        : surface === "BUNKER" || surface === "WASTE_AREA" ? `${artId}-sand-grain`
          : surface === "WATER" ? `${artId}-water-ripple`
            : null
    : null;
  return <g>
    <path d={path} fill={fill} />
    {overlay ? <path d={path} fill={`url(#${overlay})`} opacity={surface === "FAIRWAY" ? ".12" : ".3"} /> : null}
  </g>;
}

function ParklandDefs({
  artId, model, treeCanopies,
}: { artId: string; model: ReturnType<typeof canonicalRenderModel>; treeCanopies: ReturnType<typeof planTreeCanopies> }) {
  return <defs>
    <linearGradient id={`${artId}-out`} x1="0" y1="0" x2="0.8" y2="1"><stop stopColor={PARKLAND_MATERIAL.outerLight} /><stop offset="1" stopColor={PARKLAND_MATERIAL.outerDark} /></linearGradient>
    <linearGradient id={`${artId}-rough`} x1="0" y1="0" x2="0" y2="1"><stop stopColor={PARKLAND_MATERIAL.roughLight} /><stop offset="1" stopColor={PARKLAND_MATERIAL.roughDark} /></linearGradient>
    <linearGradient id={`${artId}-fairway`} x1="0" y1="0" x2="0" y2="1"><stop stopColor={PARKLAND_MATERIAL.fairwayLight} /><stop offset="1" stopColor={PARKLAND_MATERIAL.fairwayDark} /></linearGradient>
    <radialGradient id={`${artId}-green`} cx="42%" cy="34%"><stop stopColor={PARKLAND_MATERIAL.greenLight} /><stop offset="1" stopColor={PARKLAND_MATERIAL.greenDark} /></radialGradient>
    <linearGradient id={`${artId}-fringe`} x1="0" y1="0" x2="1" y2="1"><stop stopColor={PARKLAND_MATERIAL.fringeLight} /><stop offset="1" stopColor={PARKLAND_MATERIAL.fringeDark} /></linearGradient>
    <linearGradient id={`${artId}-sand`} x1="0" y1="0" x2="0.8" y2="1"><stop stopColor={PARKLAND_MATERIAL.sandLight} /><stop offset="1" stopColor={PARKLAND_MATERIAL.sandDark} /></linearGradient>
    <linearGradient id={`${artId}-water`} x1="0" y1="0" x2="0.2" y2="1"><stop stopColor={PARKLAND_MATERIAL.waterLight} /><stop offset="1" stopColor={PARKLAND_MATERIAL.waterDark} /></linearGradient>
    <linearGradient id={`${artId}-trees`} x1="0" y1="0" x2="0.9" y2="1"><stop stopColor={PARKLAND_MATERIAL.outerLight} /><stop offset="1" stopColor={PARKLAND_MATERIAL.treeBase} /></linearGradient>
    <pattern id={`${artId}-mow`} width="10" height="16" patternUnits="userSpaceOnUse"><rect width="10" height="8" fill={PARKLAND_MATERIAL.fairwayLight} /><rect y="8" width="10" height="8" fill={PARKLAND_MATERIAL.fairwayDark} /></pattern>
    <pattern id={`${artId}-grass`} width="9" height="11" patternUnits="userSpaceOnUse"><path d="M1 10 3 4M4 10 5 5M7 10 8 6" stroke={PARKLAND_MATERIAL.roughLight} strokeWidth=".55" opacity=".6" /></pattern>
    <pattern id={`${artId}-sand-grain`} width="7" height="7" patternUnits="userSpaceOnUse"><circle cx="1" cy="2" r=".45" fill={PARKLAND_MATERIAL.sandDark} /><circle cx="5" cy="5" r=".35" fill={PARKLAND_MATERIAL.sandDark} /></pattern>
    <pattern id={`${artId}-water-ripple`} width="13" height="10" patternUnits="userSpaceOnUse"><path d="M1 4 Q4 2 7 4 T13 4" fill="none" stroke={PARKLAND_MATERIAL.waterLight} strokeWidth=".55" opacity=".65" /></pattern>
    <filter id={`${artId}-canopy-shadow`} x="-40%" y="-40%" width="180%" height="180%"><feDropShadow dx="1" dy="1.6" stdDeviation="1.2" floodColor={PARKLAND_MATERIAL.treeOutline} floodOpacity=".55" /></filter>
    {treeCanopies.map((plan) => <clipPath key={plan.regionIndex} id={`${artId}-tree-clip-${plan.regionIndex}`}><path d={model.terrain[plan.regionIndex].path} /></clipPath>)}
  </defs>;
}

function ParklandCanopies({
  artId, model, treeCanopies,
}: { artId: string; model: ReturnType<typeof canonicalRenderModel>; treeCanopies: ReturnType<typeof planTreeCanopies> }) {
  return <>{treeCanopies.map((plan) => <g key={plan.regionIndex} clipPath={`url(#${artId}-tree-clip-${plan.regionIndex})`} filter={`url(#${artId}-canopy-shadow)`}>
    {plan.placements.map((placement, index) => <DeciduousCanopy key={index} point={model.project(placement)} placement={placement} />)}
  </g>)}</>;
}

function DeciduousCanopy({ point, placement }: { point: Point; placement: CanopyPlacement }) {
  const variants = placement.variant === 0 ? VEG_SYMBOLS.deciduous : [...VEG_SYMBOLS.deciduous].reverse();
  return <g transform={`translate(${point.x} ${point.y}) scale(${placement.scale})`}>
    {variants.map((primitive, index) => <SvgPrimitive key={index} primitive={primitive} />)}
  </g>;
}

function SvgPrimitive({ primitive }: { primitive: SvgPrim }) {
  if (primitive.t === "path") return <path d={primitive.d} fill={primitive.fill} opacity={primitive.opacity} />;
  if (primitive.t === "circle") return <circle cx={primitive.cx ?? 0} cy={primitive.cy ?? 0} r={primitive.r} fill={primitive.fill} opacity={primitive.opacity} />;
  if (primitive.t === "ellipse") return <ellipse cx={primitive.cx ?? 0} cy={primitive.cy ?? 0} rx={primitive.rx} ry={primitive.ry} fill={primitive.fill} opacity={primitive.opacity} />;
  return <path d={primitive.d} fill="none" stroke={primitive.stroke} strokeWidth={primitive.width} />;
}
