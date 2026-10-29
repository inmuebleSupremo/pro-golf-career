import { describe, expect, it } from "vitest";

import { scenicFrame } from "./stage-viewport";

describe("full-bleed scenic framing", () => {
  it("keeps the playable corridor at its previous pixel scale while extending scenery under chrome", () => {
    const viewport = { width: 1600, height: 900, safeTop: 72, safeBottom: 180 };
    const frame = scenicFrame(viewport, 440);
    const scale = (viewport.height - viewport.safeTop - viewport.safeBottom) / 440;
    expect(frame.safeTop * scale).toBeCloseTo(viewport.safeTop);
    expect((frame.height - frame.safeTop - frame.safeHeight) * scale).toBeCloseTo(viewport.safeBottom);
    expect(frame.height * frame.aspect).toBeCloseTo(viewport.width / scale);
  });

  it("adapts to a narrow safe area without changing its internal height", () => {
    const frame = scenicFrame({ width: 390, height: 844, safeTop: 80, safeBottom: 265 }, 300);
    expect(frame.safeHeight).toBe(300);
    expect(frame.aspect).toBeCloseTo(390 / 844);
    expect(frame.height).toBeGreaterThan(300);
  });
});
