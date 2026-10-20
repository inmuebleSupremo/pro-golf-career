import { describe, expect, it } from "vitest";

import { formatRemainingDistance, outcomeFeedback, type Outcome } from "./play-shared";

function outcome(overrides: Partial<Outcome> = {}): Outcome {
  return {
    finalSurface: "GREEN",
    carry: 4,
    lateral: 0,
    distanceRemaining: 0,
    holed: false,
    hazardEntered: false,
    penaltyStrokes: 0,
    strokes: 1,
    ...overrides,
  };
}

describe("putting feedback", () => {
  it("uses the backend holed result rather than a client distance rule", () => {
    expect(outcomeFeedback(outcome({ distanceRemaining: 0.35, holed: true })))
      .toEqual({ holed: true, remainingDistance: null });
  });

  it("shows a close missed putt in feet instead of rounding it to zero yards", () => {
    expect(outcomeFeedback(outcome({ distanceRemaining: 0.4 })))
      .toEqual({ holed: false, remainingDistance: "1.2 ft" });
  });

  it("uses useful precision at short-distance display boundaries", () => {
    expect(formatRemainingDistance(0.01)).toBe("<0.1 ft");
    expect(formatRemainingDistance(0.4)).toBe("1.2 ft");
    expect(formatRemainingDistance(10 / 3)).toBe("10 ft");
  });
});
