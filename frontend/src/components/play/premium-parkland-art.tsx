import { premiumOpenings, type PremiumTree, type PremiumTreePlan } from "../../lib/play/premium-parkland-decoration";
import type { PropertyPlan } from "../../lib/play/premium-parkland-property";

type RenderRegion = Readonly<{ surface: string; path: string }>;

/** One restrained illustration palette, separate from the application's interactive UI tokens. */
const ink = {
  woodland: "#173722", moss: "#37683a",
  turfDark: "#477f3d", turf: "#70ad55", turfLight: "#9bd174",
  greenDark: "#5da765", green: "#89ca7c", greenLight: "#b9e3a1",
  sandDark: "#aa946d", sand: "#e1cca1", sandLight: "#f5e6bf",
  waterDark: "#133b3d", water: "#2b6260", waterLight: "#699a89",
};

export function PremiumParklandDefs({ artId, coreX, coreWidth, height, fullStage = false }: {
  artId: string; coreX: number; coreWidth: number; height: number; fullStage?: boolean;
}) {
  return <defs>
    <linearGradient id={`${artId}-premium-rough`} x1="0" y1="0" x2=".8" y2="1">
      <stop stopColor="#548046" /><stop offset=".55" stopColor={ink.moss} /><stop offset="1" stopColor={ink.woodland} />
    </linearGradient>
    <linearGradient id={`${artId}-premium-wide-rough`} gradientUnits="userSpaceOnUse"
      x1={coreX} y1="0" x2={coreX + coreWidth * .8} y2={height}>
      <stop stopColor="#548046" /><stop offset=".55" stopColor={ink.moss} />
      <stop offset="1" stopColor={ink.woodland} />
    </linearGradient>
    <linearGradient id={`${artId}-premium-fairway`} x1="0" y1="0" x2=".7" y2="1">
      <stop stopColor={ink.turfLight} /><stop offset=".5" stopColor={ink.turf} /><stop offset="1" stopColor={ink.turfDark} />
    </linearGradient>
    <radialGradient id={`${artId}-premium-green`} cx="42%" cy="38%" r="68%">
      <stop stopColor={ink.greenLight} /><stop offset=".55" stopColor={ink.green} /><stop offset="1" stopColor={ink.greenDark} />
    </radialGradient>
    <linearGradient id={`${artId}-premium-fringe`} x1="0" y1="0" x2=".85" y2="1">
      <stop stopColor="#8cc46a" /><stop offset="1" stopColor="#5b9b4c" />
    </linearGradient>
    <linearGradient id={`${artId}-premium-sand`} x1=".18" y1="0" x2=".9" y2="1">
      <stop stopColor={ink.sandLight} /><stop offset=".6" stopColor={ink.sand} /><stop offset="1" stopColor={ink.sandDark} />
    </linearGradient>
    <linearGradient id={`${artId}-premium-water`} x1="0" y1="0" x2="1" y2="1">
      <stop stopColor={ink.waterLight} /><stop offset=".3" stopColor={ink.water} /><stop offset="1" stopColor={ink.waterDark} />
    </linearGradient>
    <radialGradient id={`${artId}-premium-crown-a`} cx="34%" cy="27%" r="74%">
      <stop stopColor="#8daa55" /><stop offset=".53" stopColor="#4d7a3e" /><stop offset="1" stopColor="#1b4028" />
    </radialGradient>
    <radialGradient id={`${artId}-premium-crown-b`} cx="36%" cy="24%" r="75%">
      <stop stopColor="#a6ba65" /><stop offset=".52" stopColor="#5d8644" /><stop offset="1" stopColor="#22492e" />
    </radialGradient>
    <radialGradient id={`${artId}-premium-crown-c`} cx="30%" cy="28%" r="72%">
      <stop stopColor="#789246" /><stop offset=".48" stopColor="#3c6937" /><stop offset="1" stopColor="#1b3a26" />
    </radialGradient>
    <radialGradient id={`${artId}-premium-grassland`}>
      <stop stopColor="#819d5f" stopOpacity=".38" />
      <stop offset=".55" stopColor="#76945b" stopOpacity=".21" />
      <stop offset="1" stopColor="#76945b" stopOpacity="0" />
    </radialGradient>
    <radialGradient id={`${artId}-premium-vignette`} cx="50%" cy="45%" r="70%">
      <stop offset=".3" stopColor="#06130f" stopOpacity="0" />
      <stop offset=".8" stopColor="#06130f" stopOpacity=".07" />
      <stop offset="1" stopColor="#030a08" stopOpacity=".32" />
    </radialGradient>
    <linearGradient id={`${artId}-premium-fullstage-atmosphere`} x1="0" y1="0" x2="1" y2="0">
      <stop stopColor="var(--background)" stopOpacity=".88" />
      <stop offset=".15" stopColor="var(--background)" stopOpacity=".8" />
      <stop offset=".25" stopColor="var(--background)" stopOpacity=".58" />
      <stop offset=".34" stopColor="var(--background)" stopOpacity=".16" />
      <stop offset=".42" stopColor="var(--background)" stopOpacity="0" />
      <stop offset=".58" stopColor="var(--background)" stopOpacity="0" />
      <stop offset=".66" stopColor="var(--background)" stopOpacity=".16" />
      <stop offset=".75" stopColor="var(--background)" stopOpacity=".58" />
      <stop offset=".85" stopColor="var(--background)" stopOpacity=".8" />
      <stop offset="1" stopColor="var(--background)" stopOpacity=".88" />
    </linearGradient>
    <pattern id={`${artId}-premium-ground-grain`} width="41" height="47" patternUnits="userSpaceOnUse">
      <path d="M2 19l2-3m21 10 2-4m10-8 1-2M10 41l2-3m25 8 1-3" stroke="#9ab16c" strokeWidth=".45" opacity=".24" />
      <circle cx="7" cy="6" r=".45" fill="#a5ba79" opacity=".3" /><circle cx="33" cy="35" r=".35" fill="#a5ba79" opacity=".26" />
    </pattern>
    <pattern id={`${artId}-premium-rough-grain`} width="23" height="29" patternUnits="userSpaceOnUse">
      <path d="M2 13l2-5m3 6 1-3m11 6 3-6M12 28l2-5m6 5 2-4" stroke="#a9bf78" strokeWidth=".55" opacity=".26" />
      <path d="M4 26l1-3m13-18 1-2" stroke="#122e20" strokeWidth=".8" opacity=".33" />
    </pattern>
    <pattern id={`${artId}-premium-rough-mottle`} width="67" height="83" patternUnits="userSpaceOnUse" patternTransform="rotate(8)">
      <path d="M-6 19q9-13 24-5t19 1q12-5 24 6M8 67q11-13 23-7t30-9" fill="none" stroke="#9fbd72" strokeWidth="7" opacity=".055" />
      <ellipse cx="12" cy="38" rx="8" ry="16" fill="#081e18" opacity=".07" />
      <ellipse cx="53" cy="64" rx="12" ry="9" fill="#b5cd7b" opacity=".04" />
    </pattern>
    <pattern id={`${artId}-premium-mow`} width="17" height="22" patternUnits="userSpaceOnUse" patternTransform="rotate(-4)">
      <rect width="17" height="22" fill="#b8d986" opacity=".025" />
      <rect y="11" width="17" height="11" fill="#163f28" opacity=".105" />
      <path d="M3 4l1 4m8 8 1 3M15 6l1 2" stroke="#d2e6a2" opacity=".16" strokeWidth=".5" />
    </pattern>
    <pattern id={`${artId}-premium-fairway-mottle`} width="83" height="97" patternUnits="userSpaceOnUse">
      <path d="M-9 23Q12 9 28 16T65 20T93 11M-9 74Q12 62 30 72T75 66T94 70" fill="none" stroke="#c9e69b" strokeWidth="5" opacity=".055" />
      <path d="M7 41q8-4 16 0m31 43q9-4 19 1" fill="none" stroke="#204d30" strokeWidth="3" opacity=".08" />
    </pattern>
    <pattern id={`${artId}-premium-sand-grain`} width="11" height="13" patternUnits="userSpaceOnUse">
      <path d="M-1 5 Q3 2 7 4 T13 4M-2 10 Q3 8 7 9 T13 8" fill="none" stroke="#fff2cc" strokeWidth=".5" opacity=".35" />
      <circle cx="2" cy="11" r=".4" fill="#967e59" opacity=".45" /><circle cx="9" cy="2" r=".3" fill="#967e59" opacity=".35" />
    </pattern>
    <pattern id={`${artId}-premium-water-grain`} width="17" height="13" patternUnits="userSpaceOnUse">
      <path d="M1 4q3-2 6 0t7 0M4 10q3-1 6 0" fill="none" stroke="#b0cbb1" strokeWidth=".55" opacity=".3" />
    </pattern>
    <filter id={`${artId}-premium-woodland-soften`} x="-30%" y="-30%" width="160%" height="160%">
      <feGaussianBlur stdDeviation="4" />
    </filter>
    {!fullStage ? <filter id={`${artId}-premium-corridor-soften`} filterUnits="userSpaceOnUse" x="-160" y="-100" width="540" height="640">
      <feGaussianBlur stdDeviation="13" />
    </filter> : null}
    {!fullStage ? <filter id={`${artId}-premium-distance-soften`} filterUnits="userSpaceOnUse" x="-160" y="-100" width="540" height="640">
      <feGaussianBlur stdDeviation="22" />
    </filter> : null}
    <symbol id={`${artId}-premium-tree-shadow`} viewBox="-12 -12 24 36">
      <ellipse cx="5.5" cy="8.5" rx="8" ry="12" fill="#071b13" opacity=".27" transform="rotate(-29 5.5 8.5)" />
      <ellipse cx="4.5" cy="6" rx="6.5" ry="8.5" fill="#071b13" opacity=".18" transform="rotate(-29 4.5 6)" />
    </symbol>
    <symbol id={`${artId}-premium-tree-0`} viewBox="-12 -12 24 24"><TreeCrown artId={artId} variant={0} /></symbol>
    <symbol id={`${artId}-premium-tree-1`} viewBox="-12 -12 24 24"><TreeCrown artId={artId} variant={1} /></symbol>
    <symbol id={`${artId}-premium-tree-2`} viewBox="-12 -12 24 24"><TreeCrown artId={artId} variant={2} /></symbol>
    <symbol id={`${artId}-premium-tree-3`} viewBox="-12 -12 24 24"><TreeCrown artId={artId} variant={3} /></symbol>
    <symbol id={`${artId}-premium-tree-4`} viewBox="-12 -12 24 24"><TreeCrown artId={artId} variant={4} /></symbol>
    <symbol id={`${artId}-premium-tree-5`} viewBox="-12 -12 24 24"><TreeCrown artId={artId} variant={5} /></symbol>
  </defs>;
}

