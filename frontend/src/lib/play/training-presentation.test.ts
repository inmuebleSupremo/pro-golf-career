import { describe, expect, it } from "vitest";

import { trainingRestingBalls, visibleTrainingAttempt } from "./training-presentation";

describe("training shot presentation", () => {
  const previous = { number: 1, finalPosition: { x: 4, y: 110 }, score: 0.5 };
  const current = { number: 2, finalPosition: { x: 12, y: 190 }, score: 0.8 };

  it("hides the accepted score and final marker until playback finishes", () => {
    expect(visibleTrainingAttempt(current, true)).toBeNull();
    expect(trainingRestingBalls([previous, current], current, 2, true))
      .toEqual([{ number: 1, finalPosition: previous.finalPosition }]);
    expect(visibleTrainingAttempt(current, false)).toBe(current);
    expect(trainingRestingBalls([previous, current], current, 2, false))
      .toEqual([{ number: 1, finalPosition: previous.finalPosition },
        { number: 2, finalPosition: current.finalPosition }]);
  });

  it("shows a retried or reconnected attempt without a shot trace exactly once", () => {
    expect(trainingRestingBalls([previous], current, 2, false))
      .toEqual([{ number: 1, finalPosition: previous.finalPosition },
        { number: 2, finalPosition: current.finalPosition }]);
    expect(trainingRestingBalls([previous, current], current, 2, false)).toHaveLength(2);
  });
});
