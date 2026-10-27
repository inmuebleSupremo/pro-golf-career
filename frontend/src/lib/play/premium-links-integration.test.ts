import { describe, expect, it } from "vitest";
import { canonicalRenderModel } from "./canonical-geometry";
import { premiumLinksCorridor } from "./premium-links-corridor";
import { isLinksRouteClear, linksRouteBlockers, premiumLinksPropertyPlan } from "./premium-links-property";
import { premiumLinksNeighborCorridor } from "./premium-links-neighbor-corridor";
import { routeSamples, type PropertyPath } from "./premium-parkland-property";
import course from "./premium-links-course-fixture.json";

function viewportHeight(geometry: typeof course.holes[number]["hole"]["geometry"]): number {
  const points = [...geometry.playableBoundary, ...geometry.regions.flatMap((region) => region.boundary)];
  const xs = points.map((p) => p.x), ys = points.map((p) => p.y);
  return Math.min(440, Math.max(260, Math.ceil((Math.max(...ys) - Math.min(...ys)) * 188
    / Math.max(1, Math.max(...xs) - Math.min(...xs)) + 32)));
}

function selfCrosses(points: readonly { x: number; y: number }[]): boolean {
  const cross = (a: typeof points[number], b: typeof points[number], c: typeof points[number]) =>
    (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
  for (let i = 0; i < points.length; i += 1) {
    const a = points[i], b = points[(i + 1) % points.length];
    for (let j = i + 2; j < points.length; j += 1) {
      if ((j + 1) % points.length === i) continue;
      const c = points[j], d = points[(j + 1) % points.length];
      if (cross(a, b, c) * cross(a, b, d) < 0
        && cross(c, d, a) * cross(c, d, b) < 0) return true;
    }
  }
  return false;
}

function pathSamples(path: PropertyPath) {
  return path.d.includes("C") ? routeSamples(path) : path.points;
}

function touches(point: { x: number; y: number }, path: PropertyPath): boolean {
  return pathSamples(path).some((sample) => Math.hypot(point.x - sample.x, point.y - sample.y) < .00001);
}

describe("approved Links geometry integration", () => {
  for (const { hole } of course.holes) {
    for (const aspect of [2.16, 16 / 9, .68]) {
      it(`hole ${hole.holeNumber} at aspect ${aspect}: every visible route clears all active and neighbour surfaces`, () => {
        const height = viewportHeight(hole.geometry);
        const model = canonicalRenderModel(hole.geometry, { width: 220, height, padding: 16, center: true });
        const width = Math.max(220, height * aspect), left = (220 - width) / 2;
        const plan = premiumLinksPropertyPlan(hole.geometry, model, hole.layoutSeed, hole.par,
          hole.holeNumber, left, width, height);
        const neighborCorridors = plan.glimpses.map(premiumLinksNeighborCorridor);
        expect(neighborCorridors.every(Boolean)).toBe(true);
        const outerBand = premiumLinksCorridor(hole.geometry, model, height)?.bands[0].boundary;
        const blockers = [...linksRouteBlockers(hole.geometry, model, plan.glimpses,
          neighborCorridors.flatMap((corridor) => corridor ? [corridor.outerBoundary] : [])),
        ...(outerBand ? [outerBand] : [])];
        if (aspect > 1) expect(plan.paths.length).toBeGreaterThan(0);
        expect(plan.paths.length).toBe(plan.glimpses.length * 2 + (plan.paths.length ? 1 : 0));
        for (const path of plan.paths) {
          expect(isLinksRouteClear(path, blockers, { left, right: left + width, height }, 2,
            path.kind === "junction" ? 8 : 16)).toBe(true);
        }
        for (let i = 0; i < plan.glimpses.length; i += 1) {
          const neighbor = plan.paths[1 + 2 * i], junction = plan.paths[2 + 2 * i];
          expect(neighbor.kind).toBe("neighbor");
          expect(junction.kind).toBe("junction");
          expect(touches(junction.points[0], plan.paths[0])).toBe(true);
          expect(touches(junction.points.at(-1)!, neighbor)).toBe(true);
          const corridor = neighborCorridors[i]!;
          expect(corridor.bands).toHaveLength(5);
          expect(corridor.bands.every((band) => (band.path.match(/ C/g) ?? []).length === 2
            && !selfCrosses(band.boundary))).toBe(true);
        }
      });
    }

    it(`hole ${hole.holeNumber}: every nested corridor has a continuous rounded rear cap`, () => {
      const height = viewportHeight(hole.geometry);
      const model = canonicalRenderModel(hole.geometry, { width: 220, height, padding: 16, center: true });
      const corridor = premiumLinksCorridor(hole.geometry, model, height);
      expect(corridor).not.toBeNull();
      expect(corridor?.bands).toHaveLength(5);
      expect(corridor?.bands.every((band) => (band.path.match(/ C/g) ?? []).length === 2
        && band.path.endsWith(" Z") && !band.path.includes("NaN"))).toBe(true);
      expect(corridor?.bands.every((band) => !selfCrosses(band.boundary))).toBe(true);
      expect(corridor?.bands.map((band) => band.shoulderWidth)).toEqual(
        [...corridor!.bands.map((band) => band.shoulderWidth)].sort((a, b) => b - a));
      expect(corridor?.bands.map((band) => band.capTop)).toEqual(
        [...corridor!.bands.map((band) => band.capTop)].sort((a, b) => a - b));
    });
  }

  it("deterministically connects meaningful property across the complete wide course", () => {
    const views = course.holes.map(({ hole }) => {
      const height = viewportHeight(hole.geometry);
      const model = canonicalRenderModel(hole.geometry, { width: 220, height, padding: 16, center: true });
      const width = height * 2.16, left = (220 - width) / 2;
      const args = [hole.geometry, model, hole.layoutSeed, hole.par, hole.holeNumber,
        left, width, height] as const;
      const first = premiumLinksPropertyPlan(...args);
      const second = premiumLinksPropertyPlan(...args);
      expect(second).toEqual(first);
      return first;
    });
    const counts = [0, 1, 2].map((count) => views.filter((view) => view.glimpses.length === count).length);
    const rejected = views.reduce((total, view) => ({
      attempted: total.attempted + view.audit.attempted,
      visibility: total.visibility + view.audit.rejectedVisibility,
      overlap: total.overlap + view.audit.rejectedOverlap,
      rounding: total.rounding + view.audit.rejectedRounding,
      route: total.route + view.audit.rejectedRoute,
      junction: total.junction + view.audit.rejectedJunction,
    }), { attempted: 0, visibility: 0, overlap: 0, rounding: 0, route: 0, junction: 0 });
    const connected = views.reduce((n, view) => n + view.glimpses.length, 0);
    expect(counts).toEqual([0, 8, 10]);
    expect(connected).toBe(28);
    expect(rejected.attempted).toBe(connected + rejected.visibility + rejected.overlap
      + rejected.rounding + rejected.route + rejected.junction);
    expect(views.every((view) => view.paths.length === 1 + view.glimpses.length * 2)).toBe(true);
  }, 60000);
});
