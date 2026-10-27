import type { PropertyPlan } from "@/lib/play/premium-parkland-property";
import { seededRandom } from "@/lib/play/premium-parkland-decoration";
import { premiumLinksNeighborCorridor } from "@/lib/play/premium-links-neighbor-corridor";

type Region = Readonly<{ surface: string; path: string }>;
const f = (v: number) => v.toFixed(1);

/** The approved source's sinusoidal polar dune outline. */
function organic(cx: number, cy: number, rx: number, ry: number, seed: number, wiggle = .1): string {
  return `M ${Array.from({ length: 50 }, (_, i) => {
    const t = 2 * Math.PI * i / 50;
    const k = 1 + wiggle * Math.sin(t * 3 + seed * .9)
      + wiggle * .55 * Math.cos(t * 5 + seed * .67) + wiggle * .23 * Math.sin(t * 8 + seed);
    return `${f(cx + rx * k * Math.cos(t))} ${f(cy + ry * k * Math.sin(t))}`;
  }).join(" L ")} Z`;
}

/** A top-down coastal palette. All landforms are cosmetic; only supplied regions depict hazards. */
export function PremiumLinksDefs({ id }: { id: string; left: number; width: number; height: number }) {
  return <defs>
    <linearGradient id={`${id}-links-ground`} x1="0" y1="0" x2="1" y2=".24">
      <stop stopColor="#9c9659" /><stop offset=".30" stopColor="#baa860" /><stop offset=".53" stopColor="#d5bd70" /><stop offset=".73" stopColor="#c4aa61" /><stop offset="1" stopColor="#959354" />
    </linearGradient>
    <linearGradient id={`${id}-links-dune`} x1="0" y1="0" x2=".85" y2="1">
      <stop stopColor="#e1cc85" /><stop offset=".48" stopColor="#d4bd71" /><stop offset="1" stopColor="#b4a05f" />
    </linearGradient>
    <linearGradient id={`${id}-links-fairway`} x1="0" y1="0" x2=".8" y2="1">
      <stop stopColor="#86a956" /><stop offset=".32" stopColor="#a5c96a" /><stop offset=".67" stopColor="#a3c967" /><stop offset="1" stopColor="#7c9c4d" />
    </linearGradient>
    <radialGradient id={`${id}-links-green`} cx="42%" cy="38%" r="70%">
      <stop stopColor="#84bc64" /><stop offset=".52" stopColor="#a9db80" /><stop offset="1" stopColor="#78ac56" />
    </radialGradient>
    <linearGradient id={`${id}-links-sand`} x1=".15" y1="0" x2=".8" y2="1">
      <stop stopColor="#fff0bc" /><stop offset=".6" stopColor="#f8dba2" /><stop offset="1" stopColor="#cfae79" />
    </linearGradient>
    <linearGradient id={`${id}-links-water`} x1="0" y1="0" x2="1" y2="1">
      <stop stopColor="#8eaaa1" /><stop offset="1" stopColor="#587b78" />
    </linearGradient>
    <linearGradient id={`${id}-links-light-band`} x1="0" y1="0" x2="1" y2="0"><stop stopColor="#050e0b" stopOpacity=".90" /><stop offset=".10" stopColor="#07130e" stopOpacity=".72" /><stop offset=".23" stopColor="#0a170c" stopOpacity=".42" /><stop offset=".37" stopColor="#0c180c" stopOpacity=".10" /><stop offset=".47" stopColor="#000" stopOpacity="0" /><stop offset=".60" stopColor="#070d07" stopOpacity=".05" /><stop offset=".75" stopColor="#050f0c" stopOpacity=".35" /><stop offset=".91" stopColor="#05100d" stopOpacity=".77" /><stop offset="1" stopColor="#020a09" stopOpacity=".94" /></linearGradient>
    <linearGradient id={`${id}-links-vertical-tone`} x1="0" y1="0" x2="0" y2="1"><stop stopColor="#06120d" stopOpacity=".24" /><stop offset=".19" stopColor="#000" stopOpacity="0" /><stop offset=".77" stopColor="#000" stopOpacity="0" /><stop offset="1" stopColor="#0b150f" stopOpacity=".13" /></linearGradient>
    <pattern id={`${id}-links-grass`} width="29" height="31" patternUnits="userSpaceOnUse">
      <path d="M4 9l1-3m1 3 2-2m11 16 1-4m1 4 2-2M25 5l1-3" fill="none" stroke="#6f7952" strokeWidth=".6" opacity=".36" />
      <path d="M10 28l1-3m5-12 1-3" fill="none" stroke="#e7d6a6" strokeWidth=".7" opacity=".55" />
    </pattern>
    <pattern id={`${id}-links-turf`} width="38" height="43" patternUnits="userSpaceOnUse">
      <path d="M1 16q10-4 18 0t20-1M-2 35q12-3 21 0t22-2" fill="none" stroke="#dce1ab" strokeWidth="2" opacity=".09" />
    </pattern>
    <pattern id={`${id}-links-sand-grain`} width="13" height="11" patternUnits="userSpaceOnUse">
      <path d="M0 4q4-2 8 0m-3 5q4-2 8 0" fill="none" stroke="#fff4d4" strokeWidth=".55" opacity=".5" />
    </pattern>
  </defs>;
}