function TreeCrown({ artId, variant }: { artId: string; variant: number }) {
  const d = [
    "M-9-2Q-11-8-4-9Q0-12 4-9Q10-9 9-3Q13 3 8 7Q3 11-2 9Q-10 10-10 4Q-12 1-9-2Z",
    "M-8-5Q-6-11 0-9Q5-12 8-6Q12-2 9 3Q10 9 3 9Q-2 12-6 7Q-12 5-8-5Z",
    "M-10 0Q-11-6-5-8Q-1-12 5-8Q11-7 10-1Q13 6 6 8Q1 12-4 8Q-11 8-10 0Z",
    "M-8-6Q-2-11 3-9Q9-10 10-3Q12 2 8 7Q3 11-2 9Q-9 11-10 4Q-12-2-8-6Z",
    "M-10-2Q-11-6-7-7Q-5-11 0-10Q4-12 6-8Q11-8 10-3Q13 0 9 3Q10 8 5 8Q2 11-2 8Q-8 11-10 5Q-13 1-10-2Z",
    "M-7-8Q-2-11 1-8Q7-11 9-5Q13-3 10 2Q12 7 6 8Q2 12-2 9Q-7 11-9 5Q-12 1-9-3Q-11-7-7-8Z",
  ][variant];
  const fill = `${artId}-premium-crown-${variant === 1 ? "b" : variant === 2 ? "c" : "a"}`;
  const shift = variant % 3 - 1;
  return <>
    <path d={d} fill="#0b261a" stroke="#0c291b" strokeWidth="1.2" />
    <path d={d} fill={`url(#${fill})`} transform="translate(-.65 -.8) scale(.94)" />
    <circle cx={-4 + shift} cy="-3.8" r="3.3" fill={`url(#${artId}-premium-crown-b)`} opacity=".7" />
    <circle cx={2 + shift} cy="-5" r="3.7" fill={`url(#${artId}-premium-crown-a)`} opacity=".72" />
    <circle cx="5.5" cy=".5" r="3.1" fill={`url(#${artId}-premium-crown-c)`} opacity=".65" />
    <circle cx="-3" cy="4.7" r="3.5" fill={`url(#${artId}-premium-crown-a)`} opacity=".6" />
    <path d="M-6 1q3-1 5 1m2 1q2-2 5-1" fill="none" stroke="#163a26" strokeWidth=".9" opacity=".6" />
    <path d="M-6-2q2-4 5-3m2-2q3-1 5 2M-5 3q2 1 3 3m6-4q3 0 4 2" fill="none" stroke="#a9c269" strokeWidth=".8" opacity=".31" />
    <circle cx="-3.5" cy="-4" r="1.3" fill="#bed280" opacity=".19" />
    <circle cx="4.8" cy="2.8" r="1" fill="#b6cd72" opacity=".17" />
  </>;
}

