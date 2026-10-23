import { describe, expect, it } from "vitest";
import { createElement } from "react";
import { renderToStaticMarkup } from "react-dom/server";

import { formatRemainingDistance, outcomeFeedback, OutcomeNote, type Outcome } from "./play-shared";

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
    expect(formatRemainingDistance(50.33)).toBe("50 yd");
  });
});

describe("completed shot presentation", () => {
  it("keeps carry, final lie and remaining distance in separate labelled groups", () => {
    const html = renderToStaticMarkup(createElement(OutcomeNote, { outcome: outcome({
      carry: 281.3, finalSurface: "PRIMARY_ROUGH", distanceRemaining: 50.33,
    }) }));
    expect(html).toContain("Previous carry");
    expect(html).toContain("281 yd");
    expect(html).toContain("Primary rough");
    expect(html).toContain("Remaining to pin");
    expect(html).toContain("50 yd");
  });

  it("marks a hole-out from server state and labels physical displacement accurately", () => {
    const html = renderToStaticMarkup(createElement(OutcomeNote, { outcome: outcome({
      carry: 0.4, holed: true, physicalPutt: {
        trajectory: [], finalPosition: { x: 0, y: 0 }, holed: true, terminalCondition: "CAPTURED",
      },
    }) }));
    expect(html).toContain("Ball moved");
    expect(html).toContain("1.2 ft");
    expect(html).toContain("Holed");
  });
});