export function PremiumLinksLandscape({ id, left, width, height, seed }: {
  id: string; left: number; width: number; height: number; seed: string;
}) {
  const random = seededRandom(`${seed}:links-dunes-v1`);
  const dunes = [[175,145,225,155,5],[445,75,260,165,3],[595,340,250,185,4],
    [345,525,235,230,2],[550,778,260,210,6],[1040,90,238,170,9],
    [1220,312,325,190,7],[1275,615,260,260,5],[1470,844,230,210,11]]
    .map(([x,y,rx,ry,s]) => ({ x: left + x * width / 1600, y: y * height / 900,
      rx: rx * width / 1600, ry: ry * height / 900, seed: s }));
  const scrub = Array.from({ length: Math.min(54, Math.ceil(width / 11)) }, (_, index) => {
    const side = index % 2 === 0 ? -1 : 1;
    const x = side < 0 ? left + 12 + random() * Math.max(12, -left - 26)
      : 246 + random() * Math.max(12, left + width - 260);
    return { x, y: random() * height, size: 1.6 + random() * 3.7,
      shade: random(), rotation: random() * 360 };
  });
  const contours = Array.from({ length: 68 }, () => {
    const x = left + random() * width, y = random() * height;
    const length = (50 + random() * 150) * width / 1600;
    return { d: `M${f(x)} ${f(y)} C${f(x + length * .24)} ${f(y - 23 * height / 900)} ${f(x + length * .73)} ${f(y + 39 * height / 900)} ${f(x + length)} ${f(y + (-8 + random() * 26) * height / 900)}`,
      color: ["#f1d994", "#876f45", "#687542", "#fce9a6"][Math.floor(random() * 4)] };
  });
  const grass = Array.from({ length: Math.min(910, Math.ceil(width * height / 100)) }, () => {
    const x = left + random() * width, y = random() * height;
    const size = (2.5 + random() * 8.5) * height / 900, drift = (-5 + random() * 9) * width / 1600;
    return { d: `M${f(x)} ${f(y)} q${f(drift * .3)} ${f(-size * .45)} ${f(drift)} ${f(-size)}`,
      color: ["#536742", "#6b7542", "#7d844b", "#f1d697", "#e5c878", "#847b48", "#59653b"][Math.floor(random() * 7)] };
  });
  return <g pointerEvents="none" aria-hidden="true">
    <rect x={left} width={width} height={height} fill={`url(#${id}-links-ground)`} />
    {dunes.map((dune, index) => <g key={index}>
      <path d={organic(dune.x, dune.y, dune.rx * 1.08, dune.ry * 1.07, dune.seed, .115)}
        fill={["#868b4e", "#a7a35b", "#7d8749"][index % 3]} opacity=".28" />
      <path d={organic(dune.x - 6, dune.y - 7, dune.rx * .96, dune.ry * .95, dune.seed + 2)}
        fill={`url(#${id}-links-dune)`} opacity=".87" />
      <path d={organic(dune.x - 14, dune.y - 16, dune.rx * .75, dune.ry * .73, dune.seed + 4, .085)}
        fill="#e6ca80" opacity=".55" />
      <path d={organic(dune.x - 9, dune.y - 8, dune.rx * .51, dune.ry * .5, dune.seed + 8, .09)}
        fill="#dbbf73" opacity=".34" />
    </g>)}
    {[
      "M 365 -40 C 260 102 345 191 265 309 S 316 506 160 652",
      "M 560 -60 C 430 88 603 208 520 327 S 487 540 590 656",
      "M 585 930 C 530 794 654 671 583 541 S 590 297 660 158",
      "M 1090 -85 C 1220 64 1088 217 1160 360 S 1143 559 1030 702",
      "M 1325 945 C 1305 767 1150 691 1200 535 S 1430 300 1275 99",
      "M 1410 72 C 1480 201 1388 347 1515 510 S 1450 700 1550 862",
      "M 80 930 C 190 810 285 818 295 692 S 185 511 338 408",
    ].map((ridge, index) => <g key={index} transform={`translate(${left} 0) scale(${width / 1600} ${height / 900})`}
      fill="none" strokeLinecap="round" strokeLinejoin="round">
      <path d={ridge} stroke="#827b45" strokeWidth="84" opacity=".14" />
      <path d={ridge} stroke="#e7d083" strokeWidth="63" opacity=".20" />
      <path d={ridge} stroke="#ffdf8c" strokeWidth="13" opacity=".25" />
    </g>)}
    {contours.map((contour, index) => <path key={index} d={contour.d} fill="none" stroke={contour.color}
      strokeWidth=".7" opacity=".2" />)}
    {grass.map((blade, index) => <path key={index} d={blade.d} fill="none" stroke={blade.color}
      strokeWidth=".6" opacity=".32" strokeLinecap="round" />)}
    <rect x={left} width={width} height={height} fill={`url(#${id}-links-grass)`} opacity=".74" />
    {scrub.map((plant, index) => <g key={index} transform={`translate(${plant.x.toFixed(1)} ${plant.y.toFixed(1)}) rotate(${plant.rotation.toFixed(0)})`}
      opacity={plant.shade > .72 ? ".56" : ".38"}>
      <ellipse cx="1.5" cy="2" rx={plant.size * 1.5} ry={plant.size * .8} fill="#8b8665" opacity=".44" />
      <ellipse cx={-plant.size * .5} cy="-.6" rx={plant.size} ry={plant.size * .68} fill="#687552" />
      <ellipse cx={plant.size * .62} cy=".3" rx={plant.size * .84} ry={plant.size * .6} fill="#78835b" />
      <ellipse cy={-plant.size * .44} rx={plant.size * .75} ry={plant.size * .63} fill="#92966c" />
    </g>)}
  </g>;
}

