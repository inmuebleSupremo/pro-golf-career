import { describe, expect, it } from "vitest";

import { canonicalRenderModel, pointerWorldTarget, projectCanonicalFlow, type CanonicalPlayingGeometry } from "./canonical-geometry";

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
  it("centres all authoritative extents in an optional presentation without changing inverse targeting", () => {
    const bent: CanonicalPlayingGeometry = {
      tee: { x: 4, y: 0 }, cup: { x: -18, y: 120 },
      playableBoundary: [
        { x: -45, y: 0 }, { x: 10, y: 0 }, { x: 42, y: 48 },
        { x: 25, y: 86 }, { x: 8, y: 138 }, { x: -43, y: 138 },
      ],
      regions: [{ surface: "BUNKER", boundary: [
        { x: 42, y: 48 }, { x: 48, y: 52 }, { x: 43, y: 59 },
      ] }],
    };
    const model = canonicalRenderModel(bent, { width: 220, height: 440, padding: 16, center: true });
    const projected = [...bent.playableBoundary, ...bent.regions[0].boundary].map(model.project);
    expect((Math.min(...projected.map((point) => point.x)) + Math.max(...projected.map((point) => point.x))) / 2)
      .toBeCloseTo(110, 8);
    for (const point of [bent.tee, bent.cup, ...bent.regions[0].boundary]) {
      expect(model.unproject(model.project(point)).x).toBeCloseTo(point.x, 8);
      expect(model.unproject(model.project(point)).y).toBeCloseTo(point.y, 8);
    }
  });
  it("maps a click through the actual SVG screen transform to the same world target", () => {
    const model = canonicalRenderModel(geometry);
    const world = { x: 13.25, y: 71.5 };
    const svg = model.project(world);
    // A screen transform with translation and non-uniform scale, as in a fitted viewport.
    const client = { x: svg.x * 2 + 100, y: svg.y * 1.5 + 30 };
    const selected = pointerWorldTarget(client, { a: 0.5, b: 0, c: 0, d: 2 / 3, e: -50, f: -20 }, model.unproject);
    expect(selected.x).toBeCloseTo(world.x, 8);
    expect(selected.y).toBeCloseTo(world.y, 8);
  });
  it("preserves the safe-area projection and inverse when the SVG grows beneath header and dock", () => {
    const original = canonicalRenderModel(geometry, { width: 220, height: 440, padding: 16, center: true });
    const full = canonicalRenderModel(geometry, { width: 220, height: 600, padding: 16,
      center: true, safeTop: 55, safeHeight: 440 });
    const world = { x: 11, y: 72 };
    expect(full.project(world).x).toBeCloseTo(original.project(world).x);
    expect(full.project(world).y).toBeCloseTo(original.project(world).y + 55);
    expect(full.unproject(full.project(world))).toEqual(expect.objectContaining({
      x: expect.closeTo(world.x, 8), y: expect.closeTo(world.y, 8),
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
