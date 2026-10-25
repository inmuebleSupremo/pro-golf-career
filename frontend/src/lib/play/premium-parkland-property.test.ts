import { describe, expect, it } from "vitest";

import { canonicalRenderModel } from "./canonical-geometry";
import { premiumDistantTrees, premiumTreePlans } from "./premium-parkland-decoration";
import { inPropertyClearing, isPremiumAimCoordinate, premiumPropertyPlan, type PropertyGlimpse } from "./premium-parkland-property";
import neighborGeometry from "./premium-parkland-neighbor-geometry.json";
import corpus from "./principled-architecture-preview-fixtures.json";

const geometry = {
  tee: { x: 0, y: 0 }, cup: { x: 27, y: 410 },
  playableBoundary: [{ x: -55, y: -20 }, { x: 75, y: -20 }, { x: 75, y: 430 }, { x: -55, y: 430 }],
  regions: [],
} as const;
const model = canonicalRenderModel(geometry, { width: 220, height: 440, padding: 16, center: true });
const plan = (seed: string, par: number, width = 930) =>
  premiumPropertyPlan(geometry, model, seed, par, 9, (220 - width) / 2, width, 440);

describe("premium Parkland property composition", () => {
  it("clears complete crowns from played surfaces but permits trees along the outer first cut", () => {
    const square = (left: number, right: number) => [
      { x: left, y: 0 }, { x: right, y: 0 }, { x: right, y: 100 }, { x: left, y: 100 },
    ];
    const firstCut = square(0, 100);
    const fairway = square(120, 220);
    const glimpse: PropertyGlimpse = { source: "fixture", sourcePar: 4, side: 1,
      orientation: "diagonal", angle: 30, pixelsPerYard: 1,
      tee: { x: 0, y: 0 }, cup: { x: 0, y: 0 }, clearing: [firstCut, fairway], regions: [
      { surface: "FIRST_CUT", points: firstCut, path: "" },
      { surface: "FAIRWAY", points: fairway, path: "" },
    ] };
    expect(inPropertyClearing(-7, 50, 8, glimpse)).toBe(false);
    expect(inPropertyClearing(-4, 50, 8, glimpse)).toBe(true);
    expect(inPropertyClearing(238, 50, 10, glimpse)).toBe(true);
    expect(inPropertyClearing(240, 50, 10, glimpse)).toBe(false);
  });

  it("is deterministic but changes with the hole seed", () => {
    expect(plan("hole-103", 4)).toEqual(plan("hole-103", 4));
    expect(plan("hole-103", 4)).not.toEqual(plan("hole-887", 4));
  });

  it("enforces the par caps, authentic scale, and exclusion from the active projection", () => {
    const orientations = new Set<string>();
    for (const par of [3, 4, 5]) {
      for (let seed = 0; seed < 40; seed += 1) {
        const property = plan(`hole-${seed}`, par);
        expect(property.glimpses.length).toBeLessThanOrEqual(Math.min(1, par === 3 ? 1 : 2));
        for (const glimpse of property.glimpses) {
          orientations.add(glimpse.orientation);
          const points = glimpse.regions.flatMap((region) => region.points);
          expect(glimpse.side < 0 ? Math.max(...points.map((point) => point.x)) < 0
            : Math.min(...points.map((point) => point.x)) > 220).toBe(true);
          expect(points.some((point) => point.y < 0 || point.y > 440)).toBe(true);
          const source = neighborGeometry.find((entry) => entry.source === glimpse.source)!;
          const sourceLength = Math.hypot(source.cup[0] - source.tee[0], source.cup[1] - source.tee[1]);
          expect(Math.hypot(glimpse.cup.x - glimpse.tee.x, glimpse.cup.y - glimpse.tee.y))
            .toBeCloseTo(sourceLength * glimpse.pixelsPerYard, 5);
          const sourceFairway = source.regions.find((region) => region.surface === "FAIRWAY")!;
          const renderedFairway = glimpse.regions.find((region) => region.surface === "FAIRWAY")!;
          expect(glimpse.clearing).toContain(renderedFairway.points);
          expect(inPropertyClearing(renderedFairway.points[0].x, renderedFairway.points[0].y, 8, glimpse)).toBe(true);
          const opposite = Math.floor(sourceFairway.points.length / 2);
          expect(Math.hypot(renderedFairway.points[0].x - renderedFairway.points[opposite].x,
            renderedFairway.points[0].y - renderedFairway.points[opposite].y)).toBeCloseTo(
            Math.hypot(sourceFairway.points[0][0] - sourceFairway.points[opposite][0],
              sourceFairway.points[0][1] - sourceFairway.points[opposite][1]) * glimpse.pixelsPerYard, 5);
          expect(inPropertyClearing(110, 220, 5, glimpse)).toBe(false);
          const edge = glimpse.clearing[0][0];
          expect(inPropertyClearing(edge.x, edge.y, 5, glimpse)).toBe(true);
          if (glimpse.orientation === "reverse") expect(glimpse.cup.y).toBeGreaterThan(glimpse.tee.y);
        }
      }
    }
    expect(orientations).toEqual(new Set(["reverse", "diagonal"]));
    expect(plan("hole-103", 5, 300).glimpses).toHaveLength(0);
  });

  it("omits a proposed neighbour when the protected scenic region leaves no valid placement", () => {
    const seed = Array.from({ length: 40 }, (_, index) => `hole-${index}`)
      .find((candidate) => plan(candidate, 4).glimpses.length > 0);
    expect(seed).toBeDefined();
    const blocked = premiumPropertyPlan(geometry, model, seed!, 4, 9, -355, 930, 440, [], -500, 700);
    expect(blocked.glimpses).toHaveLength(0);
  });

  it("rejects pointer targets in the decorative scenery", () => {
    expect(isPremiumAimCoordinate(110, 220, 440)).toBe(true);
    expect(isPremiumAimCoordinate(-10, 220, 440)).toBe(false);
    expect(isPremiumAimCoordinate(260, 220, 440)).toBe(false);
    expect(isPremiumAimCoordinate(110, -1, 440)).toBe(false);
  });

  it("places review glimpses outside protected V1 woodland or omits them", () => {
    const fixtureCases = [["103", 0], ["887", 0], ["103", 6], ["103", 1]] as const;
    let visible = 0;
    for (const [seed, holeIndex] of fixtureCases) {
      const course = corpus.find((entry) => entry.seed === seed && entry.biome === "PARKLAND")!;
      const hole = course.experiment.holes[holeIndex].hole;
      const playable = hole.geometry!;
      const points = [...playable.playableBoundary, ...playable.regions.flatMap((region) => region.boundary)];
      const xs = points.map((point) => point.x);
      const ys = points.map((point) => point.y);
      const viewHeight = Math.min(440, Math.max(260, Math.ceil((Math.max(...ys) - Math.min(...ys)) * 188
        / Math.max(1, Math.max(...xs) - Math.min(...xs)) + 32)));
      const render = canonicalRenderModel(playable, { width: 220, height: viewHeight, padding: 16, center: true });
      const protectedTrees = [
        ...premiumDistantTrees(playable, hole.layoutSeed, render, -105, 430, viewHeight),
        ...premiumTreePlans(playable, hole.layoutSeed, render).flatMap((region) => region.trees),
      ];
      const width = viewHeight * 1.8;
      const property = premiumPropertyPlan(playable, render, hole.layoutSeed, hole.par, hole.holeNumber,
        (220 - width) / 2, width, viewHeight, protectedTrees);
      expect(property.glimpses.length).toBeLessThanOrEqual(1);
      for (const glimpse of property.glimpses) {
        visible += 1;
        const points = glimpse.regions.flatMap((region) => region.points);
        expect(glimpse.side < 0 ? Math.max(...points.map((point) => point.x)) < -121
          : Math.min(...points.map((point) => point.x)) > 341).toBe(true);
        expect(protectedTrees.some((tree) => inPropertyClearing(tree.x, tree.y, tree.size, glimpse))).toBe(false);
      }
      expect(property.paths).toHaveLength(1);
      for (const path of property.paths) {
        const coordinates = path.d.split(/[MC ]+/).filter(Boolean).map(Number);
        const xs = [coordinates[0], coordinates[2], coordinates[4], coordinates[6]];
        expect(Math.max(...xs) <= -105 - path.width / 2 - 4
          || Math.min(...xs) >= 325 + path.width / 2 + 4).toBe(true);
      }
    }
    expect(visible).toBeGreaterThan(0);
  });
});
