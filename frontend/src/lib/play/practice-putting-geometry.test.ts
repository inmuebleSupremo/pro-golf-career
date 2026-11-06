import { describe, expect, it } from "vitest";

import { pointerWorldTarget } from "./canonical-geometry";
import { PRACTICE_PUTTING_AIM_STEP_DEGREES, practicePuttingAimDirection, practicePuttingEnvironmentViewBox,
  practicePuttingFocusViewBox, practicePuttingProjector, practicePuttingShotIntent, rotatePracticePuttingLine,
  practicePuttingWideViewBox } from "./practice-putting-geometry";

const start = { x: (515 - 640) / 35, y: (360 - 565) / 35 };
const cups = [
  { x: 335, y: 213 }, { x: 855, y: 175 }, { x: 694, y: 374 },
  { x: 1010, y: 495 }, { x: 314, y: 514 },
];

describe("practice putting projection", () => {
  it("keeps arbitrary nearby aim points distinct and rotates the line in half-degree steps", () => {
    const ball = { x: -3, y: 0 };
    const first = practicePuttingAimDirection(ball, { x: 0, y: 0.1 })!;
    const second = practicePuttingAimDirection(ball, { x: 0, y: 0.15 })!;
    expect(first.y).not.toBeCloseTo(second.y, 4);
    expect(practicePuttingAimDirection(ball, ball)).toBeNull();
    expect(PRACTICE_PUTTING_AIM_STEP_DEGREES).toBe(0.5);
    const adjusted = rotatePracticePuttingLine(first, PRACTICE_PUTTING_AIM_STEP_DEGREES);
    expect(Math.atan2(first.x * adjusted.y - first.y * adjusted.x,
      first.x * adjusted.x + first.y * adjusted.y) * 180 / Math.PI).toBeCloseTo(0.5, 8);
    expect(rotatePracticePuttingLine(adjusted, -PRACTICE_PUTTING_AIM_STEP_DEGREES).x).toBeCloseTo(first.x, 10);
  });

  it("submits the selected physical line and pace independently without snapping or rounding", () => {
    const direction = practicePuttingAimDirection({ x: -3, y: 0 }, { x: 0.2, y: -0.13 })!;
    const first = practicePuttingShotIntent(direction, 9.37, "4");
    const otherLine = practicePuttingShotIntent(rotatePracticePuttingLine(direction, 0.5), 9.37, "4");
    const otherPace = practicePuttingShotIntent(direction, 10.12, "4");
    expect(first).toEqual({ direction, intendedRollFeet: 9.37, expectedShotRevision: "4" });
    expect(otherLine.direction).not.toEqual(first.direction);
    expect(otherLine.intendedRollFeet).toBe(first.intendedRollFeet);
    expect(otherPace.direction).toEqual(first.direction);
    expect(otherPace.intendedRollFeet).toBe(10.12);
  });

  it.each([{ width: 1366, height: 540 }, { width: 390, height: 360 }])(
    "maps mouse and touch points through the $width px stage to one authoritative line", ({ width, height }) => {
      const ball = { x: -3, y: 0 };
      const target = { x: 0.24, y: -0.11 };
      const camera = width <= 700 ? practicePuttingFocusViewBox(ball, { x: 0, y: 0 }, width, height)
        : practicePuttingWideViewBox(width, height);
      const scene = practicePuttingEnvironmentViewBox(camera, width, height);
      const svg = practicePuttingProjector.project(target);
      const scale = width / scene.width;
      const client = { x: 47 + (svg.x - scene.x) * scale, y: 81 + (svg.y - scene.y) * scale };
      const screenToSvg = { a: 1 / scale, b: 0, c: 0, d: 1 / scale,
        e: scene.x - 47 / scale, f: scene.y - 81 / scale };
      for (const pointerType of ["mouse", "touch"]) {
        const world = pointerWorldTarget(client, screenToSvg, practicePuttingProjector.unproject);
        const line = practicePuttingAimDirection(ball, world)!;
        const expected = practicePuttingAimDirection(ball, target)!;
        expect(line.x, pointerType).toBeCloseTo(expected.x, 10);
        expect(line.y, pointerType).toBeCloseTo(expected.y, 10);
      }
    });

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
