import { existsSync, readFileSync } from "node:fs";

import { describe, expect, it } from "vitest";

import { authoritativeGroundPath, feedbackAimPoint, interpolatedContactPoint, isAimPointInteractionLocked, traceBallProjection, traceBounceCue, traceHeightCue, tracePlaybackDurationMs, tracePlaybackFrame, tracePlaybackStatus, tracePlaybackTiming, transitionLabel, traversedAirbornePoints, type ShotTrace } from "./trace-presentation";
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

const bouncingTrace: ShotTrace = {
  ...rollingTrace,
  origin: { x: 0, y: 0 },
  contact: { position: { x: 0, y: 100 }, surface: "FAIRWAY" },
  roll: { from: { x: 0, y: 100 }, to: { x: 0, y: 108 } },
  groundPath: [
    { kind: "BOUNCE_APEX", position: { x: 0, y: 103 }, heightYards: .45 },
    { kind: "BOUNCE_LANDING", position: { x: 0, y: 106 }, heightYards: 0 },
    { kind: "ROLL_STOP", position: { x: 0, y: 108 }, heightYards: 0 },
  ],
  finalPoint: { x: 0, y: 108 },
};

const runningChipTrace: ShotTrace = {
  ...bouncingTrace,
  club: "EIGHT_IRON",
  contact: { position: { x: 0, y: 12 }, surface: "FAIRWAY" },
  airbornePath: [
    { progress: 0, position: { x: 0, y: 0 }, height: 0 },
    { progress: .5, position: { x: 0, y: 6 }, height: 1 },
    { progress: 1, position: { x: 0, y: 12 }, height: 0 },
  ],
  backspinRpm: 1400,
  roll: { from: { x: 0, y: 12 }, to: { x: 0, y: 22 } },
  groundPath: [{ kind: "ROLL_STOP", position: { x: 0, y: 22 }, heightYards: 0 }],
  finalPoint: { x: 0, y: 22 },
};