function Crown({ artId, tree }: { artId: string; tree: PremiumTree }) {
  const aspect = tree.variant % 3;
  const xScale = tree.size / 10 * (.88 + aspect * .09);
  const yScale = tree.size / 10 * (1.11 - aspect * .07);
  return <g opacity={(tree.opacity ?? 1) * (.82 + tree.shade * .18)}
    transform={`translate(${tree.x.toFixed(2)} ${tree.y.toFixed(2)}) rotate(${tree.rotation.toFixed(0)}) scale(${xScale.toFixed(3)} ${yScale.toFixed(3)})`}>
    <ellipse cx="2" cy="3" rx="9" ry="7" fill="#020e0a" opacity={.3 + tree.shade * .23} />
    <use href={`#${artId}-premium-tree-${tree.variant}`} x="-12" y="-12" width="24" height="24" />
  </g>;
}
function CrownShadow({ artId, tree }: { artId: string; tree: PremiumTree }) {
  return <use href={`#${artId}-premium-tree-shadow`}
    opacity={tree.opacity ?? 1}
    x={(tree.x - tree.size * 1.2).toFixed(2)} y={(tree.y - tree.size * 1.2).toFixed(2)}
    width={(tree.size * 2.4).toFixed(2)} height={(tree.size * 3.6).toFixed(2)} />;
}

