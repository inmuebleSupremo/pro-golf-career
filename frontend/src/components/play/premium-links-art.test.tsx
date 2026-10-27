import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import { PremiumLinksDefs, PremiumLinksLandscape, PremiumLinksProperty, PremiumLinksSurface } from "./premium-links-art";
import { canonicalRenderModel } from "@/lib/play/canonical-geometry";
import { premiumLinksPropertyPlan } from "@/lib/play/premium-links-property";
import sourceHoles from "@/lib/play/premium-links-neighbor-geometry.json";
import course from "@/lib/play/premium-links-course-fixture.json";

describe("Premium Links artwork", () => {
  it("uses a continuous stage and only supplied bunker footprints", () => {
    const ground = renderToStaticMarkup(<svg><PremiumLinksDefs id="links" left={-240} width={700} height={440} />
      <PremiumLinksLandscape id="links" left={-240} width={700} height={440} seed="fixed" /></svg>);
    expect(ground).toContain('width="700" height="440"');
    expect(ground).not.toContain("data-links-neighbor");
    const fairway = renderToStaticMarkup(<svg><PremiumLinksSurface id="links"
      region={{ surface: "FAIRWAY", path: "M0 0L3 0L3 3Z" }} /></svg>);
    const bunker = renderToStaticMarkup(<svg><PremiumLinksSurface id="links"
      region={{ surface: "BUNKER", path: "M1 1L2 1L2 2Z" }} /></svg>);
    expect(fairway).not.toContain("links-sand");
    expect(bunker).toContain("M1 1L2 1L2 2Z");
    expect(bunker).toContain("links-sand");
  });

  it("composes genuine generated Links neighbours at the active projection scale", () => {
    let shown = 0;
    for (const item of [course.holes[1], course.holes[2], course.holes[3], course.holes[8], course.holes[13], course.holes[16]]) {
      const geometry = item.hole.geometry;
      const model = canonicalRenderModel(geometry, { width: 220, height: 440, padding: 16, center: true });
      const plan = premiumLinksPropertyPlan(geometry, model, item.hole.layoutSeed, item.hole.par,
        item.hole.holeNumber, -365, 950, 440);
      shown += plan.glimpses.length;
      for (const glimpse of plan.glimpses) {
        expect(sourceHoles.some((source) => source.source === glimpse.source)).toBe(true);
        expect(glimpse.pixelsPerYard).toBeGreaterThan(0);
      }
      const markup = renderToStaticMarkup(<svg><PremiumLinksProperty id="links" plan={plan} /></svg>);
      expect(markup).toContain('pointer-events="none"');
      expect((markup.match(/data-links-neighbor-bands=/g) ?? [])).toHaveLength(plan.glimpses.length);
      expect((markup.match(/data-links-neighbor-band=/g) ?? [])).toHaveLength(plan.glimpses.length * 5);
      expect((markup.match(/data-links-walking-track="junction"/g) ?? [])).toHaveLength(plan.glimpses.length);
    }
    expect(shown).toBeGreaterThan(1);
  }, 30000);
});
