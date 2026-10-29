import { mkdirSync, writeFileSync } from "node:fs";
import { join } from "node:path";

import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import type { HoleGeom } from "@/lib/play/hole-geometry";
import { PHYSICAL_BALL_RADIUS_YARDS, PHYSICAL_CUP_RADIUS_YARDS, cupDirectedLine, defaultPaceFeet, physicalPuttingProjector, physicalWorldMarkerRadius } from "@/lib/play/physical-putting";
import { distanceToPolygon, premiumPuttingFamily, puttingDecoration, puttingPolygonPath } from "@/lib/play/premium-putting";
import states from "@/lib/play/premium-putting-states.json";
import { PhysicalPuttingGreen } from "./physical-putting-green";

function state(family: keyof typeof states, feet: string): HoleGeom {
  const byDistance = states[family] as unknown as Record<string, { ball: HoleGeom["ball"] }>;
  return { ...(byDistance["60"] as unknown as HoleGeom), ball: byDistance[feet].ball };
}

const cases = (Object.keys(states) as (keyof typeof states)[]).map((family) => ({ family, hole: state(family, "60") }));

function render(hole: HoleGeom, read: boolean) {
  return renderToStaticMarkup(<PhysicalPuttingGreen hole={hole} ball={null}
    aimDirection={cupDirectedLine(hole.ball!.position, hole.geometry!.cup)} onAimDirection={() => {}}
    showReading={read} presentation="premium"/>);
}

