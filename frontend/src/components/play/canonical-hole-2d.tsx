"use client";

import { useEffect, useMemo, useRef, useState, type PointerEvent } from "react";

import { BIOME_KITS, resolveBiome } from "@/lib/play/biomes";
import { canonicalRenderModel } from "@/lib/play/canonical-geometry";
import type { Point } from "@/lib/play/hole-geometry";
import { feedbackAimPoint, interpolatedContactPoint, transitionLabel } from "@/lib/play/trace-presentation";
import type { Hole2dProps } from "@/components/play/hole-2d";

const W = 220;
const H = 440;
const PAD = 16;
const PLAYBACK_MS = 650;

const fills = (surface: string, kit: (typeof BIOME_KITS)[keyof typeof BIOME_KITS]) => {
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
  useEffect(() => {
    const query = window.matchMedia("(prefers-reduced-motion: reduce)");
    const sync = () => setReduced(query.matches);
    sync();
    query.addEventListener("change", sync);
    return () => query.removeEventListener("change", sync);
  }, []);
  return reduced;
}

/** Canonical renderer: terrain and every visible shot point come from backend yard-space facts. */
export function CanonicalHole2d({ hole, ball, className, aimPoint, onAimPoint }: Hole2dProps) {
  const geometry = hole.geometry!;
  const kit = BIOME_KITS[resolveBiome(hole.courseType)];
  const model = useMemo(() => canonicalRenderModel(geometry, { width: W, height: H, padding: PAD }), [geometry]);
  const svgRef = useRef<SVGSVGElement>(null);
  const reduced = usePrefersReducedMotion();
  const trace = ball?.trace ?? null;
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
      const next = Math.min(1, (now - started) / PLAYBACK_MS);
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
  const movingBall = trace && progress < 1
    ? model.project(interpolatedContactPoint(trace, progress))
    : null;
  const contactEffect = trace?.contact.surface === "WATER" ? "#dff1ff"
    : trace?.contact.surface === "BUNKER" ? kit.sandStroke : "#ffffff";
  const finalMatchesContact = trace != null && trace.finalPoint.x === trace.contact.position.x
    && trace.finalPoint.y === trace.contact.position.y;

  function selectTarget(event: PointerEvent<SVGSVGElement>) {
    if (!onAimPoint || !svgRef.current || trace) return;
    const rect = svgRef.current.getBoundingClientRect();
    onAimPoint(model.unproject({ x: (event.clientX - rect.left) * W / rect.width, y: (event.clientY - rect.top) * H / rect.height }));
  }

  const recoveryLabel = transitionLabel(trace);
  return <svg ref={svgRef} viewBox={`0 0 ${W} ${H}`} className={className} role="img"
    aria-label={`Hole ${hole.holeNumber}, par ${hole.par}, canonical terrain${trace ? `, ${trace.club} shot result` : ""}`}
    onPointerDown={selectTarget}>
    <path d={model.playableBoundaryPath} fill={kit.out} />
    {model.terrain.map((region, index) => <path key={`${region.surface}-${index}`} d={region.path} fill={fills(region.surface, kit)} />)}
    <path d={model.playableBoundaryPath} fill="none" stroke="rgba(255,255,255,.28)" strokeWidth="1" />
    <rect x={tee.x - 7} y={tee.y - 3} width="14" height="6" rx="2" fill={kit.tee} />
    <circle cx={cup.x} cy={cup.y} r="2" fill="#111" /><path d={`M${cup.x} ${cup.y} v-13 l7 2.5 -7 2.5z`} fill="#e23b3b" />

    {traceTarget && contact ? <path d={`M${traceTarget.x} ${traceTarget.y} L${contact.x} ${contact.y}`} stroke="var(--accent)" strokeWidth="1" strokeDasharray="2 2" opacity=".75" /> : null}
    {traceTarget ? <TargetMarker point={traceTarget} label="Intended target" /> : null}
    {planningTarget ? <TargetMarker point={planningTarget} label="Selected target" /> : null}

    {trace && contact ? <>
      <path d={`M${model.project(trace.origin).x} ${model.project(trace.origin).y} L${contact.x} ${contact.y}`} stroke="#ffffff" strokeWidth="1.1" strokeDasharray="3 3" opacity=".65" />
      {movingBall ? <circle cx={movingBall.x} cy={movingBall.y} r="3" fill="#fff" stroke="#c99" strokeWidth=".6" /> : null}
      <circle cx={contact.x} cy={contact.y} r="6" fill="none" stroke={contactEffect} strokeWidth="1.5" opacity=".9" />
      <circle cx={contact.x} cy={contact.y} r="2.4" fill="#fff" />
      <text x={contact.x + 5} y={contact.y - 5} fill="#fff" fontSize="5">{finalMatchesContact ? "Contact / ball" : "Contact"}</text>
    </> : null}

    {transitionFrom && transitionTo && recoveryLabel ? <>
      <path d={`M${transitionFrom.x} ${transitionFrom.y} L${transitionTo.x} ${transitionTo.y}`} stroke="#fff" strokeWidth="1.2" strokeDasharray="3 2" opacity=".9" />
      <text x={(transitionFrom.x + transitionTo.x) / 2 + 3} y={(transitionFrom.y + transitionTo.y) / 2 - 3} fill="#fff" fontSize="5">{recoveryLabel}</text>
    </> : null}

    {rollFrom && rollTo ? <>
      <path d={`M${rollFrom.x} ${rollFrom.y} L${rollTo.x} ${rollTo.y}`} stroke="var(--accent)" strokeWidth="1.3" strokeDasharray="2 1" opacity=".95" />
      <text x={(rollFrom.x + rollTo.x) / 2 + 3} y={(rollFrom.y + rollTo.y) / 2 - 3} fill="var(--accent)" fontSize="5">Roll</text>
    </> : null}

    {(!trace || !finalMatchesContact) ? <><circle cx={finalPoint.x} cy={finalPoint.y} r="3" fill="#fff" stroke="#c99" strokeWidth=".6" />
      {trace ? <text x={finalPoint.x + 5} y={finalPoint.y + 6} fill="#fff" fontSize="5">Ball</text> : null}</> : null}
  </svg>;
}

function TargetMarker({ point, label }: { point: Point; label: string }) {
  return <g aria-label={label}><circle cx={point.x} cy={point.y} r="6" fill="none" stroke="var(--accent)" strokeWidth="1.5" />
    <path d={`M${point.x - 9} ${point.y}h18M${point.x} ${point.y - 9}v18`} stroke="var(--accent)" strokeWidth="1" /></g>;
}
