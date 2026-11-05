import { afterEach, describe, expect, it, vi } from "vitest";

import { scheduleTrainingResultTransition, TRAINING_RESULT_DISPLAY_MS } from "./use-training-result-transition";

afterEach(() => vi.useRealTimers());

describe("training result transition", () => {
  it("waits for playback completion and a readable result interval", () => {
    vi.useFakeTimers();
    const advance = vi.fn();
    scheduleTrainingResultTransition(3, true, advance);
    vi.advanceTimersByTime(TRAINING_RESULT_DISPLAY_MS * 2);
    expect(advance).not.toHaveBeenCalled();
    scheduleTrainingResultTransition(3, false, advance);
    vi.advanceTimersByTime(TRAINING_RESULT_DISPLAY_MS - 1);
    expect(advance).not.toHaveBeenCalled();
    vi.advanceTimersByTime(1);
    expect(advance).toHaveBeenCalledOnce();
  });

  it("cancels pending progression on cleanup and also schedules the eighth result", () => {
    vi.useFakeTimers();
    const advance = vi.fn();
    const cancel = scheduleTrainingResultTransition(7, false, advance);
    cancel();
    vi.advanceTimersByTime(TRAINING_RESULT_DISPLAY_MS);
    expect(advance).not.toHaveBeenCalled();
    scheduleTrainingResultTransition(8, false, advance);
    vi.advanceTimersByTime(TRAINING_RESULT_DISPLAY_MS);
    expect(advance).toHaveBeenCalledOnce();
  });
});