describe("premium putting production presentation", () => {
  it("selects one shared green with four environment treatments and a legacy fallback", () => {
    for (const { family, hole } of cases) {
      expect(premiumPuttingFamily(hole, "premium")).toBe(family);
      expect(premiumPuttingFamily(hole, "polished")).toBeNull();
      const natural = render(hole, false), read = render(hole, true);
      expect(natural).toContain(`Premium ${family} putting terrain`);
      expect(read).toContain(`Premium ${family} putting terrain`);
      expect(natural).toContain('aria-label="Physical cup opening, 4.25 inches"');
      expect(read).toContain('aria-label="Green reading overlay"');
      expect(read).toContain('aria-label="Physical elevation contours"');
      expect(read).toContain('marker-end="url(#downhill-arrow-');
      expect(natural).not.toContain('marker-end="url(#downhill-arrow-');
      expect((natural.match(/<path/g) ?? []).length).toBeLessThan(350);
      const viewBox = natural.match(/viewBox="([^"]+)"/)?.[1].split(" ").map(Number);
      expect(viewBox?.[0]).toBeCloseTo(-51.67, 1);
      expect(viewBox?.[2]).toBeCloseTo(533.33, 1);
      expect(natural).toContain('aria-label="' + family + ' ground material"');
      expect(natural).toContain('stop-opacity=".82"');
      expect(natural).toContain('stop-opacity=".86"');
      expect(natural).toContain('<stop offset=".40" stop-color=');
      expect(natural).toContain('<stop offset=".62" stop-color=');
      const outputDir = process.env.PUTTING_CAPTURE_DIR;
      if (outputDir) {
        mkdirSync(outputDir, { recursive: true });
        for (const [mode, svg] of [["natural", natural], ["read-green", read]]) {
          writeFileSync(join(outputDir, `${family}-${mode}.svg`), svg.replace("<svg ", '<svg xmlns="http://www.w3.org/2000/svg" '));
        }
      }
    }
    const legacy = {...cases[0].hole, puttingPolicy: "LEGACY"};
    expect(premiumPuttingFamily(legacy, "premium")).toBeNull();
    expect(render(legacy, false)).not.toContain("Premium parkland putting terrain");
    expect(premiumPuttingFamily({...cases[0].hole, courseType: "WOODLAND", canonicalBiome: "WOODLAND"}, "premium")).toBe("parkland");
    expect(premiumPuttingFamily({...cases[0].hole, courseType: "WOODLAND", courseFamily: "HEATHLAND",
      canonicalBiome: "WOODLAND"}, "premium")).toBeNull();
    expect(premiumPuttingFamily({...cases[0].hole, geometry: {...cases[0].hole.geometry!,
      regions: cases[0].hole.geometry!.regions.filter((r) => r.surface !== "FRINGE")}}, "premium")).toBeNull();
  });

  it("keeps generated geometry, screen projection, scale, pace, and decoration clearance authoritative", () => {
    for (const { hole, family } of cases) {
      const green = hole.geometry!.regions.find((r) => r.surface === "GREEN")!.boundary;
      const fringe = hole.geometry!.regions.find((r) => r.surface === "FRINGE")!.boundary;
      const ball = hole.ball!.position, cup = hole.geometry!.cup;
      const projector = physicalPuttingProjector(ball, cup, green, 6);
      expect(projector.unproject(projector.project(ball)).x).toBeCloseTo(ball.x, 8);
      expect(projector.unproject(projector.project(cup)).y).toBeCloseTo(cup.y, 8);
      expect(Math.hypot(projector.project(ball).x - projector.project(cup).x,
        projector.project(ball).y - projector.project(cup).y)).toBeCloseTo(defaultPaceFeet(ball, cup) / 3 * projector.pixelsPerYard, 8);
      const svg = render(hole, false);
      expect(svg).toContain(puttingPolygonPath(green, projector));
      expect(svg).toContain(puttingPolygonPath(fringe, projector));
      expect(svg).toContain('pointer-events="none"');
      const decoration = puttingDecoration(hole, projector, family as "parkland" | "links" | "heathland" | "desert");
      expect(decoration.every((o) => distanceToPolygon(projector.unproject(o), green) > o.radius / projector.pixelsPerYard)).toBe(true);
      expect(decoration.every((o) => distanceToPolygon(projector.unproject(o), fringe) > o.radius / projector.pixelsPerYard)).toBe(true);
    }
  });

  it("frames representative short and long generated states without moving the cup", () => {
    for (const key of ["3", "23", "60"]) {
      const hole = state("parkland", key);
      const feet = defaultPaceFeet(hole.ball!.position, hole.geometry!.cup);
      expect(feet).toBeGreaterThan(Number(key) - 1.5);
      expect(feet).toBeLessThan(Number(key) + 1.5);
      const svg = render(hole, false);
      expect(svg).toContain('Physical putting green: choose a starting line by clicking the green');
      const outputDir = process.env.PUTTING_CAPTURE_DIR;
      if (outputDir && key !== "23") writeFileSync(join(outputDir, `parkland-${key}ft.svg`),
        svg.replace("<svg ", '<svg xmlns="http://www.w3.org/2000/svg" '));
    }
  });

  it("extends seeded scenery across the premium stage without moving physical coordinates", () => {
    const hole = state("parkland", "60");
    const green = hole.geometry!.regions.find((region) => region.surface === "GREEN")!.boundary;
    const projector = physicalPuttingProjector(hole.ball!.position, hole.geometry!.cup, green, 6);
    const scene = {x: -65, y: 0, width: 560, height: 300};
    const objects = puttingDecoration(hole, projector, "parkland", scene);
    expect(objects).toEqual(puttingDecoration(hole, projector, "parkland", scene));
    expect(objects.some((object) => object.x < 0 || object.x > projector.width)).toBe(true);
    expect(objects.every((object) => object.x >= scene.x && object.x <= scene.x + scene.width)).toBe(true);
    expect(objects.every((object) => distanceToPolygon(projector.unproject(object), green)
      > object.radius / projector.pixelsPerYard)).toBe(true);
    expect(projector.unproject(projector.project(hole.geometry!.cup)).x).toBeCloseTo(hole.geometry!.cup.x, 8);
  });

  it("keeps Links grass-led and free of decorative rocks without removing canonical bunkers", () => {
    const hole = state("links", "60");
    const green = hole.geometry!.regions.find((region) => region.surface === "GREEN")!.boundary;
    const projector = physicalPuttingProjector(hole.ball!.position, hole.geometry!.cup, green, 6);
    expect(puttingDecoration(hole, projector, "links").every((object) => object.kind === "fescue")).toBe(true);
    const svg = render(hole, false);
    expect(svg).toContain('#717b4b');
    expect(svg).toContain('#65794a');
    expect(svg).toContain('#c8ae82');
    for (const bunker of hole.geometry!.regions.filter((region) => region.surface === "BUNKER")) {
      expect(svg).toContain(puttingPolygonPath(bunker.boundary, projector));
    }
  });

  it("scales the flag while keeping the ball at its original physical radius and authoritative anchors", () => {
    const sizes = ["60", "23", "3"].map((feet) => {
      const hole = state("parkland", feet);
      const svg = render(hole, false);
      const flag = svg.match(/aria-label="Flag anchored at cup"><path d="M([^ ]+) ([^ ]+) v (-?[\d.]+)"/);
      const ball = svg.match(/aria-label="Golf ball, 1.68 inches">.*?<circle cx="([^"]+)" cy="([^"]+)" r="([^"]+)"/);
      expect(flag).not.toBeNull();
      expect(ball).not.toBeNull();
      const green = hole.geometry!.regions.find((region) => region.surface === "GREEN")!.boundary;
      const visualPadding = Number(feet) >= 45 ? 6 : Number(feet) >= 15 ? 3.5 : 2;
      const projector = physicalPuttingProjector(hole.ball!.position, hole.geometry!.cup, green, visualPadding);
      expect(Number(flag![1])).toBeCloseTo(projector.project(hole.geometry!.cup).x, 8);
      expect(Number(flag![2])).toBeCloseTo(projector.project(hole.geometry!.cup).y, 8);
      expect(Number(ball![1])).toBeCloseTo(projector.project(hole.ball!.position).x, 8);
      expect(Number(ball![2])).toBeCloseTo(projector.project(hole.ball!.position).y, 8);
      expect(Number(ball![3])).toBeCloseTo(physicalWorldMarkerRadius(PHYSICAL_BALL_RADIUS_YARDS, projector.pixelsPerYard), 8);
      expect(Number(ball![3]) / physicalWorldMarkerRadius(PHYSICAL_CUP_RADIUS_YARDS, projector.pixelsPerYard))
        .toBeCloseTo(1.68 / 4.25, 8);
      return {flagHeight: -Number(flag![3]), ballRadius: Number(ball![3])};
    });
    expect(sizes[2].flagHeight).toBeGreaterThan(sizes[1].flagHeight);
    expect(sizes[1].flagHeight).toBeGreaterThan(sizes[0].flagHeight);
    expect(sizes[2].ballRadius).toBeGreaterThan(sizes[0].ballRadius * 2);
    expect(sizes[0].flagHeight).toBeGreaterThanOrEqual(15);
    expect(sizes[2].flagHeight).toBeLessThanOrEqual(27);
  });
});
