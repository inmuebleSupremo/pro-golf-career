import { describe, expect, it } from "vitest";

import { canonicalRenderModel, projectCanonicalFlow, type CanonicalPlayingGeometry } from "./canonical-geometry";

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
  it("round-trips off-centre canonical coordinates through the display mapping", () => {
    const model = canonicalRenderModel(geometry);
    const source = { x: 13.25, y: 171.5 };
    expect(model.unproject(model.project(source))).toEqual(expect.objectContaining({
      x: expect.closeTo(source.x, 8), y: expect.closeTo(source.y, 8),
    }));
  });
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

  it("projects canonical wind flow toward the matching SVG direction without implying a compass", () => {
    const model = canonicalRenderModel(geometry);
    expect(projectCanonicalFlow(geometry.tee, { x: 1, y: 0 }, model.project)).toEqual({
      x: expect.closeTo(1, 8), y: expect.closeTo(0, 8),
    });
    expect(projectCanonicalFlow(geometry.tee, { x: 0, y: 1 }, model.project)).toEqual({
      x: expect.closeTo(0, 8), y: expect.closeTo(-1, 8),
    });
  });
});
