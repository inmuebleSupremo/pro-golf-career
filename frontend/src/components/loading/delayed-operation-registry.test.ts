import { afterEach, describe, expect, it, vi } from "vitest";

import {
  DelayedOperationRegistry,
  FOREGROUND_LOADING_DELAY_MS,
  runTrackedOperation,
  shouldAnimateGolfLoading,
  SingleFlight,
} from "./delayed-operation-registry";

describe("DelayedOperationRegistry", () => {
  afterEach(() => vi.useRealTimers());

  it("never presents a completed fast operation", () => {
    vi.useFakeTimers();
    const registry = new DelayedOperationRegistry();
    const finish = registry.start({ label: "Advancing week", presentation: "delayed" });

    vi.advanceTimersByTime(FOREGROUND_LOADING_DELAY_MS - 1);
    finish();
    vi.advanceTimersByTime(1);

    expect(registry.snapshot()).toEqual({ activeCount: 0, visibleOperations: [] });
  });

  it("presents a known expensive operation immediately and clears promptly", () => {
    vi.useFakeTimers();
    const registry = new DelayedOperationRegistry();
    const finish = registry.start({ label: "Simulating season", presentation: "immediate" });

    expect(registry.snapshot().visibleOperations).toHaveLength(1);

    finish();
    expect(registry.snapshot()).toEqual({ activeCount: 0, visibleOperations: [] });
  });

  it("presents an uncertain operation only after its configured delay", () => {
    vi.useFakeTimers();
    const registry = new DelayedOperationRegistry();
    const finish = registry.start({ label: "Advancing week", presentation: "delayed", delayMs: 400 });

    vi.advanceTimersByTime(399);
    expect(registry.snapshot().visibleOperations).toHaveLength(0);
    vi.advanceTimersByTime(1);
    expect(registry.snapshot().visibleOperations).toHaveLength(1);
    finish();
  });

  it("keeps no-presentation work out of the global overlay", () => {
    vi.useFakeTimers();
    const registry = new DelayedOperationRegistry();
    const finish = registry.start({ label: "Simulating shot", presentation: "none" });

    vi.advanceTimersByTime(FOREGROUND_LOADING_DELAY_MS + 60_000);
    expect(registry.snapshot()).toEqual({ activeCount: 1, visibleOperations: [] });
    finish();
  });

  it("does not let one concurrent completion dismiss another operation", () => {
    vi.useFakeTimers();
    const registry = new DelayedOperationRegistry();
    const finishSeason = registry.start({ label: "Simulating season", presentation: "immediate" });
    const finishEvent = registry.start({ label: "Simulating event", presentation: "immediate" });

    finishSeason();

    expect(registry.snapshot()).toEqual({
      activeCount: 1,
      visibleOperations: [{ id: "foreground-operation-1", label: "Simulating event" }],
    });
    finishEvent();
  });

  it("cleans pending timers on disposal", () => {
    vi.useFakeTimers();
    const registry = new DelayedOperationRegistry();
    registry.start({ label: "Advancing week", presentation: "delayed" });

    registry.dispose();
    vi.advanceTimersByTime(FOREGROUND_LOADING_DELAY_MS);

    expect(registry.snapshot()).toEqual({ activeCount: 0, visibleOperations: [] });
  });

  it("clears loading state when tracked work fails", async () => {
    const registry = new DelayedOperationRegistry();

    await expect(
      runTrackedOperation(registry, { label: "Simulating season", presentation: "immediate" }, async () => {
        throw new Error("request failed");
      }),
    ).rejects.toThrow("request failed");

    expect(registry.snapshot()).toEqual({ activeCount: 0, visibleOperations: [] });
  });

  it("rejects a duplicate submission until the active click is released", () => {
    const gate = new SingleFlight();

    expect(gate.tryStart()).toBe(true);
    expect(gate.tryStart()).toBe(false);
    gate.finish();
    expect(gate.tryStart()).toBe(true);
  });

  it("does not animate the loading mark when reduced motion is requested", () => {
    expect(shouldAnimateGolfLoading(true)).toBe(false);
    expect(shouldAnimateGolfLoading(false)).toBe(true);
  });
});
