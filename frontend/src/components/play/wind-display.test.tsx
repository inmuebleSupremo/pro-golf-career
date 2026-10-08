import { readFileSync } from "node:fs";
import { renderToStaticMarkup } from "react-dom/server";

import { describe, expect, it } from "vitest";

import { WindDisplay, windDisplayGeometry } from "./wind-display";
import { WindDisplayPreview } from "./wind-display-preview";
import { canonicalRenderModel, type CanonicalPlayingGeometry } from "../../lib/play/canonical-geometry";

const geometry: CanonicalPlayingGeometry = {
  tee: { x: 0, y: 0 },
  cup: { x: 0, y: 100 },
  playableBoundary: [{ x: -20, y: 0 }, { x: 20, y: 0 }, { x: 20, y: 110 }, { x: -20, y: 110 }],
  regions: [],
};

const model = canonicalRenderModel(geometry);

describe("WindDisplay", () => {
  it.each([
    ["+X", { x: 5, y: 0 }, "x", 1],
    ["-X", { x: -5, y: 0 }, "x", -1],
    ["+Y", { x: 0, y: 5 }, "y", -1],
    ["-Y", { x: 0, y: -5 }, "y", 1],
  ] as const)("projects canonical %s to the symmetric arrowhead tip", (_name, flow, axis, expectedSign) => {
    const display = windDisplayGeometry({ ...flow, magnitude: 5, unit: "EFFECTIVE_YARDS" }, model, geometry.tee)!;
    const delta = display.tip[axis] - display.start[axis];
    expect(Math.sign(delta)).toBe(expectedSign);
    expect((display.firstWing.x + display.secondWing.x) / 2).toBeCloseTo(
      display.tip.x - (display.tip.x - display.start.x) / 16 * 5,
      10,
    );
    expect((display.firstWing.y + display.secondWing.y) / 2).toBeCloseTo(
      display.tip.y - (display.tip.y - display.start.y) / 16 * 5,
      10,
    );
  });

  it("renders a labelled SVG arrow from server-shaped effective wind", () => {
    const markup = renderToStaticMarkup(<svg><WindDisplay wind={{ x: 0, y: 5, magnitude: 5, unit: "EFFECTIVE_YARDS" }} model={model} tee={geometry.tee} /></svg>);
    expect(markup).toContain('data-wind-stem="true"');
    expect(markup).toContain('data-wind-arrowhead="true"');
    expect(markup).toContain("Wind toward");
    expect(markup).toContain("5.0 effective yd");
  });

  it("keeps the development preview isolated from production and gameplay state", () => {
    const previewMarkup = renderToStaticMarkup(<WindDisplayPreview />);
    expect(previewMarkup.match(/data-wind-stem="true"/g)).toHaveLength(4);

    const page = readFileSync(new URL("../../app/dev/wind-preview/page.tsx", import.meta.url), "utf8");
    const preview = readFileSync(new URL("./wind-display-preview.tsx", import.meta.url), "utf8");
    expect(page).toContain('process.env.NODE_ENV !== "development"');
    expect(page).toContain("notFound()");
    expect(preview).not.toContain("fetch(");
    expect(preview).not.toContain("useMutation");
  });
});