describe("trace presentation", () => {
  it("reveals only the airborne samples behind a curved draw or fade", () => {
    const curved: ShotTrace = { ...rollingTrace,
      origin: { x: 0, y: 0 }, contact: { position: { x: 0, y: 100 }, surface: "FAIRWAY" },
      airbornePath: [
        { progress: 0, position: { x: 0, y: 0 }, height: 0 },
        { progress: .25, position: { x: 5, y: 25 }, height: 10 },
        { progress: .5, position: { x: 8, y: 50 }, height: 15 },
        { progress: .75, position: { x: 5, y: 75 }, height: 10 },
        { progress: 1, position: { x: 0, y: 100 }, height: 0 },
      ],
      roll: null, finalPoint: { x: 0, y: 100 },
    };
    const timing = tracePlaybackTiming(curved);
    const progress = timing.flightMs * .375 / timing.totalMs;
    expect(traversedAirbornePoints(curved, 0)).toEqual([]);
    expect(traversedAirbornePoints(curved, progress)).toEqual([
      curved.origin, { x: 5, y: 25 }, { x: 6.5, y: 37.5 },
    ]);
    expect(traversedAirbornePoints(curved, progress).at(-1)).toEqual(tracePlaybackFrame(curved, progress).point);
    expect(traversedAirbornePoints(curved, timing.flightMs / timing.totalMs).at(-1)).toEqual(curved.contact.position);
    expect(traversedAirbornePoints(curved, 1)).toEqual(traversedAirbornePoints(curved, timing.flightMs / timing.totalMs));
  });
  it("keeps a running chip from accelerating horizontally at first contact", () => {
    const timing = tracePlaybackTiming(runningChipTrace);
    const frameAt = (ms: number) => tracePlaybackFrame(runningChipTrace, ms / timing.totalMs);
    const before = frameAt(timing.flightMs - 16);
    const contact = frameAt(timing.flightMs);
    const after = frameAt(timing.flightMs + 16);
    const later = frameAt(timing.flightMs + 32);
    expect(timing.contactMs).toBe(0);
    expect(contact).toMatchObject({ phase: "roll", point: runningChipTrace.contact.position, height: 0 });
    expect(after.point.y - contact.point.y).toBeGreaterThan(0);
    expect(after.point.y - contact.point.y).toBeLessThanOrEqual(contact.point.y - before.point.y);
    expect(later.point.y - after.point.y).toBeLessThan(after.point.y - contact.point.y);
    expect(frameAt(timing.totalMs)).toEqual({ phase: "final", point: runningChipTrace.finalPoint, finalVisible: true });
    expect(tracePlaybackTiming(bouncingTrace).contactMs).toBeGreaterThan(0);
    expect(tracePlaybackTiming({ ...runningChipTrace, club: "DRIVER" }).contactMs).toBeGreaterThan(0);
    expect(tracePlaybackTiming({ ...runningChipTrace, club: "GAP_WEDGE", backspinRpm: 9000 }).contactMs).toBeGreaterThan(0);
  });
  it("plays only authored bounce events and their heights before easing the remaining ground roll", () => {
    const timing = tracePlaybackTiming(bouncingTrace);
    const halfHopMs = Math.round(1000 * Math.sqrt(2 * .45 * .9144 / 9.81));
    const groundStart = timing.flightMs + timing.contactMs;
    const frameAt = (ms: number) => tracePlaybackFrame(bouncingTrace, ms / timing.totalMs);
    expect(authoritativeGroundPath(bouncingTrace)).toHaveLength(3);
    expect(frameAt(timing.flightMs)).toMatchObject({ phase: "contact", point: bouncingTrace.contact.position });
    expect(frameAt(groundStart + halfHopMs)).toMatchObject({ phase: "bounce", point: { x: 0, y: 103 }, height: .45 });
    expect(frameAt(groundStart + halfHopMs * 2)).toMatchObject({ phase: "roll", point: { x: 0, y: 106 }, height: 0 });
    const rising = frameAt(groundStart + halfHopMs / 2);
    expect(rising.point.y).toBeGreaterThan(100);
    expect(rising.point.y).toBeLessThan(103);
    expect(rising.height).toBeGreaterThan(0);
    expect(rising.height).toBeLessThan(.45);
    expect(tracePlaybackStatus(bouncingTrace, rising)).toBe("Bouncing");
    const projected = traceBallProjection(rising, (point) => ({ x: point.x, y: 300 - point.y }));
    expect(projected.ball.y).toBeLessThan(projected.ground.y);
    expect(traceBounceCue(.45).lift).toBeGreaterThan(traceHeightCue(.45).lift);
    const rollStart = groundStart + halfHopMs * 2;
    const rollDuration = timing.totalMs - rollStart;
    const early = frameAt(rollStart + rollDuration * .25).point.y;
    const late = frameAt(rollStart + rollDuration * .75).point.y;
    expect(early - 106).toBeGreaterThan(108 - late);
    expect(frameAt(timing.totalMs)).toEqual({ phase: "final", point: bouncingTrace.finalPoint, finalVisible: true });
  });

  it("keeps legacy roll playback when ground events are absent or incomplete", () => {
    expect(authoritativeGroundPath(rollingTrace)).toBeNull();
    const incomplete = { ...bouncingTrace, groundPath: [bouncingTrace.groundPath![0]] } satisfies ShotTrace;
    expect(authoritativeGroundPath(incomplete)).toBeNull();
    const timing = tracePlaybackTiming(incomplete);
    const frame = tracePlaybackFrame(incomplete, (timing.flightMs + timing.contactMs + timing.rollMs / 2) / timing.totalMs);
    expect(frame.phase).toBe("roll");
    expect(frame.height).toBeUndefined();
    const boundary = { ...bouncingTrace, groundPath: [
      { kind: "BOUNDARY_STOP", position: bouncingTrace.finalPoint, heightYards: 0 },
    ] } satisfies ShotTrace;
    expect(tracePlaybackFrame(boundary, .95)).toMatchObject({ phase: "roll", height: 0 });
    expect(authoritativeGroundPath(boundary)).toHaveLength(1);
  });
  it("follows resolver-authored curved roll samples before showing the final ball", () => {
    const curved = { ...bouncingTrace, club: "GAP_WEDGE", backspinRpm: 1400,
      finalPoint: { x: 2, y: 108 },
      roll: { from: bouncingTrace.contact.position, to: { x: 2, y: 108 } },
      groundPath: [
        { kind: "ROLL_SAMPLE", position: { x: 0.5, y: 104 }, heightYards: 0 },
        { kind: "ROLL_SAMPLE", position: { x: 1.5, y: 106 }, heightYards: 0 },
        { kind: "ROLL_STOP", position: { x: 2, y: 108 }, heightYards: 0 },
      ] } satisfies ShotTrace;
    expect(authoritativeGroundPath(curved)).toHaveLength(3);
    const timing = tracePlaybackTiming(curved);
    const rollStart = timing.flightMs + timing.contactMs;
    const halfway = tracePlaybackFrame(curved, (rollStart + timing.rollMs / 2) / timing.totalMs);
    expect(halfway.phase).toBe("roll");
    expect(halfway.point.x).toBeGreaterThan(0);
    expect(halfway.point.x).toBeLessThan(2);
    expect(halfway.finalVisible).toBe(false);
    expect(tracePlaybackFrame(curved, 1)).toEqual({ phase: "final", point: curved.finalPoint, finalVisible: true });
  });
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
    const timing = tracePlaybackTiming(rollingTrace);
    expect(tracePlaybackFrame(rollingTrace, timing.flightMs / timing.totalMs)).toMatchObject({ phase: "contact", finalVisible: false });
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
    expect(tracePlaybackDurationMs(trace)).toBeGreaterThan(1000);
    expect(tracePlaybackStatus(trace, tracePlaybackFrame(trace, 0.2))).toBe("Airborne");
    const timing = tracePlaybackTiming(trace);
    const contact = tracePlaybackFrame(trace, timing.flightMs / timing.totalMs);
    expect(contact).toMatchObject({ phase: "contact", point: trace.contact.position, finalVisible: false });
    expect(tracePlaybackStatus(trace, contact)).toBe("Contact");
    const rolling = tracePlaybackFrame(trace, (timing.flightMs + timing.contactMs + timing.rollMs / 2) / timing.totalMs);
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
    expect(tracePlaybackFrame(noRoll, 0.99)).toMatchObject({ phase: "contact", finalVisible: false });
    expect(tracePlaybackFrame(noRoll, 1)).toEqual({ phase: "final", point: noRoll.contact.position, finalVisible: true });
    expect(tracePlaybackDurationMs(noRoll)).toBeGreaterThan(650);
    expect(tracePlaybackStatus(noRoll, tracePlaybackFrame(noRoll, 0.99))).toBe("Contact");
    expect(tracePlaybackStatus(noRoll, tracePlaybackFrame(noRoll, 1))).toBe("Final");
    expect(tracePlaybackFrame(trace, 0.86)).toMatchObject({ phase: "transition", finalVisible: false });
    expect(tracePlaybackFrame(trace, 1)).toEqual({ phase: "final", point: trace.finalPoint, finalVisible: true });
    expect(tracePlaybackStatus(trace, tracePlaybackFrame(trace, 0.86))).toBeNull();
  });

  it("generalizes the approved tempos from carry and release, without club-specific duration presets", () => {
    const sample = (club: string, carry: number, release: number): ShotTrace => ({
      club, origin: { x: 0, y: 0 }, aimPoint: { x: 0, y: carry },
      contact: { position: { x: 0, y: carry }, surface: "GREEN" },
      airbornePath: [
        { progress: 0, position: { x: 0, y: 0 }, height: 0 },
        { progress: .5, position: { x: 0, y: carry / 2 }, height: carry * .12 },
        { progress: 1, position: { x: 0, y: carry }, height: 0 },
      ],
      roll: release ? { from: { x: 0, y: carry }, to: { x: 0, y: carry + release } } : null,
      transition: null, finalPoint: { x: 0, y: carry + release },
    });
    const iron = sample("SEVEN_IRON", 155, 2.94);
    const driver = sample("DRIVER", 255, 22);
    const wedge = sample("PITCHING_WEDGE", 105, .6);
    const pitch = sample("GAP_WEDGE", 42, .75);
    const chip = sample("EIGHT_IRON", 16, 2.55);
    for (const [shot, target] of [[iron, 1990], [driver, 2702], [pitch, 1346], [chip, 843]] as const) {
      const timing = tracePlaybackTiming(shot);
      expect(timing.totalMs).toBeGreaterThan(target - 100);
      expect(timing.totalMs).toBeLessThan(target + 100);
      expect(tracePlaybackFrame(shot, 0).point).toEqual(shot.origin);
      expect(tracePlaybackFrame(shot, timing.flightMs / timing.totalMs).point).toEqual(shot.contact.position);
      expect(tracePlaybackFrame(shot, 1).point).toEqual(shot.finalPoint);
    }
    expect(tracePlaybackTiming(driver).rollMs).toBeGreaterThan(tracePlaybackTiming(iron).rollMs);
    expect(tracePlaybackTiming(wedge).rollMs).toBeLessThan(tracePlaybackTiming(iron).rollMs);
    expect(tracePlaybackTiming(wedge).flightMs).toBeLessThan(tracePlaybackTiming(iron).flightMs);
    expect(tracePlaybackTiming(chip).flightMs).toBeLessThan(tracePlaybackTiming(pitch).flightMs);
    const apex = tracePlaybackFrame(iron, tracePlaybackTiming(iron).flightMs / tracePlaybackTiming(iron).totalMs / 2);
    expect(apex.point).toEqual({ x: 0, y: 77.5 });
    expect(apex.height).toBeCloseTo(18.6);
    expect(traceHeightCue(apex.height!).lift).toBeGreaterThan(0);
    expect(traceHeightCue(0)).toMatchObject({ lift: 0, scale: 1 });
    const ironTiming = tracePlaybackTiming(iron);
    const project = ({ x, y }: { x: number; y: number }) => ({ x: x * 2 + 10, y: 400 - y * 2 });
    const airborne = traceBallProjection(apex, project);
    expect(airborne.ground).toEqual(project(apex.point));
    expect(airborne.ball.x).toBe(airborne.ground.x);
    expect(airborne.ball.y).toBeLessThan(airborne.ground.y);
    const landed = traceBallProjection(tracePlaybackFrame(iron, ironTiming.flightMs / ironTiming.totalMs), project);
    expect(landed.ball).toEqual(landed.ground);
    const rollStart = ironTiming.flightMs + ironTiming.contactMs;
    const early = tracePlaybackFrame(iron, (rollStart + ironTiming.rollMs * .25) / ironTiming.totalMs).point.y;
    const late = tracePlaybackFrame(iron, (rollStart + ironTiming.rollMs * .75) / ironTiming.totalMs).point.y;
    expect(early - iron.contact.position.y).toBeGreaterThan(iron.finalPoint.y - late);
  });

  it.each([
    ["driver", 255, 31], ["iron", 155, 19], ["pitch", 42, 12], ["chip", 16, 4],
  ])("moves continuously through the first authoritative %s flight samples", (_name, carry, apex) => {
    const shot: ShotTrace = {
      club: "SEVEN_IRON", origin: { x: 0, y: 0 }, aimPoint: { x: 0, y: carry },
      contact: { position: { x: 0, y: carry }, surface: "GREEN" },
      airbornePath: Array.from({ length: 9 }, (_, index) => {
        const progress = index / 8;
        return { progress, position: { x: 0, y: carry * progress },
          height: apex * 4 * progress * (1 - progress) };
      }),
      roll: null, transition: null, finalPoint: { x: 0, y: carry },
    };
    const total = tracePlaybackDurationMs(shot);
    const frames = [0, 16, 32, 48].map((elapsed) => tracePlaybackFrame(shot, elapsed / total));
    expect(frames[0]).toMatchObject({ phase: "airborne", point: shot.origin, height: 0, finalVisible: false });
    const steps = frames.slice(1).map((frame, index) => frame.point.y - frames[index].point.y);
    expect(steps[0]).toBeGreaterThan(0);
    expect(steps[1]).toBeCloseTo(steps[0]);
    expect(steps[2]).toBeCloseTo(steps[0]);
    const lifts = frames.map((frame) => traceHeightCue(frame.height ?? 0).lift);
    expect(lifts[1]).toBeGreaterThan(lifts[0]);
    expect(lifts[2] - lifts[1]).toBeLessThan(lifts[1] - lifts[0]);
  });

  it("bounds very short, very long, no-roll and recovery playback without changing endpoints", () => {
    const short = { ...rollingTrace, contact: { position: { x: 0, y: 0 }, surface: "BUNKER" },
      roll: null, transition: null, finalPoint: { x: 0, y: 0 } } satisfies ShotTrace;
    const bunkerEscape = { ...rollingTrace, club: "SAND_WEDGE", origin: { x: 0, y: 0 },
      contact: { position: { x: 0, y: 18 }, surface: "GREEN" }, roll: null,
      transition: null, finalPoint: { x: 0, y: 18 } } satisfies ShotTrace;
    const long = { ...rollingTrace, contact: { position: { x: 0, y: 10000 }, surface: "FAIRWAY" },
      roll: { from: { x: 0, y: 10000 }, to: { x: 0, y: 20000 } }, finalPoint: { x: 0, y: 20000 } } satisfies ShotTrace;
    expect(tracePlaybackTiming(short).totalMs).toBeGreaterThan(400);
    expect(tracePlaybackTiming(long).totalMs).toBeLessThanOrEqual(3140);
    expect(tracePlaybackFrame(short, 1).point).toEqual(short.finalPoint);
    expect(tracePlaybackFrame(bunkerEscape, 1).point).toEqual(bunkerEscape.finalPoint);
    expect(tracePlaybackTiming(bunkerEscape).rollMs).toBe(0);
    expect(tracePlaybackFrame(long, 1).point).toEqual(long.finalPoint);
    expect(tracePlaybackFrame(trace, 1).point).toEqual(trace.finalPoint);
    expect(tracePlaybackFrame(rollingTrace, 0.25).height).toBe(0);
    const sparse = { ...rollingTrace, airbornePath: [
      { progress: .5, position: { x: 2, y: 70 }, height: 12 },
    ] } satisfies ShotTrace;
    expect(tracePlaybackFrame(sparse, 0).point).toEqual(sparse.origin);
    const timing = tracePlaybackTiming(sparse);
    expect(tracePlaybackFrame(sparse, timing.flightMs / timing.totalMs).point).toEqual(sparse.contact.position);
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
    const motionOverlay = readFileSync(
      new URL("../../components/play/shot-motion-overlay.tsx", import.meta.url),
      "utf8",
    );

    expect(compatibilityEntry).toContain("CanonicalHole2d");
    expect(compatibilityEntry).not.toContain("shot-router");
    expect(canonicalRenderer).not.toContain("shot-router");
    expect(canonicalRenderer).not.toContain("shot-flight");
    expect(canonicalRenderer).toContain("ShotMotionOverlay");
    expect(motionOverlay).toContain("trace.roll");
  });
});