export function PremiumParklandBackdrop({ artId, x, width, height, seed, fullStage = false, coreX = x, coreWidth = width }: {
  artId: string; x: number; width: number; height: number; seed: string; fullStage?: boolean;
  coreX?: number; coreWidth?: number;
}) {
  const openings = premiumOpenings(seed, coreX, coreWidth, height);
  return <g pointerEvents="none">
    <rect x={x} width={width} height={height} fill={`url(#${artId}-premium-${fullStage ? "wide-rough" : "rough"})`} />
    <rect x={x} width={width} height={height} fill={`url(#${artId}-premium-ground-grain)`} />
    {!fullStage ? <rect x={x} width={width} height={height} fill={`url(#${artId}-premium-rough-mottle)`} /> : null}
    {openings.map((opening, index) => <g key={index} transform={`rotate(${opening.rotation} ${opening.x} ${opening.y})`}>
      <ellipse cx={opening.x} cy={opening.y} rx={opening.rx * 1.55} ry={opening.ry * 1.45}
        fill={`url(#${artId}-premium-grassland)`} />
      <ellipse cx={opening.x - 4} cy={opening.y + 5} rx={opening.rx * .75} ry={opening.ry * .8}
        fill="#7fa15e" opacity=".055" />
    </g>)}
  </g>;
}

