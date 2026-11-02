import { ACTIVE_BAY_MAT } from "@/lib/play/practice-range-geometry";

/** Decorative SVG scenery. Interactive targets and ball flight use the same range projection separately. */
export function PracticeRangeScenery({ fieldPath }: { fieldPath: string }) {
  const stripeCount = 20;
  const trees = [
    [12, 37, 27], [55, 32, 23], [103, 27, 19], [151, 26, 23], [209, 25, 19],
    [274, 26, 24], [345, 23, 20], [412, 26, 24], [490, 23, 19], [563, 23, 25],
    [636, 24, 22], [709, 23, 19], [784, 25, 24], [861, 22, 18], [929, 26, 23],
    [997, 25, 20], [1057, 28, 25], [1113, 27, 23], [1164, 32, 26], [1208, 38, 30],
    [28, 156, 25], [2, 251, 29], [11, 355, 26], [-8, 473, 31], [10, 564, 25],
    [1173, 146, 26], [1203, 244, 30], [1182, 356, 26], [1210, 466, 33], [1182, 554, 27],
  ] as const;
  const bayCenters = [96, 264, 432, ACTIVE_BAY_MAT.x, 768, 936, 1104];

  return <g pointerEvents="none">
    <defs>
      <linearGradient id="range-landscape" x1="0" y1="0" x2=".88" y2="1">
        <stop stopColor="var(--range-landscape-light)" /><stop offset=".48" stopColor="var(--range-landscape-mid)" />
        <stop offset="1" stopColor="var(--range-landscape-dark)" />
      </linearGradient>
      <linearGradient id="range-turf" x1=".08" y1="0" x2=".85" y2="1">
        <stop stopColor="var(--range-turf-light)" /><stop offset=".26" stopColor="var(--range-turf-mid)" />
        <stop offset=".55" stopColor="var(--range-grass)" /><stop offset=".78" stopColor="var(--range-turf-mid)" />
        <stop offset="1" stopColor="var(--range-turf-dark)" />
      </linearGradient>
      <radialGradient id="range-sunwash" cx=".38" cy=".26" r=".82">
        <stop stopColor="var(--range-sunwash)" stopOpacity=".26" />
        <stop offset=".5" stopColor="var(--range-sunwash)" stopOpacity=".09" />
        <stop offset="1" stopColor="var(--range-sunwash)" stopOpacity="0" />
      </radialGradient>
      <linearGradient id="range-edge-shade">
        <stop stopColor="var(--range-edge-dark)" stopOpacity=".28" />
        <stop offset=".16" stopColor="var(--range-edge-dark)" stopOpacity=".02" />
        <stop offset=".78" stopColor="var(--range-edge-dark)" stopOpacity="0" />
        <stop offset="1" stopColor="var(--range-edge-dark)" stopOpacity=".29" />
      </linearGradient>
      <linearGradient id="range-bay-ground" x1="0" y1="0" x2=".5" y2="1">
        <stop stopColor="var(--range-bay-light)" /><stop offset=".29" stopColor="var(--range-bay-mid)" />
        <stop offset="1" stopColor="var(--range-bay-dark)" />
      </linearGradient>
      <linearGradient id="range-mat-fill" x1="0" y1="0" x2=".7" y2="1">
        <stop stopColor="var(--range-mat-light)" /><stop offset="1" stopColor="var(--range-mat-dark)" />
      </linearGradient>
      <radialGradient id="range-green-fill" cx=".32" cy=".26" r=".8">
        <stop stopColor="var(--range-green-light)" /><stop offset=".48" stopColor="var(--range-green)" />
        <stop offset="1" stopColor="var(--range-green-dark)" />
      </radialGradient>
      <radialGradient id="range-leaf-fill" cx=".32" cy=".26" r=".8">
        <stop stopColor="var(--range-leaf-light)" /><stop offset=".52" stopColor="var(--range-leaf-mid)" />
        <stop offset="1" stopColor="var(--range-leaf-dark)" />
      </radialGradient>
      <pattern id="range-turf-blades" width="53" height="45" patternUnits="userSpaceOnUse">
        <path d="M7 11l2-5 M29 33l2-4 M43 17l1-4" stroke="var(--range-grain-light)" strokeWidth=".8" opacity=".24" />
        <path d="M13 37l1-2 M38 7l2-3 M24 18l1-2" stroke="var(--range-grain-dark)" strokeWidth=".9" opacity=".18" />
      </pattern>
      <pattern id="range-bay-grit" width="37" height="24" patternUnits="userSpaceOnUse">
        <path d="M6 5h2 M25 10h3 M11 19h3" stroke="var(--range-grain-light)" strokeWidth="1" opacity=".15" />
      </pattern>
      <clipPath id="range-field-clip"><path d={fieldPath} /></clipPath>
      <mask id="range-landscape-only" maskUnits="userSpaceOnUse" x="0" y="0" width="1200" height="700">
        <rect width="1200" height="700" fill="white" />
        <path d={fieldPath} fill="black" />
      </mask>
    </defs>
    <rect width="1200" height="700" fill="url(#range-landscape)" />
    <path d="M0 0H1200V119Q1114 100 1040 105Q610 113 235 99Q125 102 0 118Z" fill="var(--range-landscape-dark)" opacity=".32" />
    <path d={fieldPath} fill="url(#range-turf)" />
    <g clipPath="url(#range-field-clip)">
      <rect y="78" width="1200" height="552" fill="url(#range-sunwash)" />
      {Array.from({ length: stripeCount }, (_, index) => {
        const farLeft = 95 + index * (1010 / stripeCount);
        const farRight = 95 + (index + 1) * (1010 / stripeCount);
        const nearLeft = -60 + index * (1320 / stripeCount);
        const nearRight = -60 + (index + 1) * (1320 / stripeCount);
        return <path key={index} d={`M${farLeft} 80L${farRight} 80L${nearRight} 630L${nearLeft} 630Z`}
          fill={index % 2 ? "var(--range-stripe-a)" : "var(--range-stripe-b)"} opacity={index % 2 ? ".05" : ".065"} />;
      })}
      <ellipse cx="280" cy="366" rx="203" ry="23" fill="var(--range-sunwash)" opacity=".035" />
      <ellipse cx="760" cy="447" rx="190" ry="17" fill="var(--range-sunwash)" opacity=".035" />
      <ellipse cx="900" cy="290" rx="106" ry="25" fill="var(--range-stripe-b)" opacity=".028" />
      <rect y="78" width="1200" height="552" fill="url(#range-turf-blades)" opacity=".62" />
      <rect y="78" width="1200" height="552" fill="url(#range-edge-shade)" />
    </g>
    <path d="M95 80Q45 290 -60 630 M1105 80Q1160 320 1260 630" fill="none" stroke="var(--range-edge)" strokeWidth="26" opacity=".3" />
    <path d="M95 80Q45 290 -60 630 M1105 80Q1160 320 1260 630" fill="none" stroke="var(--range-turf-light)" strokeWidth="5" opacity=".25" />
    <g mask="url(#range-landscape-only)">{trees.map(([x, y, radius], index) => <g key={index} transform={`translate(${x} ${y})`}>
      <ellipse cy={radius * .65} rx={radius * 1.08} ry={radius * .46} fill="var(--range-edge-dark)" opacity=".24" />
      <path d={`M${-radius} 0Q${-radius * 1.15} ${-radius * .75} ${-radius * .35} ${-radius * .78}Q0 ${-radius * 1.2} ${radius * .42} ${-radius * .55}Q${radius * 1.2} ${-radius * .38} ${radius * .9} ${radius * .38}Q0 ${radius * .83} ${-radius} ${radius * .35}Z`}
        fill="url(#range-leaf-fill)" />
      <ellipse cx={-radius * .3} cy={-radius * .2} rx={radius * .28} ry={radius * .15} fill="var(--range-leaf-light)" opacity=".17" />
      <ellipse cx={radius * .28} cy={radius * .12} rx={radius * .23} ry={radius * .12} fill="var(--range-leaf-mid)" opacity=".2" />
    </g>)}</g>
    <path d="M0 626H1200V700H0Z" fill="url(#range-bay-ground)" />
    <path d="M0 626H1200V641H0Z" fill="var(--range-deck-edge)" />
    <path d="M0 630H1200" stroke="var(--range-deck-highlight)" strokeWidth="3" opacity=".65" />
    <rect y="646" width="1200" height="54" fill="url(#range-bay-grit)" />
    <path d="M0 684H1200" stroke="var(--range-bay-dark)" strokeWidth="16" opacity=".43" />
    {bayCenters.map((center, index) => <g key={index}>
      <path d={`M${center - 63} 656H${center + 63}L${center + 70} 691H${center - 70}Z`} fill="var(--range-bay-shadow)" opacity=".66" />
      <path d={`M${center - 60} 657H${center + 60}L${center + 64} 680H${center - 64}Z`} fill="url(#range-mat-fill)"
        stroke={index === 3 ? "var(--range-active)" : "var(--range-mat-edge)"} strokeWidth={index === 3 ? 2.2 : 1.1} />
      {Array.from({ length: 10 }, (_, blade) => <path key={blade} d={`M${center - 54 + blade * 12} 664l2-3`}
        stroke="var(--range-grain-light)" strokeWidth=".8" opacity=".27" />)}
      {index < 6 && <path d={`M${center + 84} 651v31`} stroke="var(--range-divider)" strokeWidth="3" opacity=".8" />}
      {index !== 3 && <text x={center} y="694" fill="var(--range-bay-label)" textAnchor="middle" fontSize="10" fontWeight="600">{index + 1}</text>}
    </g>)}
  </g>;
}
