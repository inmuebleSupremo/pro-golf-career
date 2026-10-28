import { memo, useMemo } from "react";

import type { HoleGeom, Point } from "@/lib/play/hole-geometry";
import { pointInPolygon, puttingDecoration, puttingPolygonPath, type PuttingFamily } from "@/lib/play/premium-putting";
import type { PhysicalPuttingProjector } from "@/lib/play/physical-putting";

/** Material stops and layered shapes transcribed from the approved Python/SVG source package. */
const PALETTES = {
  parkland: { ground: ["#162918", "#24451e", "#234523", "#10251a"], sand: ["#f5e7c7", "#dbc9aa", "#bfa989"], shade: "#06110d" },
  links: { ground: ["#695f40", "#97835b", "#b29a6c", "#846e4b"], sand: ["#f4e6bd", "#dfcc9b", "#a78b6b"], shade: "#161a14" },
  heathland: { ground: ["#19281e", "#42472d", "#605c3b", "#333c28"], sand: ["#e9dbb3", "#ddc9a5", "#9d8b69"], shade: "#091711" },
  desert: { ground: ["#694d36", "#a57851", "#c39b68", "#926744"], sand: ["#f3d9a5", "#dfbd8b", "#9c7550"], shade: "#211915" },
} as const;

export const PremiumPuttingTerrain = memo(function PremiumPuttingTerrain({ hole, projector, family, id, greenPath }: {
  hole: HoleGeom; projector: PhysicalPuttingProjector; family: PuttingFamily; id: string; greenPath: string;
}) {
  const palette = PALETTES[family];
  const fringe = hole.geometry!.regions.find((r) => r.surface === "FRINGE")!.boundary;
  const fringePath = puttingPolygonPath(fringe, projector);
  const hazards = hole.geometry!.regions.filter((r) => r.surface === "BUNKER" || r.surface === "WATER");
  const decoration = useMemo(() => puttingDecoration(hole, projector, family), [hole, projector, family]);
  const turf = useMemo(() => turfMarks(hole.layoutSeed, projector, hole.geometry!.regions.find((r) => r.surface === "GREEN")!.boundary), [hole.layoutSeed, hole.geometry, projector]);
  // No exterior band when a tight short-putt view is entirely inside the green/fringe.
  const edgeWidth = Math.max(1, Math.min(4, projector.pixelsPerYard * .32));
  return <g pointerEvents="none" aria-label={`Premium ${family} putting terrain`}>
    <defs>
      <linearGradient id={`${id}-ground`} x1="0" y1="0" x2=".68" y2="1">
        {palette.ground.map((color, i) => <stop key={i} offset={`${i * 100 / 3}%`} stopColor={color} />)}
      </linearGradient>
      <linearGradient id={`${id}-sand`} x1=".1" y1="0" x2=".8" y2="1">
        {palette.sand.map((color, i) => <stop key={i} offset={`${i * 50}%`} stopColor={color} />)}
      </linearGradient>
      <linearGradient id={`${id}-green`} x1=".22" y1="0" x2=".85" y2="1">
        <stop stopColor="#88bf62"/><stop offset="42%" stopColor="#75ae55"/><stop offset="79%" stopColor="#6aa34d"/><stop offset="100%" stopColor="#679c49"/>
      </linearGradient>
      <linearGradient id={`${id}-fringe`} x1="0" y1=".1" x2="1" y2="1">
        <stop stopColor="#5c8e46"/><stop offset="60%" stopColor="#568540"/><stop offset="100%" stopColor="#3c7235"/>
      </linearGradient>
      <radialGradient id={`${id}-shade`} cx="38%" cy="31%" r="86%">
        <stop stopColor="#d8ee9a" stopOpacity=".17"/><stop offset="60%" stopColor="#8cbd68" stopOpacity=".04"/><stop offset="100%" stopColor="#112e22" stopOpacity=".23"/>
      </radialGradient>
      <linearGradient id={`${id}-side`} x1="0" y1="0" x2="1" y2="0">
        <stop stopColor={palette.shade} stopOpacity=".42"/><stop offset="24%" stopColor={palette.shade} stopOpacity="0"/>
        <stop offset="75%" stopColor={palette.shade} stopOpacity="0"/><stop offset="100%" stopColor={palette.shade} stopOpacity=".42"/>
      </linearGradient>
      <clipPath id={`${id}-turf`}><path d={greenPath}/></clipPath>
      <radialGradient id={`${id}-ball`} cx="31%" cy="25%" r="78%"><stop stopColor="#fff"/><stop offset="53%" stopColor="#f7f8f0"/><stop offset="100%" stopColor="#9aa49c"/></radialGradient>
    </defs>
    <rect width={projector.width} height={projector.height} fill={`url(#${id}-ground)`}/>
    <Scenery objects={decoration} family={family}/>
    {hazards.map((region, i) => region.boundary.length >= 3 ? <g key={i}>
      <path d={puttingPolygonPath(region.boundary, projector)} fill={region.surface === "BUNKER" ? `url(#${id}-sand)` : "#426b74"}
        stroke={region.surface === "BUNKER" ? palette.sand[2] : "#8fb6b1"} strokeWidth={edgeWidth * 1.5}/>
      {region.surface === "BUNKER" ? <path d={puttingPolygonPath(region.boundary, projector)} fill="none" stroke={palette.sand[0]} strokeOpacity=".45" strokeWidth=".8"/> : null}
    </g> : null)}
    <path d={fringePath} fill="none" stroke="#183d23" strokeOpacity=".66" strokeWidth={edgeWidth * 3} strokeLinejoin="round"/>
    <path d={fringePath} fill="none" stroke="#436d35" strokeWidth={edgeWidth * 1.8} strokeLinejoin="round"/>
    <path d={fringePath} fill={`url(#${id}-fringe)`}/>
    <path d={greenPath} fill="none" stroke="#55873f" strokeWidth={edgeWidth * 1.2} strokeLinejoin="round"/>
    <path d={greenPath} fill={`url(#${id}-green)`}/>
    <path d={greenPath} fill={`url(#${id}-shade)`}/>
    <g clipPath={`url(#${id}-turf)`}>
      {Array.from({length: 8}, (_, i) => <path key={i} d={`M${i * 72 - 120} -30 C${i * 72 - 155} 90 ${i * 72 - 80} 195 ${i * 72 - 120} 340`}
        fill="none" stroke={i % 2 ? "#d1e8a0" : "#2e6534"} strokeWidth="35" strokeOpacity={i % 2 ? ".047" : ".043"}/>)}
      <path d={turf.light} fill="none" stroke="#cde2a2" strokeWidth="3" strokeOpacity=".055" strokeLinecap="round"/>
      <path d={turf.dark} fill="none" stroke="#2a6735" strokeWidth="1" strokeOpacity=".08" strokeLinecap="round"/>
    </g>
    <rect width={projector.width} height={projector.height} fill={`url(#${id}-side)`}/>
  </g>;
});

