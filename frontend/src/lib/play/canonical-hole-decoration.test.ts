import { describe, expect, it } from "vitest";

import { planTreeCanopies, pointInPolygon } from "./canonical-hole-decoration";
import type { CanonicalPlayingGeometry } from "./canonical-geometry";

const treeBoundary = [
  { x: 20, y: 20 },
  { x: 62, y: 20 },
  { x: 68, y: 44 },
  { x: 46, y: 68 },
  { x: 18, y: 50 },
];

const geometry: CanonicalPlayingGeometry = {
  tee: { x: 0, y: 0 },
  cup: { x: 0, y: 140 },
  playableBoundary: [{ x: -40, y: 0 }, { x: 80, y: 0 }, { x: 80, y: 150 }, { x: -40, y: 150 }],
  regions: [
    { surface: "FAIRWAY", boundary: [{ x: -12, y: 0 }, { x: 12, y: 0 }, { x: 12, y: 140 }, { x: -12, y: 140 }] },
    { surface: "TREES", boundary: treeBoundary },
  ],
};

describe("planTreeCanopies", () => {
  it("is deterministic for the same backend seed and supplied tree polygon", () => {
    expect(planTreeCanopies(geometry, "parkland-42")).toEqual(planTreeCanopies(geometry, "parkland-42"));
  });

  it("places every canopy inside the authoritative tree polygon", () => {
    const [plan] = planTreeCanopies(geometry, "parkland-42");

    expect(plan.placements).not.toHaveLength(0);
    expect(plan.placements.every((placement) => pointInPolygon(placement, treeBoundary))).toBe(true);
  });

  it("does not create vegetation when canonical geometry has no tree terrain", () => {
    expect(planTreeCanopies({ ...geometry, regions: [geometry.regions[0]] }, "parkland-42")).toEqual([]);
  });
});
