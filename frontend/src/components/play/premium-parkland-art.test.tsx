import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import { PremiumParklandBackdrop, PremiumParklandDefs, PremiumParklandLight, PremiumParklandProperty, PremiumParklandSurface } from "./premium-parkland-art";

describe("full-stage Parkland lighting", () => {
  it("keeps the horizontal atmosphere while anchoring ground colour to V1", () => {
    const defs = renderToStaticMarkup(<svg><PremiumParklandDefs artId="lighting-test"
      coreX={-105} coreWidth={430} height={440} fullStage /></svg>);
    expect(defs).toContain('<linearGradient id="lighting-test-premium-wide-rough" gradientUnits="userSpaceOnUse" x1="-105" y1="0" x2="239" y2="440">');
    expect(defs).toContain('<linearGradient id="lighting-test-premium-fullstage-atmosphere" x1="0" y1="0" x2="1" y2="0">');
    expect(defs).not.toContain('<radialGradient id="lighting-test-premium-fullstage-atmosphere"');

    const light = renderToStaticMarkup(<svg><PremiumParklandLight artId="lighting-test"
      x={-365} width={950} height={440} corridorPath="M0 0L220 440Z" fullStage /></svg>);
    expect(light).toContain("lighting-test-premium-fullstage-atmosphere");
    expect(light).not.toContain("<mask");
    expect(light).not.toContain("<path");
  });
});

describe("active-hole ground detail", () => {
  it("shares V1's fairway, green, bunker and tree-surface rendering", () => {
    for (const surface of ["FAIRWAY", "GREEN", "BUNKER", "TREES"]) {
      const region = { surface, path: "M0 0L10 0L10 10Z" };
      const v1 = renderToStaticMarkup(<svg><PremiumParklandSurface artId="shared" region={region} /></svg>);
      const v2 = renderToStaticMarkup(<svg><PremiumParklandSurface artId="shared" region={region} fullStage /></svg>);
      expect(v2).toBe(v1);
    }
    const rough = { surface: "PRIMARY_ROUGH", path: "M0 0L10 0L10 10Z" };
    const v1Rough = renderToStaticMarkup(<svg><PremiumParklandSurface artId="shared" region={rough} /></svg>);
    const v2Rough = renderToStaticMarkup(<svg><PremiumParklandSurface artId="shared" region={rough} fullStage /></svg>);
    expect(v1Rough).toContain("premium-rough-mottle");
    expect(v2Rough).not.toContain("premium-rough-mottle");
  });

  it("keeps the V1 seeded openings in the same scene coordinates on a wider stage", () => {
    const v1 = renderToStaticMarkup(<svg><PremiumParklandBackdrop artId="ground"
      x={-105} width={430} height={440} seed="hole-103" /></svg>);
    const v2 = renderToStaticMarkup(<svg><PremiumParklandBackdrop artId="ground"
      x={-365} width={950} height={440} seed="hole-103" fullStage coreX={-105} coreWidth={430} /></svg>);
    const openings = (markup: string) => [...markup.matchAll(/<g transform="rotate\([^>]+><ellipse[^>]+><\/ellipse><ellipse[^>]+><\/ellipse><\/g>/g)]
      .map(([group]) => group);

    expect(openings(v1)).toHaveLength(7);
    expect(openings(v2)).toEqual(openings(v1));
    expect(v2).not.toContain("premium-rough-mottle");
  });
});

describe("neighbouring property ground", () => {
  it("blends the first cut through existing rough and fringe materials", () => {
    const markup = renderToStaticMarkup(<svg><PremiumParklandProperty artId="property"
      plan={{ paths: [], building: null, glimpses: [{
        source: "fixture", sourcePar: 4, side: 1, orientation: "diagonal", angle: 30,
        pixelsPerYard: 1, tee: { x: 0, y: 0 }, cup: { x: 0, y: 0 }, clearing: [],
        regions: [{ surface: "FIRST_CUT", path: "M0 0L10 0L10 10Z", points: [] }],
      }] }} /></svg>);
    expect(markup).toContain("property-premium-rough");
    expect(markup).toContain("property-premium-fringe");
    expect(markup).toContain("property-premium-rough-grain");
    expect(markup).not.toContain("#24402a");
    expect(markup).toContain('pointer-events="none"');
  });
});