export function PremiumLinksSurface({ id, region, neighbor = false }: { id: string; region: Region; neighbor?: boolean }) {
  const { surface, path } = region;
  const bunker = surface === "BUNKER" || surface === "WASTE_AREA";
  const fill = surface === "FAIRWAY" ? neighbor ? "#92aa62" : `url(#${id}-links-fairway)`
    : surface === "GREEN" ? neighbor ? "#8fb76d" : `url(#${id}-links-green)`
      : bunker ? `url(#${id}-links-sand)`
        : surface === "WATER" ? `url(#${id}-links-water)`
          : surface === "FIRST_CUT" || surface === "FRINGE" ? "#78944b"
            : surface === "PRIMARY_ROUGH" ? "#697743"
              : surface === "DEEP_ROUGH" ? "#827f45"
                : surface === "TEE_BOX" ? "#dce8b6" : "#748249";
  return <g pointerEvents="none" opacity={neighbor ? ".68" : undefined}>
    {bunker ? <path d={path} fill="none" stroke="#3e4c30" strokeWidth="5" opacity=".36" /> : null}
    {bunker ? <path d={path} fill="none" stroke="#777549" strokeWidth="3.5" opacity=".85" /> : null}
    <path d={path} fill={fill} />
    {surface === "FAIRWAY" || surface === "FIRST_CUT" ? <path d={path} fill={`url(#${id}-links-turf)`} /> : null}
    {["PRIMARY_ROUGH", "DEEP_ROUGH", "RECOVERY_AREA", "TREES"].includes(surface)
      ? <path d={path} fill={`url(#${id}-links-grass)`} opacity=".72" /> : null}
    {bunker ? <><path d={path} fill={`url(#${id}-links-sand-grain)`} />
      <path d={path} fill="none" stroke="#ead09d" strokeWidth="1.6" opacity=".84" />
      <path d={path} fill="none" stroke="#b38d66" strokeWidth=".55" opacity=".48" /></> : null}
    {surface === "GREEN" ? <path d={path} fill="none" stroke="#b2dc87" strokeWidth=".8" opacity=".34" /> : null}
  </g>;
}

