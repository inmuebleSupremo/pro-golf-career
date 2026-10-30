import { mkdirSync, writeFileSync } from "node:fs";
import { join } from "node:path";

import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import { Hole2d } from "./hole-2d";
import samples from "@/lib/play/course-family-preview-fixtures.json";
import corpus from "@/lib/play/principled-architecture-preview-fixtures.json";
import coastalCourse from "@/lib/play/coastal-links-course-fixture.json";
import type { HoleGeom } from "@/lib/play/hole-geometry";

const cases = samples as readonly { identity: string; hole: HoleGeom }[];

describe("actual Generator 8 course-family renders", () => {
  it("renders open ocean for coastal version 9 holes and retains an inland Links view", () => {
    for (const number of [7, 8, 9, 1]) {
      const item = coastalCourse.holes[number - 1];
      const svg = renderToStaticMarkup(<Hole2d hole={item.hole as HoleGeom} presentation="premium"
        stageViewport={{ width: 1600, height: 900, safeTop: 50, safeBottom: 0 }} />);
      expect(svg).toContain("<svg");
      expect(svg.includes('data-links-open-ocean="true"')).toBe(item.coastal);
      const outputDir = process.env.COASTAL_LINKS_CAPTURE_DIR;
      if (outputDir) {
        mkdirSync(outputDir, { recursive: true });
        writeFileSync(join(outputDir, `hole-${number}.svg`),
          svg.replace("<svg ", '<svg xmlns="http://www.w3.org/2000/svg" '));
      }
    }
  });
  it("extends the production SVG to the full viewport while retaining a safe course camera", () => {
    for (const sample of cases) {
      const svg = renderToStaticMarkup(<Hole2d hole={sample.hole} presentation="premium"
        stageViewport={{ width: 1600, height: 900, safeTop: 63, safeBottom: 157 }} />);
      const [,, width, height] = svg.match(/viewBox="([^"]+)"/)![1].split(" ").map(Number);
      expect(width / height).toBeCloseTo(1600 / 900, 8);
      expect(height).toBeGreaterThan(440);
      expect(svg).toContain('aria-label="Hole');
    }
  });

  it("renders both Parkland variations from backend geometry without changing scale or aim coordinates", () => {
    expect(cases.map((sample) => sample.identity)).toEqual(["PARKLAND", "WOODLAND"]);
    expect(cases[0].hole.length).toBe(cases[1].hole.length);
    expect(cases[0].hole.par).toBe(cases[1].hole.par);

    for (const sample of cases) {
      const svg = renderToStaticMarkup(<Hole2d hole={sample.hole} presentation="premium"
        previewAspect={1.3} aimPoint={sample.hole.geometry!.cup} className="w-full" />);
      expect(svg).toContain("<svg");
      expect(svg).toContain("viewBox=");
      expect(svg).toContain("premium");
      const outputDir = process.env.COURSE_FAMILY_CAPTURE_DIR;
      if (outputDir) {
        mkdirSync(outputDir, { recursive: true });
        writeFileSync(join(outputDir, `${sample.identity.toLowerCase()}-premium-v2.svg`),
          svg.replace("<svg ", '<svg xmlns="http://www.w3.org/2000/svg" '));
      }
    }
  });

  it("renders every hole in a generated Parkland course with connected scenic routes", () => {
    const course = corpus.find((entry) => entry.seed === "103" && entry.biome === "PARKLAND")!;
    expect(course.experiment.holes).toHaveLength(18);
    for (const { hole } of course.experiment.holes) {
      const svg = renderToStaticMarkup(<Hole2d hole={hole} presentation="premium"
        previewAspect={2.16} aimPoint={hole.geometry!.cup} className="w-full" />);
      expect(svg).toContain('data-property-path-kind="active"');
      const outputDir = process.env.PARKLAND_CONTACT_DIR;
      if (outputDir) {
        mkdirSync(outputDir, { recursive: true });
        writeFileSync(join(outputDir, `hole-${String(hole.holeNumber).padStart(2, "0")}.svg`),
          svg.replace("<svg ", '<svg xmlns="http://www.w3.org/2000/svg" '));
      }
    }
  }, 90_000);
});
