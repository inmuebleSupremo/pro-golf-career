import { describe, expect, it } from "vitest";
import { renderToStaticMarkup } from "react-dom/server";
import { mkdirSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { Hole2d } from "./hole-2d";
import type { HoleGeom } from "@/lib/play/hole-geometry";
import course from "@/lib/play/premium-desert-course-fixture.json";

describe("Premium Desert SVG production composition", () => {
  it("renders all real course holes with excavated canonical bunkers and connected property", () => {
    let connected = 0;
    let maxElements = 0;
    const outputDir = process.env.DESERT_CONTACT_DIR;
    if (outputDir) mkdirSync(outputDir, { recursive: true });
    for (const { hole: raw } of course.holes) {
      const hole = raw as HoleGeom;
      const svg = renderToStaticMarkup(<Hole2d hole={hole} presentation="premium"
        previewAspect={2.16} aimPoint={hole.geometry!.cup} className="w-full" />);
      expect(svg).toContain("data-desert-cap-band=\"4\"");
      expect(svg).toContain("-desert-bunker-cutouts");
      expect(svg).toContain("data-desert-walking-track=\"active\"");
      expect(svg).not.toContain("NaN");
      const bunkerCount = hole.geometry!.regions.filter((r) => r.surface === "BUNKER").length;
      expect((svg.match(/fill="black" stroke="black"/g) ?? []).length).toBeGreaterThanOrEqual(bunkerCount);
      connected += (svg.match(/data-desert-connected="true"/g) ?? []).length;
      maxElements = Math.max(maxElements, (svg.match(/<\w+/g) ?? []).length);
      if (outputDir) writeFileSync(join(outputDir, `hole-${String(hole.holeNumber).padStart(2, "0")}.svg`),
        svg.replace("<svg ", '<svg xmlns="http://www.w3.org/2000/svg" '));
      if (outputDir && hole.holeNumber === 4) {
        const current = renderToStaticMarkup(<Hole2d hole={hole} presentation="polished"
          aimPoint={hole.geometry!.cup} className="w-full" />);
        writeFileSync(join(outputDir, "current-hole-04.svg"),
          current.replace("<svg ", '<svg xmlns="http://www.w3.org/2000/svg" '));
        const mobile = renderToStaticMarkup(<Hole2d hole={hole} presentation="premium"
          previewAspect={.68} aimPoint={hole.geometry!.cup} className="w-full" />);
        writeFileSync(join(outputDir, "mobile-hole-04.svg"),
          mobile.replace("<svg ", '<svg xmlns="http://www.w3.org/2000/svg" '));
      }
    }
    expect(connected).toBeGreaterThan(0);
    expect(maxElements).toBeLessThan(10000);
  }, 120000);
});
