"use client";

import { useMemo, useRef, type PointerEvent } from "react";

import { BIOME_KITS, resolveBiome } from "@/lib/play/biomes";
import { canonicalRenderModel } from "@/lib/play/canonical-geometry";
import type { Hole2dProps } from "@/components/play/hole-2d";

const W = 220;
const H = 440;
const PAD = 16;

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

/** Canonical renderer: all landforms and every ball coordinate are backend supplied yard-space data. */
export function CanonicalHole2d({ hole, ball, className, aimPoint, onAimPoint }: Hole2dProps) {
  const geometry = hole.geometry!;
  const kit = BIOME_KITS[resolveBiome(hole.courseType)];
  const model = useMemo(() => canonicalRenderModel(geometry, { width: W, height: H, padding: PAD }), [geometry]);
  const tee = model.project(geometry.tee);
  const cup = model.project(geometry.cup);
  const contact = ball?.settlement?.contact ? model.project(ball.settlement.contact.position) : null;
  const recovery = ball?.settlement?.recoveryPosition ? model.project(ball.settlement.recoveryPosition) : null;
  const resting = ball?.settlement ? model.project(ball.settlement.ball.position) : model.project(hole.ball?.position ?? geometry.tee);
  const target = aimPoint ? model.project(aimPoint) : null;
  const effect = ball?.settlement?.contact.surface === "WATER" ? "#dff1ff" : ball?.settlement?.contact.surface === "BUNKER" ? kit.sandStroke : "#ffffff";
  const svgRef = useRef<SVGSVGElement>(null);

  function selectTarget(event: PointerEvent<SVGSVGElement>) {
    if (!onAimPoint || !svgRef.current) return;
    const rect = svgRef.current.getBoundingClientRect();
    onAimPoint(model.unproject({ x: (event.clientX - rect.left) * W / rect.width, y: (event.clientY - rect.top) * H / rect.height }));
  }

  return <svg ref={svgRef} viewBox={`0 0 ${W} ${H}`} className={className} role="img" aria-label={`Hole ${hole.holeNumber}, par ${hole.par}, canonical terrain`} onPointerDown={selectTarget}>
    <path d={model.playableBoundaryPath} fill={kit.out} />
    {model.terrain.map((region, index) => <path key={`${region.surface}-${index}`} d={region.path} fill={fills(region.surface, kit)} />)}
    <path d={model.playableBoundaryPath} fill="none" stroke="rgba(255,255,255,.28)" strokeWidth="1" />
    <rect x={tee.x - 7} y={tee.y - 3} width="14" height="6" rx="2" fill={kit.tee} />
    <circle cx={cup.x} cy={cup.y} r="2" fill="#111" /><path d={`M${cup.x} ${cup.y} v-13 l7 2.5 -7 2.5z`} fill="#e23b3b" />
    {contact && <><circle cx={contact.x} cy={contact.y} r="7" fill="none" stroke={effect} strokeWidth="1.5" opacity=".9"><animate attributeName="r" values="1;10" dur="500ms" fill="freeze" /></circle><circle cx={contact.x} cy={contact.y} r="2.6" fill="#fff" /></>}
    {recovery && contact && <path d={`M${contact.x} ${contact.y} L${recovery.x} ${recovery.y}`} stroke="#fff" strokeWidth="1.2" strokeDasharray="3 2" opacity=".85" />}
    <circle cx={resting.x} cy={resting.y} r="3" fill="#fff" stroke="#c99" strokeWidth=".6" />
    {target ? <><circle cx={target.x} cy={target.y} r="6" fill="none" stroke="var(--accent)" strokeWidth="1.5" /><path d={`M${target.x - 9} ${target.y}h18M${target.x} ${target.y - 9}v18`} stroke="var(--accent)" strokeWidth="1" /></> : null}
  </svg>;
}
