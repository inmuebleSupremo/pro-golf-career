import { existsSync, readFileSync } from "node:fs";

import { describe, expect, it } from "vitest";

import { feedbackAimPoint, interpolatedContactPoint, isAimPointInteractionLocked, tracePlaybackDurationMs, tracePlaybackFrame, tracePlaybackStatus, transitionLabel, type ShotTrace } from "./trace-presentation";
import { buildBallStrikeIntent } from "./intent";
import { completeNonHolingPlayback, showResolvedShot } from "./playback-presentation";

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

// Resolver-authored values from the deterministic ShotFamilyResolutionTest fixtures.
const pitchRollTrace: ShotTrace = {
  ...rollingTrace,
  club: "GAP_WEDGE",
  contact: { position: { x: 1.251922899377285, y: 24.04285985538389 }, surface: "FAIRWAY" },
  roll: {
    from: { x: 1.251922899377285, y: 24.04285985538389 },
    to: { x: 1.251922899377285, y: 24.79285985538389 },
  },
  finalPoint: { x: 1.251922899377285, y: 24.79285985538389 },
};

const chipRollTrace: ShotTrace = {
  ...rollingTrace,
  club: "GAP_WEDGE",
  contact: { position: { x: 1.1603187847887033, y: 24.091943965364205 }, surface: "FAIRWAY" },
  roll: {
    from: { x: 1.1603187847887033, y: 24.091943965364205 },
    to: { x: 1.1603187847887033, y: 26.641943965364206 },
  },
  finalPoint: { x: 1.1603187847887033, y: 26.641943965364206 },
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

  it("sequences authoritative airborne, roll, and final phases without rendering the final point early", () => {
    expect(tracePlaybackFrame(rollingTrace, 0.4)).toMatchObject({ phase: "airborne", finalVisible: false });
    expect(tracePlaybackFrame(rollingTrace, 0.5)).toMatchObject({ phase: "contact", finalVisible: false });
    expect(tracePlaybackFrame(rollingTrace, 0.86)).toMatchObject({ phase: "roll", finalVisible: false });
    expect(tracePlaybackFrame(rollingTrace, 1)).toEqual({ phase: "final", point: rollingTrace.finalPoint, finalVisible: true });
  });

  it.each([
    ["0.75-yard PITCH", pitchRollTrace, 0.75],
    ["2.55-yard CHIP", chipRollTrace, 2.55],
  ])("keeps the authoritative %s release perceptible without changing either endpoint", (_name, trace, rollYards) => {
    expect(trace.roll?.from).toEqual(trace.contact.position);
    expect(Math.hypot((trace.roll?.to.x ?? 0) - (trace.roll?.from.x ?? 0), (trace.roll?.to.y ?? 0) - (trace.roll?.from.y ?? 0)))
      .toBeCloseTo(rollYards, 10);
    expect(tracePlaybackDurationMs(trace)).toBe(1050);
    expect(tracePlaybackStatus(trace, tracePlaybackFrame(trace, 0.2))).toBe("Airborne");
    expect(tracePlaybackFrame(trace, 0.5)).toMatchObject({ phase: "contact", point: trace.contact.position, finalVisible: false });
    expect(tracePlaybackStatus(trace, tracePlaybackFrame(trace, 0.5))).toBe("Contact");
    const rolling = tracePlaybackFrame(trace, 0.8);
    expect(rolling).toMatchObject({ phase: "roll", finalVisible: false });
    expect(tracePlaybackStatus(trace, rolling)).toBe("Rolling");
    expect(tracePlaybackFrame(trace, 1)).toEqual({ phase: "final", point: trace.finalPoint, finalVisible: true });
    expect(tracePlaybackStatus(trace, tracePlaybackFrame(trace, 1))).toBe("Final");
  });

  it("finishes a no-roll trace at contact and keeps recovery as an explicit authoritative transition", () => {
    const noRoll: ShotTrace = {
      ...rollingTrace,
      roll: null,
      transition: null,
      finalPoint: rollingTrace.contact.position,
    };
    expect(tracePlaybackFrame(noRoll, 0.99)).toMatchObject({ phase: "airborne", finalVisible: false });
    expect(tracePlaybackFrame(noRoll, 1)).toEqual({ phase: "final", point: noRoll.contact.position, finalVisible: true });
    expect(tracePlaybackDurationMs(noRoll)).toBe(650);
    expect(tracePlaybackStatus(noRoll, tracePlaybackFrame(noRoll, 0.99))).toBe("Airborne");
    expect(tracePlaybackStatus(noRoll, tracePlaybackFrame(noRoll, 1))).toBe("Final");
    expect(tracePlaybackFrame(trace, 0.86)).toMatchObject({ phase: "transition", finalVisible: false });
    expect(tracePlaybackFrame(trace, 1)).toEqual({ phase: "final", point: trace.finalPoint, finalVisible: true });
    expect(tracePlaybackStatus(trace, tracePlaybackFrame(trace, 0.86))).toBeNull();
  });

  it("unlocks canonical targeting after completed playback and builds the next literal intent", () => {
    const completed = completeNonHolingPlayback(showResolvedShot(rollingTrace, { label: "last shot" }));
    expect(completed).toEqual({ playbackShot: null, lastOutcome: { label: "last shot" } });
    expect(isAimPointInteractionLocked(true)).toBe(true);
    expect(isAimPointInteractionLocked(false)).toBe(false);
    expect(buildBallStrikeIntent("SEVEN_IRON", { x: 23, y: 141 }, "FULL", "DRAW", "next-revision")).toEqual({
      club: "SEVEN_IRON",
      aimPoint: { x: 23, y: 141 },
      shotFamily: "FULL",
      shotShape: "DRAW",
      expectedShotRevision: "next-revision",
    });
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
