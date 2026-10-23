import { describe, expect, it } from "vitest";

import { acceptCourseAim, aimWithinEnvelope, buildBallStrikeIntent, currentAimPoint } from "./intent";

const envelope = { minX: -20, maxX: 60, minY: 0, maxY: 400 };
const initial = { x: 0, y: 260 };
const manual = { x: 28, y: 218 };

describe("player target", () => {
  it("keeps a manual target through club, technique and shape changes in the same shot", () => {
    const selection = { revision: "1:1", point: manual };
    const target = currentAimPoint(selection, "1:1", initial);
    expect(target).toBe(manual);
    expect(buildBallStrikeIntent("THREE_WOOD", target, "FULL", "DRAW", "1:1"))
      .toMatchObject({ aimPoint: manual, club: "THREE_WOOD", shotFamily: "FULL", shotShape: "DRAW" });
  });

  it("starts a new server shot from its new initial target", () => {
    expect(currentAimPoint({ revision: "1:1", point: manual }, "1:2", { x: 2, y: 80 }))
      .toEqual({ x: 2, y: 80 });
  });

  it("accepts free aiming inside the server envelope, including its edges", () => {
    expect(aimWithinEnvelope(manual, envelope)).toBe(true);
    expect(aimWithinEnvelope({ x: -20, y: 400 }, envelope)).toBe(true);
    expect(aimWithinEnvelope({ x: 61, y: 200 }, envelope)).toBe(false);
    expect(aimWithinEnvelope({ x: Number.NaN, y: 200 }, envelope)).toBe(false);
  });

  it("dismisses the hint only after a new legal course aim and keeps it dismissed across shots", () => {
    const start = { selection: { revision: "1:1", point: initial }, usedCourseAim: false };
    expect(acceptCourseAim(start, "1:1", initial, { x: 61, y: 200 }, envelope)).toBe(start);
    expect(acceptCourseAim(start, "1:1", initial, initial, envelope)).toBe(start);

    const aimed = acceptCourseAim(start, "1:1", initial, manual, envelope);
    expect(aimed).toEqual({ selection: { revision: "1:1", point: manual }, usedCourseAim: true });
    expect(currentAimPoint(aimed.selection, "1:2", { x: 2, y: 80 })).toEqual({ x: 2, y: 80 });
    expect(acceptCourseAim(aimed, "1:2", { x: 2, y: 80 }, { x: 61, y: 200 }, envelope).usedCourseAim).toBe(true);
  });
});
