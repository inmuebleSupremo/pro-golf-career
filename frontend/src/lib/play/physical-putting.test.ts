import { describe, expect, it } from "vitest";
import { PHYSICAL_BALL_RADIUS_YARDS, PHYSICAL_CUP_RADIUS_YARDS, PHYSICAL_PUTTING_SVG_STYLE, currentPhysicalPuttControls, cupDirectedLine, defaultPaceFeet, physicalAimGuideEndpoint, physicalAimGuideVisible, physicalBallLocatorRadius, physicalBallMarkerRadius, physicalCupBallScreenGap, physicalCupLocatorRadius, physicalPuttControlSource, physicalPuttPlaybackDurationMs, physicalPuttingProjector, physicalTrajectoryPoint, physicalTraversedTrajectory, physicalWorldMarkerRadius, validPaceFeet, visibleSlopeSamples } from "./physical-putting";

describe("physical putting presentation", () => {
  it("defaults to the cup-centre line and measured level-ground pace", () => {
    expect(cupDirectedLine({ x: 2, y: 4 }, { x: 2, y: 9 })).toEqual({ x: 0, y: 1 });
    expect(defaultPaceFeet({ x: 2, y: 4 }, { x: 2, y: 9 })).toBe(15);
  });

  it("keeps line and pace independent while rejecting only invalid pace input", () => {
    expect(cupDirectedLine({ x: 0, y: 0 }, { x: 3, y: 4 })).toEqual({ x: 0.6, y: 0.8 });
    expect(validPaceFeet(-1)).toBe(0.1);
    expect(validPaceFeet(140)).toBe(140);
    expect(validPaceFeet(12.5)).toBe(12.5);
  });

  it("resets a new physical putt to its measured cup distance without overwriting same-shot choices", () => {
    const cup = { x: 0, y: 0 };
    const firstBall = { x: -19.2666666667, y: 0 }; // 57.8 ft
    const secondBall = { x: -2.2, y: 0 }; // 6.6 ft
    const firstSource = physicalPuttControlSource("revision-1", firstBall, cup);
    const first = currentPhysicalPuttControls(firstSource, firstBall, cup, null);
    const adjusted = { ...first, paceFeet: 63.4, direction: { x: 0.98, y: 0.2 } };
    expect(currentPhysicalPuttControls(firstSource, firstBall, cup, adjusted)).toEqual(adjusted);

    const secondSource = physicalPuttControlSource("revision-2", secondBall, cup);
    const next = currentPhysicalPuttControls(secondSource, secondBall, cup, adjusted);
    expect(next.source).toBe(secondSource);
    expect(next.direction).toEqual({ x: 1, y: 0 });
    expect(next.paceFeet).toBeCloseTo(6.6, 8);
  });

  it("uses the renderer's physical playback duration rather than a competing timeout", () => {
    expect(physicalPuttPlaybackDurationMs([{ timeSeconds: 0, position: { x: 0, y: 0 } }, { timeSeconds: 2, position: { x: 1, y: 0 } }])).toBe(900);
    expect(physicalPuttPlaybackDurationMs([])).toBe(0);
  });

  it("keeps a visible guide exactly on the submitted line and preserves the real ball-to-cup ratio", () => {
    expect(physicalAimGuideEndpoint({ x: 3, y: 4 }, { x: 0, y: 1 }, 2)).toEqual({ x: 3, y: 6 });
    expect(physicalAimGuideEndpoint({ x: 3, y: 4 }, { x: 1, y: 0 }, 18)).toEqual({ x: 21, y: 4 });
    expect(physicalBallMarkerRadius(8)).toBeCloseTo(8 * 1.68 / 4.25, 8);
    expect(PHYSICAL_PUTTING_SVG_STYLE).toMatchObject({
      greenFill: "var(--primary)", guideStroke: "var(--foreground)", canvasFill: "var(--surface-elevated)",
    });
    expect(Object.values(PHYSICAL_PUTTING_SVG_STYLE)).not.toContain("var(--accent)");
  });

  it("projects the 0.4-foot near miss and physical cup/ball radii through one world-space scale", () => {
    const centreDistanceYards = 0.4 / 3;
    expect(PHYSICAL_CUP_RADIUS_YARDS).toBeCloseTo(2.125 / 36, 10);
    expect(PHYSICAL_BALL_RADIUS_YARDS).toBeCloseTo(0.84 / 36, 10);
    expect(physicalCupBallScreenGap(centreDistanceYards, 50)).toBeGreaterThan(0);
    expect(physicalCupBallScreenGap(centreDistanceYards, 150)).toBeGreaterThan(0);
    expect(physicalWorldMarkerRadius(PHYSICAL_CUP_RADIUS_YARDS, 150)).toBeCloseTo(8.8541667, 5);
  });

  it("keeps a 0.5-foot centre-to-centre miss visibly outside the real cup without locator-ring overlap", () => {
    const closeUpScale = 150;
    expect(physicalCupBallScreenGap(0.5 / 3, closeUpScale)).toBeCloseTo(12.6458333, 5);
    expect(physicalBallLocatorRadius(physicalWorldMarkerRadius(PHYSICAL_BALL_RADIUS_YARDS, closeUpScale))).toBeNull();
    expect(physicalCupLocatorRadius(physicalWorldMarkerRadius(PHYSICAL_CUP_RADIUS_YARDS, closeUpScale))).toBeNull();
  });

  it("hides the next-shot guide until an authoritative physical trajectory is cleared", () => {
    expect(physicalAimGuideVisible(true)).toBe(false);
    expect(physicalAimGuideVisible(false)).toBe(true);
  });

  it("plays only the authoritative chronological trajectory", () => {
    const samples = [{ timeSeconds: 0, position: { x: 0, y: 0 } }, { timeSeconds: 1, position: { x: 2, y: 1 } }, { timeSeconds: 2, position: { x: 3, y: 4 } }];
    expect(physicalTrajectoryPoint(samples, 0.25)).toEqual({ x: 1, y: 0.5 });
    expect(physicalTrajectoryPoint(samples, 1)).toEqual({ x: 3, y: 4 });
  });

  it("keeps ball, cup and a finite direct guide usable across the fixed distance matrix", () => {
    const green = [{ x: -40, y: -25 }, { x: 40, y: -25 }, { x: 40, y: 25 }, { x: -40, y: 25 }];
    for (const feet of [0.5, 2, 10, 30, 60, 92]) {
      const ball = { x: 0, y: 0 };
      const cup = { x: feet / 3, y: 0 };
      const projector = physicalPuttingProjector(ball, cup, green);
      const ballScreen = projector.project(ball);
      const cupScreen = projector.project(cup);
      expect(ballScreen.x).toBeGreaterThan(0);
      expect(cupScreen.x).toBeLessThan(projector.width);
      expect(projector.pixelsPerYard).toBeGreaterThan(0);
      expect(physicalAimGuideEndpoint(ball, { x: 1, y: 0 }, feet / 3)).toEqual(cup);
    }
    const longView = physicalPuttingProjector({ x: 0, y: 0 }, { x: 20, y: 0 }, green);
    expect(physicalBallLocatorRadius(physicalWorldMarkerRadius(PHYSICAL_BALL_RADIUS_YARDS, longView.pixelsPerYard))).not.toBeNull();
    expect(physicalCupLocatorRadius(physicalWorldMarkerRadius(PHYSICAL_CUP_RADIUS_YARDS, longView.pixelsPerYard))).not.toBeNull();
  });

  it("conceals unresolved trajectory points and keeps authoritative reading samples usable in the active view", () => {
    const trajectory = [{ timeSeconds: 0, position: { x: 0, y: 0 } }, { timeSeconds: 1, position: { x: 3, y: 0 } }, { timeSeconds: 2, position: { x: 8, y: 2 } }];
    expect(physicalTraversedTrajectory(trajectory, .25).at(-1)?.position).toEqual({ x: 1.5, y: 0 });
    expect(physicalTraversedTrajectory(trajectory, .25)).not.toContainEqual(trajectory.at(-1));
    expect(physicalTraversedTrajectory(trajectory, 1)).toEqual(trajectory);

    const projector = physicalPuttingProjector({ x: 0, y: 0 }, { x: 10 / 3, y: 0 }, [{ x: -20, y: -20 }, { x: 20, y: -20 }, { x: 20, y: 20 }, { x: -20, y: 20 }]);
    const samples = [
      { position: { x: .2, y: .1 }, slope: .01 },
      { position: { x: .2, y: .1 }, slope: .03 },
      { position: { x: 100, y: 100 }, slope: .9 },
    ];
    expect(visibleSlopeSamples(samples, projector)).toEqual([samples[1]]);
  });
});
