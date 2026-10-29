import { memo, useMemo } from "react";

import type { HoleGeom, Point } from "@/lib/play/hole-geometry";
import { pointInPolygon, puttingDecoration, puttingPolygonPath, type PuttingFamily, type PuttingScene } from "@/lib/play/premium-putting";
import type { PhysicalPuttingProjector } from "@/lib/play/physical-putting";

/** Material stops and layered shapes transcribed from the approved Python/SVG source package. */
const PALETTES = {
  parkland: { ground: ["#162918", "#24451e", "#234523", "#10251a"], sand: ["#f5e7c7", "#dbc9aa", "#bfa989"], shade: "#06110d" },
  links: { ground: ["#717b4b", "#9da163", "#b8aa70", "#828654"], sand: ["#f4e6bd", "#dfcc9b", "#a78b6b"], shade: "#161a14" },
  heathland: { ground: ["#19281e", "#42472d", "#605c3b", "#333c28"], sand: ["#e9dbb3", "#ddc9a5", "#9d8b69"], shade: "#091711" },
  desert: { ground: ["#694d36", "#a57851", "#c39b68", "#926744"], sand: ["#f3d9a5", "#dfbd8b", "#9c7550"], shade: "#211915" },
} as const;

export const PremiumPuttingTerrain = memo(function PremiumPuttingTerrain({ hole, projector, family, id, greenPath, scene }: {
  hole: HoleGeom; projector: PhysicalPuttingProjector; family: PuttingFamily; id: string; greenPath: string; scene: PuttingScene;
}) {
  const palette = PALETTES[family];
  const fringe = hole.geometry!.regions.find((r) => r.surface === "FRINGE")!.boundary;
  const fringePath = puttingPolygonPath(fringe, projector);
  const hazards = hole.geometry!.regions.filter((r) => r.surface === "BUNKER" || r.surface === "WATER");
  const decoration = useMemo(() => puttingDecoration(hole, projector, family, scene), [hole, projector, family, scene]);
  const terrain = useMemo(() => terrainDetail(hole.layoutSeed, family, scene), [hole.layoutSeed, family, scene]);
  const turf = useMemo(() => turfMarks(hole.layoutSeed, projector, hole.geometry!.regions.find((r) => r.surface === "GREEN")!.boundary), [hole.layoutSeed, hole.geometry, projector]);
  // No exterior band when a tight short-putt view is entirely inside the green/fringe.
  const edgeWidth = Math.max(1, Math.min(4, projector.pixelsPerYard * .32));
  return <g pointerEvents="none" aria-label={`Premium ${family} putting terrain`}>
    <defs>
      <linearGradient id={`${id}-ground`} x1="0" y1="0" x2=".68" y2="1">
        {palette.ground.map((color, i) => <stop key={i} offset={["0", ".35", ".7", "1"][i]} stopColor={color} />)}
      </linearGradient>
      <radialGradient id={`${id}-light`} cx="52%" cy="42%" r="67%">
        <stop stopColor="#96c66d" stopOpacity={family === "desert" ? ".10" : ".30"}/>
        <stop offset="65%" stopColor="#517a43" stopOpacity=".10"/><stop offset="100%" stopColor="#0b1a0e" stopOpacity="0"/>
      </radialGradient>
      <linearGradient id={`${id}-sand`} x1=".1" y1="0" x2=".8" y2="1">
        {palette.sand.map((color, i) => <stop key={i} offset={["0", ".57", "1"][i]} stopColor={color} />)}
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
        {([ ["0", ".82"], [".19", ".39"], [".40", "0"], [".62", "0"], [".84", ".32"], ["1", ".86"] ] as const)
          .map(([offset, opacity]) => <stop key={offset} offset={offset} stopColor={palette.shade} stopOpacity={opacity}/>)}
      </linearGradient>
      <radialGradient id={`${id}-edge`} cx="50%" cy="50%" r="72%">
        <stop offset="55%" stopColor="#000" stopOpacity="0"/><stop offset="100%" stopColor="#06120d" stopOpacity=".86"/>
      </radialGradient>
      <clipPath id={`${id}-turf`}><path d={greenPath}/></clipPath>
      <radialGradient id={`${id}-ball`} cx="31%" cy="25%" r="78%"><stop stopColor="#fff"/><stop offset="53%" stopColor="#f7f8f0"/><stop offset="100%" stopColor="#9aa49c"/></radialGradient>
    </defs>
    <rect x={scene.x} y={scene.y} width={scene.width} height={scene.height} fill={`url(#${id}-ground)`}/>
    <TerrainDetail detail={terrain} family={family}/>
    <Scenery objects={decoration} family={family}/>
    <rect x={scene.x} y={scene.y} width={scene.width} height={scene.height} fill={`url(#${id}-light)`}/>
    {hazards.map((region, i) => region.boundary.length >= 3 ? <g key={i}>
      {region.surface === "BUNKER" ? <path d={puttingPolygonPath(region.boundary, projector)} fill="none"
        stroke={palette.sand[2]} strokeOpacity=".55" strokeWidth={edgeWidth * 3}/>: null}
      <path d={puttingPolygonPath(region.boundary, projector)} fill={region.surface === "BUNKER" ? `url(#${id}-sand)` : "#426b74"}
        stroke={region.surface === "BUNKER" ? palette.sand[2] : "#8fb6b1"} strokeWidth={edgeWidth * 1.5}/>
      {region.surface === "BUNKER" ? <>
        <clipPath id={`${id}-bunker-${i}`}><path d={puttingPolygonPath(region.boundary, projector)}/></clipPath>
        <path d={bunkerRaking(region.boundary, projector, i)} clipPath={`url(#${id}-bunker-${i})`}
          fill="none" stroke={palette.sand[2]} strokeOpacity=".15" strokeWidth=".65"/>
        <path d={puttingPolygonPath(region.boundary, projector)} fill="none" stroke={palette.sand[0]} strokeOpacity=".45" strokeWidth=".8"/>
      </> : null}
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
    <rect x={scene.x} y={scene.y} width={scene.width} height={scene.height} fill={`url(#${id}-side)`}/>
    <rect x={scene.x} y={scene.y} width={scene.width} height={scene.height} fill={`url(#${id}-edge)`} opacity=".58"/>
  </g>;
});

type Decor = ReturnType<typeof puttingDecoration>[number];
function Scenery({objects, family}: {objects: readonly Decor[]; family: PuttingFamily}) {
  const fescue = ["", "", ""];
  objects.filter((o) => o.kind === "fescue").forEach((o, i) => {
    const random = objectRandom(o);
    const strokes = Array.from({length: 6}, () => {
      const dx = (random() - .5) * o.radius * 1.4, dy = -(.5 + random()) * o.radius;
      return `M${(o.x + (random() - .5) * 3).toFixed(1)} ${(o.y + 2).toFixed(1)} q${(dx * .1).toFixed(1)} ${(dy * .4).toFixed(1)} ${dx.toFixed(1)} ${(dy * .53).toFixed(1)}`;
    }).join(" ");
    fescue[i % 4 === 0 ? 2 : i % 2] += `${strokes} `;
  });
  return <g aria-label={`${family} scenery`}>
    {fescue.map((path, i) => path ? <path key={i} d={path} fill="none"
      stroke={["#65794a", "#8e9a58", "#c8ae82"][i]} strokeOpacity=".55" strokeWidth=".85"/> : null)}
    {objects.filter((o) => o.kind !== "fescue").map((o, i) => <ScenicObject key={i} object={o}/>)}
  </g>;
}

function ScenicObject({object: o}: {object: Decor}) {
  const r = o.radius, x = o.x, y = o.y;
  const random = objectRandom(o);
  if (o.kind === "tree") return <g>
    <ellipse cx={x + r * .23} cy={y + r * .34} rx={r * 1.16} ry={r * .96} fill="#010b08" opacity=".28"/>
    <circle cx={x} cy={y} r={r} fill="#162a17"/>
    {Array.from({length: 12}, (_, i) => {const a = i * Math.PI * 2 / 12 + (random() - .5) * .32;
      const distance = r * (.31 + random() * .35), radius = r * (.37 + random() * .21);
      return <ellipse key={i} cx={x + Math.cos(a) * distance} cy={y + Math.sin(a) * distance * .78}
        rx={radius} ry={radius * (.70 + random() * .33)}
        fill={["#38572a", "#466b32", "#507638", "#638345", "#73934b", "#294921"][i % 6]}/>;})}
    {Array.from({length: 4}, (_, i) => <ellipse key={i} cx={x + (random() * .74 - .52) * r} cy={y + (random() * .58 - .55) * r}
      rx={r * (.18 + random() * .13)} ry={r * .16} fill={["#7da458", "#6f934c", "#587a41"][i % 3]} opacity=".48"/>)}
  </g>;
  if (o.kind === "rock") {
    const points = Array.from({length: 8}, (_, i) => { const a = i * Math.PI / 4, wobble = .78 + random() * .38;
      return {x: x + Math.cos(a) * r * wobble, y: y + Math.sin(a) * r * .6 * wobble}; });
    const shape = (dx: number, dy: number, scale: number) => `M${points.map((p) => `${(x + (p.x - x) * scale + dx).toFixed(1)} ${(y + (p.y - y) * scale + dy).toFixed(1)}`).join(" L")} Z`;
    return <g><path d={shape(2, 2, 1)} fill="#172217" opacity=".24"/><path d={shape(0, 0, 1)} fill="#835c43"/>
      <path d={shape(-1, -1, .79)} fill="#d7aa76" opacity=".83"/>
      <path d={`M${x-r*.52} ${y-r*.1} q${r*.62} ${-r*.28} ${r*1.08} ${-r*.02}`} stroke="#f5d1a2" strokeOpacity=".22" fill="none"/></g>;
  }
  if (o.kind === "cactus") return <g><ellipse cx={x + 2} cy={y + 2} rx={r * .7} ry={r * .45} fill="#291e16" opacity=".22"/>
    <ellipse cx={x} cy={y} rx={r * .55} ry={r * .8} fill="#3e623d"/><ellipse cx={x - r * .15} cy={y - r * .18} rx={r * .23} ry={r * .5} fill="#6f9556" opacity=".6"/>
    <path d={`M${x} ${y-r*.7} v${r*1.32}`} stroke="#a0ad77" strokeWidth=".65" strokeOpacity=".4"/></g>;
  const heather = o.kind === "heather", gorse = o.kind === "gorse";
  const greens = heather ? ["#4b5035", "#5b5c3e", "#373e31", "#4d6140", "#746a51"]
    : gorse ? ["#36482b", "#47603a", "#5a6a3c", "#6c7440"] : ["#394a36", "#657548", "#756d48", "#8d8855"];
  const flowers = heather ? ["#aa88b5", "#b594c4", "#92709c", "#d1afd0", "#805c99"] : ["#dac261", "#e5d77a", "#bb9f46"];
  return <g><ellipse cx={x + 3} cy={y + 5} rx={r * (heather ? 1.4 : 1.2)} ry={r * .75} fill="#101912" opacity=".2"/>
    {Array.from({length: heather ? 10 : gorse ? 9 : 6}, (_, i) => {
      const a = random() * Math.PI * 2, distance = random() * r * .83;
      const px = x + Math.cos(a) * distance, py = y + Math.sin(a) * distance * .52;
      const radius = r * (.19 + random() * .23);
      return <g key={i}><ellipse cx={px} cy={py} rx={radius} ry={radius * (heather ? .57 : .71)} fill={greens[i % greens.length]} opacity=".9"/>
        {(heather && random() < .83 || gorse && random() < .62) ? <ellipse cx={px - radius * .15} cy={py - radius * .28}
          rx={radius * .65} ry={radius * .29} fill={flowers[i % flowers.length]} opacity=".8"/> : null}</g>;})}
    {!heather && !gorse ? <ellipse cx={x-r*.22} cy={y-r*.2} rx={r*.38} ry={r*.15} fill="#839568" opacity=".4"/> : null}
  </g>;
}

function objectRandom(o: Decor) {
  let seed = (Math.imul(Math.round(o.x * 100), 73856093) ^ Math.imul(Math.round(o.y * 100), 19349663)) >>> 0;
  return () => ((seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0) / 4294967296);
}

/** The approved generators layer broad translucent land washes and fine, sparse surface marks. */
function terrainDetail(seedText: string, family: PuttingFamily, scene: PuttingScene) {
  let seed = [...`${seedText}:${family}:terrain`].reduce((value, char) => Math.imul(value ^ char.charCodeAt(0), 16777619), 2166136261) >>> 0;
  const random = () => ((seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0) / 4294967296);
  const colors = family === "parkland" ? ["#36582c", "#2e502a", "#55713a", "#456638", "#1b3926", "#658545", "#1c3b2b"]
    : family === "links" ? ["#a7a35b", "#868b4e", "#7d8749", "#ccb47e", "#f0db9a", "#8d875f"]
    : family === "heathland" ? ["#827d50", "#91845a", "#676f43", "#c1aa75", "#515337"]
    : ["#d6b580", "#d7ad73", "#e7c995", "#895a42", "#a67b58"];
  const scale = scene.height / 300;
  const area = scene.width * scene.height / (430 * 300);
  const washes = Array.from({length: Math.round(105 * area)}, () => ({
    x: scene.x + random() * scene.width, y: scene.y + random() * scene.height,
    rx: (7 + random() * 36) * scale, ry: (3 + random() * 22) * scale,
    angle: (random() - .5) * (family === "heathland" ? 110 : 65),
    color: colors[Math.floor(random() * colors.length)], opacity: .02 + random() * .09,
  }));
  const marks = ["", "", ""];
  for (let i = 0; i < Math.round((family === "parkland" ? 260 : 410) * area); i++) {
    const x = scene.x + random() * scene.width, y = scene.y + random() * scene.height;
    const index = i % 3;
    marks[index] += family === "desert" ? `M${x.toFixed(1)} ${y.toFixed(1)} l${((random() - .5) * 2).toFixed(1)} ${(random() * 1.4).toFixed(1)} `
      : `M${x.toFixed(1)} ${y.toFixed(1)} q${((random() - .65) * 2).toFixed(1)} ${(-random() * 2).toFixed(1)} ${((random() - .5) * 3).toFixed(1)} ${(-1 - random() * 3).toFixed(1)} `;
  }
  const contours = family === "links" || family === "heathland" || family === "desert"
    ? Array.from({length: family === "links" ? 12 : family === "heathland" ? 8 : 4}, (_, i) => {
      const y = scene.y + (i + .5) / (family === "links" ? 12 : family === "heathland" ? 8 : 4) * scene.height;
      const shift = (random() - .5) * 30;
      return `M${scene.x - 20} ${y.toFixed(1)} C${(scene.x + scene.width * .25).toFixed(1)} ${(y + 15).toFixed(1)} ${(scene.x + scene.width * .45).toFixed(1)} ${(y - 18 + shift).toFixed(1)} ${(scene.x + scene.width * .65).toFixed(1)} ${(y + 3).toFixed(1)} S${(scene.x + scene.width * .9).toFixed(1)} ${(y + 12).toFixed(1)} ${(scene.x + scene.width + 20).toFixed(1)} ${(y - 8).toFixed(1)}`;
    }).join(" ") : "";
  return {washes, marks, contours};
}

function TerrainDetail({detail, family}: {detail: ReturnType<typeof terrainDetail>; family: PuttingFamily}) {
  const markColors = family === "desert" ? ["#f1c997", "#704b35", "#e4bf8a"]
    : family === "links" ? ["#b4b189", "#c8ae82", "#536441"]
    : family === "heathland" ? ["#93936e", "#8b7656", "#4c6844"] : ["#668b4a", "#365a37", "#7b8d49"];
  return <g aria-label={`${family} ground material`}>
    {detail.washes.map((wash, i) => <ellipse key={i} cx={wash.x} cy={wash.y} rx={wash.rx} ry={wash.ry}
      transform={`rotate(${wash.angle} ${wash.x} ${wash.y})`} fill={wash.color} opacity={wash.opacity}/>)}
    {detail.contours ? <path d={detail.contours} fill="none" stroke={family === "desert" ? "#e0ba85" : "#e4d2a7"}
      strokeWidth={family === "desert" ? 2 : 1.1} strokeOpacity={family === "desert" ? ".14" : ".11"}/> : null}
    {detail.marks.map((path, i) => <path key={i} d={path} fill="none" stroke={markColors[i]} strokeOpacity={family === "parkland" ? ".2" : ".3"}
      strokeWidth={family === "desert" ? ".8" : ".65"}/>)}
  </g>;
}

function bunkerRaking(boundary: readonly Point[], projector: PhysicalPuttingProjector, index: number) {
  const points = boundary.map(projector.project);
  const left = Math.min(...points.map((point) => point.x)), right = Math.max(...points.map((point) => point.x));
  const top = Math.min(...points.map((point) => point.y)), bottom = Math.max(...points.map((point) => point.y));
  let seed = Math.imul(index + 1, 2654435761) >>> 0;
  const random = () => ((seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0) / 4294967296);
  return Array.from({length: 35}, () => {
    const x = left + random() * (right - left), y = top + random() * (bottom - top);
    return `M${(x - 9).toFixed(1)} ${(y - 1).toFixed(1)} q7 -3 15 -.7`;
  }).join(" ");
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
