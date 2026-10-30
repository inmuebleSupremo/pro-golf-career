import { mkdirSync, writeFileSync } from "node:fs";
import { join } from "node:path";

import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import { Hole2d } from "./hole-2d";
import { PhysicalPuttingGreen } from "./physical-putting-green";
import { cupDirectedLine } from "@/lib/play/physical-putting";
import type { HoleGeom } from "@/lib/play/hole-geometry";
import familyCourse from "@/lib/play/course-family-preview-fixtures.json";
import coastalCourse from "@/lib/play/coastal-links-course-fixture.json";
import heathlandCourse from "@/lib/play/premium-heathland-course-fixture.json";
import desertCourse from "@/lib/play/premium-desert-course-fixture.json";
import puttingStates from "@/lib/play/premium-putting-states.json";

const viewport = { width: 1600, height: 900, safeTop: 63, safeBottom: 157 };
const samples = [
  ["parkland", familyCourse.find((item) => item.identity === "PARKLAND")!.hole, viewport],
  ["woodland", familyCourse.find((item) => item.identity === "WOODLAND")!.hole, viewport],
  ["inland-links", coastalCourse.holes[0].hole, viewport],
  ["coastal-links", coastalCourse.holes[6].hole, viewport],
  ["coastal-links-bay", coastalCourse.holes[8].hole, viewport],
  ["heathland", heathlandCourse.holes[3].hole, viewport],
  ["desert", desertCourse.holes[3].hole, viewport],
] as const;
const fadeSuffix: Record<string, string> = {
  parkland: "premium-fullstage-atmosphere",
  woodland: "premium-fullstage-atmosphere",
  "inland-links": "links-light-band",
  "coastal-links": "links-light-band",
  "coastal-links-bay": "links-light-band",
  heathland: "heath-light",
  desert: "desert-light",
};

function sideStops(svg: string, suffix: string) {
  const gradient = svg.match(
    new RegExp(`<linearGradient id="[^"]+-${suffix}"[^>]*>(.*?)<\\/linearGradient>`),
  )?.[1];
  expect(gradient).toBeDefined();
  return [
    ...gradient!.matchAll(/<stop offset="([^"]+)" stop-color="[^"]+" stop-opacity="([^"]+)"/g),
  ].map(([, offset, opacity]) => `${offset}:${opacity}`);
}

describe("full-stage side focus", () => {
  it("uses one horizontal stop curve across premium hole families", () => {
    const outputDir = process.env.SIDE_FOCUS_CAPTURE_DIR;
    if (outputDir) mkdirSync(outputDir, { recursive: true });
    let fullHoleStops: string[] | undefined;
    for (const [name, raw, stageViewport] of samples) {
      const svg = renderToStaticMarkup(
        <Hole2d hole={raw as HoleGeom} presentation="premium" stageViewport={stageViewport} />,
      );
      expect(svg).toContain("<svg");
      expect(svg).toContain("viewBox=");
      const stops = sideStops(svg, fadeSuffix[name]);
      expect(stops).toHaveLength(10);
      expect(stops).toContain("0.240:0.920");
      expect(stops).toContain("0.400:0.000");
      expect(stops).toContain("0.600:0.000");
      expect(stops).toContain("0.760:0.920");
      if (name.startsWith("coastal")) expect(svg).toContain('data-links-open-ocean="true"');
      if (name === "inland-links") expect(svg).not.toContain('data-links-open-ocean="true"');
      if (fullHoleStops) expect(stops).toEqual(fullHoleStops);
      else fullHoleStops = stops;
      if (outputDir)
        writeFileSync(
          join(outputDir, `${name}.svg`),
          svg.replace("<svg ", '<svg xmlns="http://www.w3.org/2000/svg" '),
        );
    }
    const putt = (puttingStates.links as Record<string, unknown>)["60"] as HoleGeom;
    const putting = renderToStaticMarkup(
      <PhysicalPuttingGreen
        hole={putt}
        ball={null}
        aimDirection={cupDirectedLine(putt.ball!.position, putt.geometry!.cup)}
        onAimDirection={() => {}}
        showReading={false}
        presentation="premium"
        stageViewport={viewport}
      />,
    );
    expect(putting).toContain("Premium links putting terrain");
    expect(sideStops(putting, "side")).toContain("0.350:0.000");
    if (outputDir)
      writeFileSync(
        join(outputDir, "links-putting.svg"),
        putting.replace("<svg ", '<svg xmlns="http://www.w3.org/2000/svg" '),
      );
  }, 90_000);
});
