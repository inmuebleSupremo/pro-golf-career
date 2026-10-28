import { mkdirSync, writeFileSync } from "node:fs";
import { join } from "node:path";

import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import type { HoleGeom } from "@/lib/play/hole-geometry";
import { cupDirectedLine, defaultPaceFeet, physicalPuttingProjector } from "@/lib/play/physical-putting";
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
      expect((natural.match(/<path/g) ?? []).length).toBeLessThan(150);
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
});