/** Property fragments are composed outside the playable projection and never receive pointer events. */
export function PremiumParklandProperty({ artId, plan }: { artId: string; plan: PropertyPlan }) {
  return <g pointerEvents="none" aria-hidden="true">
    {plan.paths.map((path, index) => <g key={`path-${index}`} opacity=".45">
      <path d={path.d} fill="none" stroke="#13271c" strokeWidth={path.width + 2.5} strokeLinecap="round" />
      <path d={path.d} fill="none" stroke="#9d9b75" strokeWidth={path.width} strokeLinecap="round" />
    </g>)}
    {plan.glimpses.map((glimpse, index) => <g key={`glimpse-${index}`} data-property-glimpse={index}
      data-property-orientation={glimpse.orientation} opacity=".7">
      {glimpse.regions.map((region, regionIndex) => region.surface === "FIRST_CUT"
        ? <g key={`first-cut-${regionIndex}`}>
          <path d={region.path} fill={`url(#${artId}-premium-rough)`}
            stroke={`url(#${artId}-premium-rough)`} strokeWidth="18" opacity=".72" />
          <path d={region.path} fill={`url(#${artId}-premium-fringe)`} opacity=".52" />
          <path d={region.path} fill={`url(#${artId}-premium-rough-grain)`} opacity=".34" />
          <path d={region.path} fill="none" stroke={`url(#${artId}-premium-rough)`}
            strokeWidth="6" opacity=".48" />
        </g>
        : <PremiumParklandSurface key={`${region.surface}-${regionIndex}`} artId={artId}
          region={region} fullStage />)}
    </g>)}
    {plan.building ? <g transform={`translate(${plan.building.x} ${plan.building.y}) scale(${plan.building.scale})`} opacity=".52">
      <path d="M-21 1 0-10 21 1v4h-42z" fill="#101d17" stroke="#74806a" strokeWidth="1.2" />
      <path d="M-17 5h34v13h-34z" fill="#687363" /><path d="M-18 18h36" stroke="#19281e" strokeWidth="2" />
      <path d="M-11 9v7m8-7v7m8-7v7m8-7v7" stroke="#283a2b" strokeWidth="3" />
    </g> : null}
  </g>;
}

export function PremiumParklandSurface({ artId, region, fullStage = false }: {
  artId: string; region: RenderRegion; fullStage?: boolean;
}) {
  const { surface, path } = region;
  const fill = surface === "FAIRWAY" ? "fairway"
    : surface === "FIRST_CUT" || surface === "FRINGE" ? "fringe"
      : surface === "GREEN" ? "green"
        : surface === "BUNKER" || surface === "WASTE_AREA" ? "sand"
          : surface === "WATER" ? "water"
            : surface === "TREES" ? "rough"
              : "rough";
  const grain = surface === "FAIRWAY" ? "mow"
    : surface === "BUNKER" || surface === "WASTE_AREA" ? "sand-grain"
      : surface === "WATER" ? "water-grain"
        : surface === "GREEN" ? null : "rough-grain";
  const bunker = surface === "BUNKER" || surface === "WASTE_AREA";
  const water = surface === "WATER";
  return <g pointerEvents="none">
    {bunker ? <path d={path} fill="none" stroke="#182d20" strokeWidth="5" opacity=".5" /> : null}
    <path d={path} fill={`url(#${artId}-premium-${fill})`} />
    {grain ? <path d={path} fill={`url(#${artId}-premium-${grain})`} opacity={surface === "FAIRWAY" ? ".68" : ".6"} /> : null}
    {surface === "FAIRWAY" ? <path d={path} fill={`url(#${artId}-premium-fairway-mottle)`} /> : null}
    {!fullStage && (surface === "PRIMARY_ROUGH" || surface === "DEEP_ROUGH" || surface === "RECOVERY_AREA") ?
      <path d={path} fill={`url(#${artId}-premium-rough-mottle)`} /> : null}
    {bunker ? <><path d={path} fill="none" stroke="#816c48" strokeWidth="2.3" opacity=".65" />
      <path d={path} fill="none" stroke="#f8ebc4" strokeWidth=".75" opacity=".8" /></> : null}
    {water ? <path d={path} fill="none" stroke="#7ba68b" strokeWidth="1.3" opacity=".48" /> : null}
    {surface === "GREEN" ? <path d={path} fill="none" stroke="#b9de8d" strokeWidth=".8" opacity=".48" /> : null}
  </g>;
}

