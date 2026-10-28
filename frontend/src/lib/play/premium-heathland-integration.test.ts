import { describe, expect, it } from "vitest";
import { canonicalRenderModel } from "./canonical-geometry";
import { premiumHeathlandCorridor } from "./premium-heathland-corridor";
import { premiumHeathlandNeighborCorridor } from "./premium-heathland-neighbor-corridor";
import { heathlandRouteBlockers, isHeathlandRouteClear, premiumHeathlandPropertyPlan } from "./premium-heathland-property";
import { planHeathlandDecoration } from "@/components/play/premium-heathland-art";
import { routeSamples, type PropertyPath } from "./premium-parkland-property";
import course from "./premium-heathland-course-fixture.json";

type Point = { x: number; y: number };
function heightOf(geometry: typeof course.holes[number]["hole"]["geometry"]): number {
  const points = [...geometry.playableBoundary, ...geometry.regions.flatMap((r) => r.boundary)];
  const xs = points.map((p) => p.x), ys = points.map((p) => p.y);
  return Math.min(440, Math.max(260, Math.ceil((Math.max(...ys) - Math.min(...ys)) * 188
    / Math.max(1, Math.max(...xs) - Math.min(...xs)) + 32)));
}
function inside(p: Point, polygon: readonly Point[]): boolean {
  let value = false;
  for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
    const a = polygon[i], b = polygon[j];
    if ((a.y > p.y) !== (b.y > p.y)
      && p.x < (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x) value = !value;
  }
  return value;
}
function distance(p: Point, a: Point, b: Point): number {
  const dx = b.x - a.x, dy = b.y - a.y;
  const t = dx * dx + dy * dy ? Math.max(0, Math.min(1,
    ((p.x - a.x) * dx + (p.y - a.y) * dy) / (dx * dx + dy * dy))) : 0;
  return Math.hypot(p.x - a.x - t * dx, p.y - a.y - t * dy);
}
function clear(p: Point, radius: number, polygon: readonly Point[]): boolean {
  return !inside(p, polygon) && polygon.every((a, i) => distance(p, a, polygon[(i + 1) % polygon.length]) > radius);
}
function selfCrosses(points: readonly Point[]): boolean {
  const cross = (a: Point, b: Point, c: Point) =>
    (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
  for (let i = 0; i < points.length; i += 1) {
    const a = points[i], b = points[(i + 1) % points.length];
    for (let j = i + 2; j < points.length; j += 1) {
      if ((j + 1) % points.length === i) continue;
      const c = points[j], d = points[(j + 1) % points.length];
      if (cross(a, b, c) * cross(a, b, d) < 0 && cross(c, d, a) * cross(c, d, b) < 0) return true;
    }
  }
  return false;
}
function samples(path: PropertyPath): readonly Point[] {
  return path.d.includes("C") ? routeSamples(path) : path.points;
}

describe("Generator 8 Heathland production scene", () => {
  it("uses one genuine eighteen-hole fixture across par 3, par 4 and par 5", () => {
    expect(course.holes).toHaveLength(18);
    expect(new Set(course.holes.map(({ hole }) => hole.par))).toEqual(new Set([3, 4, 5]));
    expect(course.holes.every(({ hole }) => hole.courseFamily === "HEATHLAND"
      && hole.canonicalBiome === "HEATHLAND" && hole.courseGeneratorVersion === 8)).toBe(true);
  });

  it("preserves rounded corridors, connected clear routes and deterministic protected vegetation", () => {
    let neighbors = 0, flowers = 0;
    for (const { hole } of course.holes) {
      const geometry = hole.geometry, height = heightOf(geometry);
      const model = canonicalRenderModel(geometry, { width: 220, height, padding: 16, center: true });
      const width = Math.max(220, height * 2.16), left = (220 - width) / 2;
      const corridor = premiumHeathlandCorridor(geometry, model, height);
      expect(corridor?.bands).toHaveLength(4);
      expect(corridor?.bands.every((band) => band.path.endsWith(" Z")
        && !band.path.includes("NaN") && (band.path.match(/ C/g) ?? []).length === 2
        && !selfCrosses(band.boundary))).toBe(true);
      expect(corridor?.bands.map((band) => band.shoulderWidth)).toEqual(
        [...corridor!.bands.map((band) => band.shoulderWidth)].sort((a, b) => b - a));
      const plan = premiumHeathlandPropertyPlan(geometry, model, hole.layoutSeed, hole.par,
        hole.holeNumber, left, width, height);
      expect(premiumHeathlandPropertyPlan(geometry, model, hole.layoutSeed, hole.par,
        hole.holeNumber, left, width, height)).toEqual(plan);
      neighbors += plan.glimpses.length;
      const outer = plan.glimpses.map(premiumHeathlandNeighborCorridor).flatMap((c) => c ? [c.outerBoundary] : []);
      expect(outer).toHaveLength(plan.glimpses.length);
      expect(plan.glimpses.every((g) => premiumHeathlandNeighborCorridor(g)?.bands.every((b) =>
        !selfCrosses(b.boundary)))).toBe(true);
      expect(plan.paths.length).toBe(plan.glimpses.length * 2 + (plan.paths.length ? 1 : 0));
      const blockers = [...heathlandRouteBlockers(geometry, model, plan.glimpses, outer),
        ...(corridor ? [corridor.bands[0].boundary] : [])];
      for (const path of plan.paths) expect(isHeathlandRouteClear(path, blockers,
        { left, right: left + width, height }, 2, path.kind === "junction" ? 8 : 16)).toBe(true);
      for (let i = 0; i < plan.glimpses.length; i += 1) {
        const junction = plan.paths[2 + i * 2], neighbor = plan.paths[1 + i * 2];
        expect(junction.kind).toBe("junction");
        expect(samples(plan.paths[0]).some((p) => Math.hypot(p.x - junction.points[0].x, p.y - junction.points[0].y) < .001)).toBe(true);
        expect(samples(neighbor).some((p) => Math.hypot(p.x - junction.points.at(-1)!.x,
          p.y - junction.points.at(-1)!.y) < .001)).toBe(true);
      }
      const vegetation = planHeathlandDecoration(geometry, model, corridor, plan, hole.layoutSeed,
        left, width, height);
      expect(planHeathlandDecoration(geometry, model, corridor, plan, hole.layoutSeed,
        left, width, height)).toEqual(vegetation);
      flowers += vegetation.filter((v) => v.kind === "heather" || v.kind === "gorse").length;
      for (const v of vegetation.filter((v) => v.kind !== "canonical-pine")) {
        expect(clear(v, v.radius * 1.35, corridor!.bands[0].boundary)).toBe(true);
        expect(outer.every((polygon) => clear(v, v.radius * 1.35, polygon))).toBe(true);
        expect(geometry.regions.filter((r) => ["GREEN", "TEE_BOX", "BUNKER", "WATER"].includes(r.surface))
          .every((r) => clear(v, v.radius, r.boundary.map(model.project)))).toBe(true);
      }
      for (const v of vegetation.filter((v) => v.kind === "canonical-pine")) {
        expect(geometry.regions.filter((r) => ["GREEN", "TEE_BOX", "FAIRWAY", "FIRST_CUT", "BUNKER", "WATER"].includes(r.surface))
          .every((r) => clear(v, v.radius, r.boundary.map(model.project)))).toBe(true);
      }
    }
    expect(neighbors).toBeGreaterThan(0);
    expect(flowers).toBeGreaterThan(100);
  }, 120000);

  it("keeps narrow par 3, par 4 and par 5 layouts clear", () => {
    for (const index of [0, 3, 6]) {
      const { hole } = course.holes[index];
      const height = heightOf(hole.geometry);
      const model = canonicalRenderModel(hole.geometry, { width: 220, height, padding: 16, center: true });
      const width = 220, left = 0;
      const plan = premiumHeathlandPropertyPlan(hole.geometry, model, hole.layoutSeed, hole.par,
        hole.holeNumber, left, width, height);
      const corridor = premiumHeathlandCorridor(hole.geometry, model, height);
      const blockers = [...heathlandRouteBlockers(hole.geometry, model, plan.glimpses,
        plan.glimpses.map(premiumHeathlandNeighborCorridor).flatMap((c) => c ? [c.outerBoundary] : [])),
        ...(corridor ? [corridor.bands[0].boundary] : [])];
      expect(plan.paths.every((path) => isHeathlandRouteClear(path, blockers,
        { left, right: left + width, height }, 2, path.kind === "junction" ? 8 : 16))).toBe(true);
      expect(planHeathlandDecoration(hole.geometry, model, corridor, plan, hole.layoutSeed,
        left, width, height).every((v) => v.kind === "canonical-pine"
          || clear(v, v.radius, corridor!.bands[0].boundary))).toBe(true);
    }
  }, 30000);
});
