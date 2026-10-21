import { describe, expect, it } from "vitest";

import { physicalPuttingEligible, physicalTransitionObservationMs, resolvedPlaybackDurationMs } from "./play-sequence-timing";

const physicalHole = {
  puttingPolicy: "PHYSICAL_V1",
  physicalGreen: { slopeSamples: [] },
} as never;

const greenSituation = { lie: "GREEN" } as never;

describe("physical putting transition timing", () => {
  it("permits the putting view only for an eligible physical green, never an off-green result", () => {
    expect(physicalPuttingEligible(physicalHole, greenSituation)).toBe(true);
    expect(physicalPuttingEligible(physicalHole, { lie: "FAIRWAY" } as never)).toBe(false);
    expect(physicalPuttingEligible({ puttingPolicy: "LEGACY", physicalGreen: { slopeSamples: [] } } as never, greenSituation)).toBe(false);
  });

  it("waits for the trace renderer before beginning the physical-green observation beat", () => {
    // 1050 ms is the canonical renderer duration for a trace with ground roll.
    expect(resolvedPlaybackDurationMs(1050, 1600, false)).toBe(1050);
    expect(physicalTransitionObservationMs(false)).toBe(1700);
    expect(resolvedPlaybackDurationMs(1050, 1600, true)).toBe(420);
    expect(physicalTransitionObservationMs(true)).toBe(250);
  });
});
