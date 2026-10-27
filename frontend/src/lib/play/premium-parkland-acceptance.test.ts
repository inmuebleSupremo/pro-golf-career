import { expect, it } from "vitest";

import { canonicalRenderModel } from "./canonical-geometry";
import { premiumDistantTrees, premiumTreePlans } from "./premium-parkland-decoration";
import { premiumPropertyPlan } from "./premium-parkland-property";
import corpus from "./principled-architecture-preview-fixtures.json";

/** Full generated Generator 8 course, measured at the same 2.16 desktop stage ratio as the property preview. */
it("measures connected golf-property access across all 18 Parkland holes", () => {
  const course = corpus.find((entry) => entry.seed === "103" && entry.biome === "PARKLAND")!;
  const rows = course.experiment.holes.map(({ hole }) => {
    const geometry = hole.geometry!;
    const points = [...geometry.playableBoundary, ...geometry.regions.flatMap((region) => region.boundary)];
    const xs = points.map((point) => point.x), ys = points.map((point) => point.y);
    const height = Math.min(440, Math.max(260, Math.ceil((Math.max(...ys) - Math.min(...ys)) * 188
      / Math.max(1, Math.max(...xs) - Math.min(...xs)) + 32)));
    const model = canonicalRenderModel(geometry, { width: 220, height, padding: 16, center: true });
    const trees = [...premiumDistantTrees(geometry, hole.layoutSeed, model, -105, 430, height, 260, "open"),
      ...premiumTreePlans(geometry, hole.layoutSeed, model, "open").flatMap((region) => region.trees)];
    const width = height * 2.16;
    const property = premiumPropertyPlan(geometry, model, hole.layoutSeed, hole.par, hole.holeNumber,
      (220 - width) / 2, width, height, trees);
    const active = property.paths[0];
    expect(active?.points[0].y).toBeGreaterThan(height);
    expect(active?.points.at(-1)?.y).toBeLessThan(0);
    expect(Math.abs(active!.points[1].x - model.project(geometry.tee).x)).toBeLessThan(100);
    expect(Math.abs(active!.points.at(-2)!.x - model.project(geometry.cup).x)).toBeLessThan(100);
    return { hole: hole.holeNumber, par: hole.par, path: Number(property.paths.some((path) => !path.kind)),
      routeSide: property.paths[0]?.side,
      neighbors: property.glimpses.length, sides: property.glimpses.map((glimpse) => glimpse.side).join(","),
      orientations: property.glimpses.map((glimpse) => glimpse.orientation).join(","),
      connected: property.paths.filter((path) => path.kind === "junction").length };
  });
  console.table(rows);
  console.log("Property totals", {
    paths: rows.reduce((sum, row) => sum + row.path, 0),
    neighbors: [0, 1, 2].map((count) => rows.filter((row) => row.neighbors === count).length),
    connections: rows.reduce((sum, row) => sum + row.connected, 0),
  });
  expect(rows).toHaveLength(18);
  expect(rows.reduce((sum, row) => sum + row.path, 0)).toBe(18);
  expect(rows.filter((row) => row.par === 3).every((row) => row.path === 1)).toBe(true);
  expect([0, 1, 2].map((count) => rows.filter((row) => row.neighbors === count).length)).toEqual([7, 9, 2]);
  expect(rows.reduce((sum, row) => sum + row.connected, 0)).toBe(13);
  expect(rows.every((row) => row.neighbors === row.connected)).toBe(true);
}, 30_000);