export function PremiumLinksProperty({ id, plan }: { id: string; plan: PropertyPlan }) {
  return <g pointerEvents="none" aria-hidden="true">
    {plan.glimpses.map((glimpse, index) => {
      const corridor = premiumLinksNeighborCorridor(glimpse);
      const fadeId = `${id}-links-neighbor-fade-${index}`;
      const maskId = `${id}-links-neighbor-mask-${index}`;
      return <g key={index} data-links-neighbor={glimpse.source}
        data-property-orientation={glimpse.orientation} data-links-connected={Boolean(corridor)}>
        {corridor ? <defs>
          <linearGradient id={fadeId} gradientUnits="userSpaceOnUse"
            x1={corridor.fadeFrom.x} y1={corridor.fadeFrom.y}
            x2={corridor.fadeTo.x} y2={corridor.fadeTo.y}>
            <stop stopColor="black" /><stop offset="1" stopColor="white" />
          </linearGradient>
          <mask id={maskId} maskUnits="userSpaceOnUse" x="-2000" y="-2000" width="4000" height="4000">
            <rect x="-2000" y="-2000" width="4000" height="4000" fill={`url(#${fadeId})`} />
          </mask>
        </defs> : null}
        {corridor ? <g transform={corridor.transform} opacity=".68" data-links-neighbor-bands={index}>
          {corridor.bands.map((band, bandIndex) => <path key={bandIndex} d={band.path}
            fill={bandIndex === 4 ? `url(#${id}-links-fairway)`
              : ["#827f45", "#bfa366", "#697743", "#78944b"][bandIndex]}
            opacity={[.63, .94, .95, .99, 1][bandIndex]} data-links-neighbor-band={bandIndex} />)}
        </g> : null}
        <g mask={corridor ? `url(#${maskId})` : undefined}>
          {glimpse.regions.filter((region) => ["FIRST_CUT", "FAIRWAY"].includes(region.surface))
            .map((region, regionIndex) => <PremiumLinksSurface key={regionIndex} id={id}
              region={region} neighbor />)}
        </g>
        {glimpse.regions.filter((region) => !["FIRST_CUT", "FAIRWAY"].includes(region.surface))
          .map((region, regionIndex) => <PremiumLinksSurface key={regionIndex} id={id}
            region={region} neighbor />)}
      </g>;
    })}
    {plan.paths.map((path, index) => <g key={index} data-links-walking-track={path.kind ?? "main"}>
      <path d={path.d} fill="none" stroke="#696a45" strokeWidth={path.width + 2.2} opacity=".28" />
      <path d={path.d} fill="none" stroke="#ecd8a1" strokeWidth={path.width} opacity=".7" />
      <path d={path.d} fill="none" stroke="#fff2ba" strokeWidth=".7" opacity=".22" />
    </g>)}
  </g>;
}

export function PremiumLinksAtmosphere({ id, left, width, height }: { id: string; left: number; width: number; height: number }) {
  return <g pointerEvents="none" aria-hidden="true">
    <rect x={left} width={width} height={height} fill={`url(#${id}-links-light-band)`} />
    <rect x={left} width={width} height={height} fill={`url(#${id}-links-vertical-tone)`} />
  </g>;
}
