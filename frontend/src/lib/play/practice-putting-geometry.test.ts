import { describe, expect, it } from "vitest";

import { practicePuttingEnvironmentViewBox, practicePuttingFocusViewBox, practicePuttingProjector,
  practicePuttingWideViewBox } from "./practice-putting-geometry";

const start = { x: (515 - 640) / 35, y: (360 - 565) / 35 };
const cups = [
  { x: 335, y: 213 }, { x: 855, y: 175 }, { x: 694, y: 374 },
  { x: 1010, y: 495 }, { x: 314, y: 514 },
];

describe("practice putting projection", () => {
  it("maps the approved five cup positions and starting point to reversible world coordinates", () => {
    expect(practicePuttingProjector.project(start)).toEqual({ x: 515, y: 565 });
    for (const cup of cups) {
      const world = practicePuttingProjector.unproject(cup);
      expect(practicePuttingProjector.project(world)).toEqual(cup);
    }
  });

  it("keeps the ready ball and each selected cup in the mobile focus view", () => {
    for (const cup of cups) {
      const view = practicePuttingFocusViewBox(start, practicePuttingProjector.unproject(cup), 390, 360);
      for (const point of [{ x: 515, y: 565 }, cup]) {
        expect(point.x).toBeGreaterThanOrEqual(view.x);
        expect(point.x).toBeLessThanOrEqual(view.x + view.width);
        expect(point.y).toBeGreaterThanOrEqual(view.y);
        expect(point.y).toBeLessThanOrEqual(view.y + view.height);
      }
    }
  });

  it("uses more desktop width at a uniform scale while retaining the full playable vertical span", () => {
    const view = practicePuttingWideViewBox(1366, 540);
    expect(view.width).toBe(1280);
    expect(view.height).toBe(580);
    expect(view.y).toBe(70);
    expect(view.y).toBeLessThanOrEqual(74);
    expect(view.y + view.height).toBeGreaterThanOrEqual(650);
    for (const cup of cups) {
      expect(cup.y).toBeGreaterThanOrEqual(view.y);
      expect(cup.y).toBeLessThanOrEqual(view.y + view.height);
    }
  });

  it("extends only landscape bounds to fill wide and tall viewports at the camera's existing scale", () => {
    const camera = practicePuttingWideViewBox(1600, 600);
    const wide = practicePuttingEnvironmentViewBox(camera, 1600, 600);
    expect(wide.width / wide.height).toBeCloseTo(1600 / 600);
    expect(wide.height).toBe(camera.height);
    expect(wide.x).toBeLessThan(camera.x);
    expect(wide.x + wide.width).toBeGreaterThan(camera.x + camera.width);

    const overview = { x: 0, y: 0, width: 1280, height: 720 };
    const tall = practicePuttingEnvironmentViewBox(overview, 390, 600);
    expect(tall.width / tall.height).toBeCloseTo(390 / 600);
    expect(tall.width).toBe(overview.width);
    expect(tall.y).toBeLessThan(overview.y);
    expect(tall.y + tall.height).toBeGreaterThan(overview.y + overview.height);
  });
});
