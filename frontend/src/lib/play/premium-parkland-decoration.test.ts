import { describe, expect, it } from "vitest";

import { canonicalRenderModel } from "./canonical-geometry";
import { pointInPolygon } from "./canonical-hole-decoration";
import { premiumDistantTrees, premiumOpenings, premiumSurfacePrecedence, premiumTreePlans } from "./premium-parkland-decoration";

const geometry = {
  tee: { x: 0, y: 0 }, cup: { x: 0, y: 250 },
  playableBoundary: [{ x: -50, y: -20 }, { x: 50, y: -20 }, { x: 50, y: 270 }, { x: -50, y: 270 }],
  regions: [
    { surface: "TREES", boundary: [{ x: -42, y: 40 }, { x: -15, y: 40 }, { x: -15, y: 145 }, { x: -42, y: 145 }] },
    { surface: "BUNKER", boundary: [{ x: -25, y: 115 }, { x: -12, y: 115 }, { x: -12, y: 130 }, { x: -25, y: 130 }] },
  ],
} as const;

describe("premium Parkland decoration", () => {
  const model = canonicalRenderModel(geometry);

  it("keeps reproducible canopy centers inside the authoritative tree region", () => {
    const first = premiumTreePlans(geometry, "hole-103", model);
    expect(first).toEqual(premiumTreePlans(geometry, "hole-103", model));
    expect(first[0].trees.length).toBeGreaterThan(4);
    expect(first).not.toEqual(premiumTreePlans(geometry, "hole-887", model));
    for (const tree of first[0].trees) {
      expect(pointInPolygon(model.unproject(tree), geometry.regions[0].boundary)).toBe(true);
    }
  });

  it("places background trees outside the playable polygon", () => {
    const trees = premiumDistantTrees(geometry, "hole-103", model, -105, 430, 440);
    expect(trees.length).toBeGreaterThan(0);
    expect(trees).toEqual(premiumDistantTrees(geometry, "hole-103", model, -105, 430, 440));
    expect(premiumOpenings("hole-103", -105, 430, 440)).toEqual(premiumOpenings("hole-103", -105, 430, 440));
    for (const tree of trees) {
      expect(pointInPolygon(model.unproject(tree), geometry.playableBoundary)).toBe(false);
    }
  });

  it("paints overlapping hazards over tree and fairway surfaces", () => {
    expect(premiumSurfacePrecedence("WATER")).toBeLessThan(premiumSurfacePrecedence("BUNKER"));
    expect(premiumSurfacePrecedence("BUNKER")).toBeLessThan(premiumSurfacePrecedence("TREES"));
    expect(premiumSurfacePrecedence("TREES")).toBeLessThan(premiumSurfacePrecedence("FAIRWAY"));
  });
});