export function PremiumParklandShadows({ artId, plans, regions, distantTrees }: {
  artId: string; plans: readonly PremiumTreePlan[]; regions: readonly RenderRegion[];
  distantTrees: readonly PremiumTree[];
}) {
  return <g pointerEvents="none">
    <g filter={`url(#${artId}-premium-woodland-soften)`} opacity=".36">
      {plans.map((plan) => <path key={plan.regionIndex} d={regions[plan.regionIndex].path}
        transform="translate(5 8)" fill="#071b13" />)}
    </g>
    <g>{distantTrees.map((tree, index) => <CrownShadow key={index} artId={artId} tree={tree} />)}</g>
    <g>{plans.flatMap((plan) => plan.trees.map((tree, index) =>
      <CrownShadow key={`${plan.regionIndex}-${index}`} artId={artId} tree={tree} />))}</g>
  </g>;
}

export function PremiumParklandCanopies({ artId, plans, distantTrees }: {
  artId: string; plans: readonly PremiumTreePlan[]; distantTrees: readonly PremiumTree[];
}) {
  return <g pointerEvents="none">
    <g opacity=".93">{distantTrees.map((tree, index) => <Crown key={index} artId={artId} tree={tree} />)}</g>
    {plans.map((plan) => <g key={plan.regionIndex}>
      {plan.trees.map((tree, index) => <Crown key={index} artId={artId} tree={tree} />)}
    </g>)}
  </g>;
}

export function PremiumParklandLight({ artId, x, width, height, corridorPath, fullStage = false }: {
  artId: string; x: number; width: number; height: number; corridorPath: string; fullStage?: boolean;
}) {
  return <g pointerEvents="none">
    {!fullStage ? <defs><mask id={`${artId}-premium-outside`} maskUnits="userSpaceOnUse" x={x} y="0" width={width} height={height}>
      <rect x={x} width={width} height={height} fill="white" />
      <path d={corridorPath} fill="black" stroke="black" strokeWidth="42"
        filter={`url(#${artId}-premium-corridor-soften)`} />
    </mask>
    <mask id={`${artId}-premium-far-outside`} maskUnits="userSpaceOnUse" x={x} y="0" width={width} height={height}>
      <rect x={x} width={width} height={height} fill="white" />
      <path d={corridorPath} fill="black" stroke="black" strokeWidth="126"
        filter={`url(#${artId}-premium-distance-soften)`} />
    </mask></defs> : null}
    {!fullStage ? <path d={corridorPath} fill="#d9ed9e" opacity=".055" /> : null}
    {!fullStage ? <rect x={x} width={width} height={height} fill="#06110c" opacity=".34" mask={`url(#${artId}-premium-outside)`} /> : null}
    {!fullStage ? <rect x={x} width={width} height={height} fill="#030b08" opacity=".62" mask={`url(#${artId}-premium-far-outside)`} /> : null}
    <rect x={x} width={width} height={height}
      fill={`url(#${artId}-premium-${fullStage ? "fullstage-atmosphere" : "vignette"})`} />
  </g>;
}
