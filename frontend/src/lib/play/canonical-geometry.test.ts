import { describe, expect, it } from "vitest";

import { canonicalRenderModel, type CanonicalPlayingGeometry } from "./canonical-geometry";

const geometry: CanonicalPlayingGeometry = {
  tee: { x: 0, y: 0 },
  cup: { x: 0, y: 100 },
  playableBoundary: [
    { x: -20, y: 0 },
    { x: 20, y: 0 },
    { x: 20, y: 110 },
    { x: -20, y: 110 },
  ],
  regions: [
    {
      surface: "FAIRWAY",
      boundary: [
        { x: -8, y: 0 },
        { x: 8, y: 0 },
        { x: 8, y: 85 },
        { x: -8, y: 85 },
      ],
    },
    {
      surface: "WATER",
      boundary: [
        { x: 9, y: 35 },
        { x: 18, y: 35 },
        { x: 18, y: 60 },
        { x: 9, y: 60 },
      ],
    },
  ],
};

describe("canonicalRenderModel", () => {
  it("projects every terrain path from the API geometry without inventing terrain", () => {
    const model = canonicalRenderModel(geometry);

    expect(model.terrain.map((region) => region.surface)).toEqual(["FAIRWAY", "WATER"]);
    expect(model.playableBoundaryPath).toContain("M16.0 424.0");
    expect(model.terrain[1].path).toContain("M123.6 294.2");
  });

  it("moves the rendered terrain when the canonical API polygon changes", () => {
    const original = canonicalRenderModel(geometry).terrain[1].path;
    const moved: CanonicalPlayingGeometry = {
      ...geometry,
      regions: [
        geometry.regions[0],
        {
          ...geometry.regions[1],
          boundary: geometry.regions[1].boundary.map((p) => ({ ...p, x: p.x - 8 })),
        },
      ],
    };

    expect(canonicalRenderModel(moved).terrain[1].path).not.toEqual(original);
  });

  it("renders a setup-specific narrower fairway from the supplied geometry", () => {
    const standardPath = canonicalRenderModel(geometry).terrain[0].path;
    const tightSetup: CanonicalPlayingGeometry = {
      ...geometry,
      playableBoundary: geometry.playableBoundary.map((point) => ({ ...point, x: point.x * 0.75 })),
      regions: [
        {
          ...geometry.regions[0],
          boundary: geometry.regions[0].boundary.map((point) => ({ ...point, x: point.x * 0.6 })),
        },
        geometry.regions[1],
      ],
    };

    const model = canonicalRenderModel(tightSetup);
    expect(model.terrain.map((region) => region.surface)).toEqual(["FAIRWAY", "WATER"]);
    expect(model.terrain[0].path).not.toEqual(standardPath);
    expect(model.playableBoundaryPath).not.toEqual(
      canonicalRenderModel(geometry).playableBoundaryPath,
    );
  });
});
