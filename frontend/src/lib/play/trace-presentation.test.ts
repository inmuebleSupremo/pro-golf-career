import { existsSync, readFileSync } from "node:fs";

import { describe, expect, it } from "vitest";

import { feedbackAimPoint, interpolatedContactPoint, transitionLabel, type ShotTrace } from "./trace-presentation";

const trace: ShotTrace = {
  club: "SEVEN_IRON",
  origin: { x: 0, y: 0 },
  aimPoint: { x: 5, y: 150 },
  contact: { position: { x: 8, y: 142 }, surface: "WATER" },
  transition: { kind: "WATER_DROP", from: { x: 8, y: 142 }, to: { x: 4, y: 126 } },
  finalPoint: { x: 4, y: 126 },
};

const rollingTrace: ShotTrace = {
  ...trace,
  contact: { position: { x: 6, y: 142 }, surface: "FAIRWAY" },
  roll: { from: { x: 6, y: 142 }, to: { x: 6, y: 146 } },
  transition: null,
  finalPoint: { x: 6, y: 146 },
};

describe("trace presentation", () => {
  it("keeps the completed trace target over a newly selected planning target", () => {
    expect(feedbackAimPoint(trace, { x: 0, y: 200 })).toEqual(trace.aimPoint);
    expect(feedbackAimPoint(null, { x: 0, y: 200 })).toEqual({ x: 0, y: 200 });
  });

  it("interpolates only between trace origin and contact", () => {
    expect(interpolatedContactPoint(trace, 0.5)).toEqual({ x: 4, y: 71 });
  });

  it("labels recovery as a rule transition rather than a flight profile", () => {
    expect(transitionLabel(trace)).toBe("water drop");
  });

  it("keeps an authoritative roll distinct from recovery and client flight interpolation", () => {
    expect(rollingTrace.roll?.from).toEqual(rollingTrace.contact.position);
    expect(rollingTrace.roll?.to).toEqual(rollingTrace.finalPoint);
    expect(transitionLabel(rollingTrace)).toBeNull();
    expect(interpolatedContactPoint(rollingTrace, 1)).toEqual(rollingTrace.contact.position);
  });

  it("prevents retired synthetic-flight modules from returning to canonical play", () => {
    expect(existsSync(new URL("./shot-flight.ts", import.meta.url))).toBe(false);
    expect(existsSync(new URL("./shot-router.ts", import.meta.url))).toBe(false);

    const compatibilityEntry = readFileSync(
      new URL("../../components/play/hole-2d.tsx", import.meta.url),
      "utf8",
    );
    const canonicalRenderer = readFileSync(
      new URL("../../components/play/canonical-hole-2d.tsx", import.meta.url),
      "utf8",
    );

    expect(compatibilityEntry).toContain("CanonicalHole2d");
    expect(compatibilityEntry).not.toContain("shot-router");
    expect(canonicalRenderer).not.toContain("shot-router");
    expect(canonicalRenderer).not.toContain("shot-flight");
    expect(canonicalRenderer).toContain("trace?.roll");
  });
});