type Decor = ReturnType<typeof puttingDecoration>[number];
function Scenery({objects, family}: {objects: readonly Decor[]; family: PuttingFamily}) {
  const fescue = objects.filter((o) => o.kind === "fescue").map((o) => `M${o.x.toFixed(1)} ${o.y.toFixed(1)} q-2 -${o.radius.toFixed(1)} 1 -${(o.radius * 1.4).toFixed(1)}`).join(" ");
  return <g aria-label={`${family} scenery`}>
    {fescue ? <path d={fescue} fill="none" stroke="#c8ae82" strokeOpacity=".62" strokeWidth="1.1"/> : null}
    {objects.filter((o) => o.kind !== "fescue").map((o, i) => <ScenicObject key={i} object={o}/>)}
  </g>;
}

function ScenicObject({object: o}: {object: Decor}) {
  const r = o.radius, x = o.x, y = o.y;
  if (o.kind === "tree") return <g>
    <ellipse cx={x + 2} cy={y + 3} rx={r * 1.15} ry={r * .9} fill="#010b08" opacity=".28"/>
    <circle cx={x} cy={y} r={r} fill="#162a17"/>
    {Array.from({length: 11}, (_, i) => {const a = i * Math.PI * 2 / 11; return <ellipse key={i} cx={x + Math.cos(a) * r * .48} cy={y + Math.sin(a) * r * .39}
      rx={r * .46} ry={r * .39} fill={["#38572a", "#466b32", "#507638", "#638345", "#73934b", "#294921"][i % 6]}/>;})}
    {Array.from({length: 4}, (_, i) => <ellipse key={i} cx={x - r * (.36 - i * .13)} cy={y - r * (.38 - i * .09)}
      rx={r * .23} ry={r * .16} fill="#7da458" opacity=".42"/>)}
  </g>;
  if (o.kind === "rock") return <g><ellipse cx={x + 2} cy={y + 2} rx={r} ry={r * .58} fill="#172217" opacity=".24"/>
    <ellipse cx={x} cy={y} rx={r} ry={r * .6} fill="#835c43"/><ellipse cx={x - 1} cy={y - 1} rx={r * .7} ry={r * .38} fill="#d7aa76" opacity=".8"/></g>;
  if (o.kind === "cactus") return <g><ellipse cx={x + 2} cy={y + 2} rx={r * .7} ry={r * .45} fill="#291e16" opacity=".22"/>
    <ellipse cx={x} cy={y} rx={r * .55} ry={r * .8} fill="#3e623d"/><ellipse cx={x - r * .15} cy={y - r * .18} rx={r * .23} ry={r * .5} fill="#6f9556" opacity=".6"/></g>;
  const flower = o.kind === "heather" ? "#b594c4" : o.kind === "gorse" ? "#dac261" : null;
  const base = o.kind === "heather" ? "#4b5035" : o.kind === "gorse" ? "#47603a" : "#60794c";
  return <g><ellipse cx={x + 2} cy={y + 3} rx={r * 1.15} ry={r * .65} fill="#101912" opacity=".2"/>
    {Array.from({length: 5}, (_, i) => {const a = i * Math.PI * 2 / 5; const px = x + Math.cos(a) * r * .42, py = y + Math.sin(a) * r * .28;
      return <g key={i}><ellipse cx={px} cy={py} rx={r * .47} ry={r * .28} fill={base}/>
        {flower ? <ellipse cx={px - r * .1} cy={py - r * .15} rx={r * .32} ry={r * .12} fill={flower} opacity=".8"/> : null}</g>;})}
  </g>;
}

function turfMarks(seed: string, projector: PhysicalPuttingProjector, green: readonly Point[]) {
  let state = [...seed].reduce((n, c) => Math.imul(n ^ c.charCodeAt(0), 16777619), 2166136261) >>> 0;
  const random = () => ((state = (Math.imul(state, 1664525) + 1013904223) >>> 0) / 4294967296);
  const light: string[] = [], dark: string[] = [];
  for (let i = 0; i < 180; i++) {
    const x = random() * projector.width, y = random() * projector.height;
    if (!pointInPolygon(projector.unproject({ x, y }), green)) continue;
    (i % 3 ? light : dark).push(`M${x.toFixed(1)} ${y.toFixed(1)} l${(2 + random() * 4).toFixed(1)} 1`);
  }
  return { light: light.join(" "), dark: dark.join(" ") };
}
