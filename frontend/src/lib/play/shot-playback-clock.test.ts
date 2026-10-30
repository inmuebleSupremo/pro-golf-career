import { describe, expect, it, vi } from "vitest";

import { startShotPlayback } from "./shot-playback-clock";

function frames() {
  let id = 0;
  const pending = new Map<number, (timestamp: number) => void>();
  const request = (frame: (timestamp: number) => void) => {
    pending.set(++id, frame);
    return id;
  };
  const cancel = (frameId: number) => { pending.delete(frameId); };
  const paint = (timestamp: number) => {
    const next = pending.entries().next();
    if (next.done) throw new Error("No pending animation frame");
    const [nextId, frame] = next.value;
    pending.delete(nextId);
    frame(timestamp);
  };
  return { request, cancel, paint, pending };
}

describe("shot playback clock", () => {
  it("shows the committed origin first, then advances from the first frame timestamp", () => {
    const clock = frames();
    const progress: number[] = [];
    const completed = vi.fn();
    startShotPlayback(100, (value) => progress.push(value), completed, clock.request, clock.cancel);
    expect(progress).toEqual([]);
    clock.paint(1000);
    expect(progress).toEqual([]);
    clock.paint(1016);
    expect(progress[0]).toBeCloseTo(.16);
    clock.paint(1050);
    expect(progress.at(-1)).toBeCloseTo(.5);
    clock.paint(1084);
    clock.paint(1100);
    expect(progress.at(-1)).toBe(1);
    expect(completed).not.toHaveBeenCalled();
    clock.paint(1116);
    expect(completed).toHaveBeenCalledTimes(1);
  });

  it("cancels stale frames and completion on unmount or Strict Mode restart", () => {
    const clock = frames();
    const progress = vi.fn();
    const completed = vi.fn();
    const stop = startShotPlayback(100, progress, completed, clock.request, clock.cancel);
    stop();
    expect(clock.pending.size).toBe(0);
    const restart = startShotPlayback(100, progress, completed, clock.request, clock.cancel);
    clock.paint(200);
    clock.paint(300);
    restart();
    expect(clock.pending.size).toBe(0);
    expect(completed).not.toHaveBeenCalled();
  });

  it.each([2702, 1990, 1350, 843])("keeps successive launch steps continuous for a %i ms shot", (duration) => {
    const clock = frames();
    const progress: number[] = [];
    startShotPlayback(duration, (value) => progress.push(value), undefined, clock.request, clock.cancel);
    clock.paint(1000);
    clock.paint(1016);
    clock.paint(1032);
    clock.paint(1048);
    expect(progress[0]).toBeCloseTo(16 / duration);
    expect(progress[1] - progress[0]).toBeCloseTo(16 / duration);
    expect(progress[2] - progress[1]).toBeCloseTo(16 / duration);
  });

  it("limits the distance advanced after a delayed frame without adding a launch acceleration phase", () => {
    const clock = frames();
    const progress: number[] = [];
    startShotPlayback(1000, (value) => progress.push(value), undefined, clock.request, clock.cancel);
    clock.paint(1000);
    clock.paint(1200);
    clock.paint(1216);
    expect(progress).toEqual([.034, .05]);
  });
});
