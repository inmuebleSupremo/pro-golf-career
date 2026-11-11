// @vitest-environment happy-dom
import { act } from "react";
import { createRoot, type Root } from "react-dom/client";
import { afterEach, beforeEach, expect, it, vi } from "vitest";

import { NextAction } from "./identity-strip";

const state = vi.hoisted(() => ({
  push: vi.fn(),
  refresh: vi.fn(),
  advance: vi.fn(),
  complete: vi.fn(),
  operation: null as { state: string } | null,
  foregroundPending: false,
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: state.push, refresh: state.refresh }),
}));
vi.mock("@/lib/api/queries", () => ({
  useCareerOverview: () => ({ data: { world: { season: 1 } } }),
  usePlayerProfile: () => ({ data: null }),
}));
vi.mock("@/lib/api/play", () => ({
  useAdvanceWeek: () => ({ mutateAsync: state.advance, isPending: false }),
  useCompleteEvent: () => ({ mutateAsync: state.complete, isPending: false }),
  useSeasonAdvanceOperation: () => ({
    data: { seasonAdvanceOperation: state.operation },
    isPending: false,
    isError: false,
  }),
}));
vi.mock("@/components/loading/foreground-loading-provider", () => ({
  useForegroundOperation: () => ({
    isPending: state.foregroundPending,
    hasPendingForegroundOperation: state.foregroundPending,
    run: async (_options: unknown, work: () => Promise<unknown>) => work(),
  }),
  useSeasonReviewTransition: () => ({ transition: null }),
}));

let host: HTMLDivElement;
let root: Root;
function render(hasPendingEvent = false, pendingEventFinished = false, eventInProgress = false) {
  act(() =>
    root.render(
      <NextAction
        id="career-1"
        hub
        hasPendingEvent={hasPendingEvent}
        pendingEventFinished={pendingEventFinished}
        eventInProgress={eventInProgress}
      />,
    ),
  );
}

beforeEach(() => {
  (
    globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT: boolean }
  ).IS_REACT_ACT_ENVIRONMENT = true;
  state.push.mockReset();
  state.refresh.mockReset();
  state.advance.mockReset();
  state.complete.mockReset();
  state.operation = null;
  state.foregroundPending = false;
  host = document.createElement("div");
  document.body.append(host);
  root = createRoot(host);
});
afterEach(() => {
  act(() => root.unmount());
  host.remove();
});

it("advances without an upcoming event and preserves the off-season transition", async () => {
  state.advance.mockResolvedValue({ advanceWeek: { season: 2, hasPendingEvent: true } });
  render();
  expect(host.querySelector("button")?.className).toContain("bg-primary");
  await act(async () => {
    host.querySelector("button")?.click();
    await Promise.resolve();
  });
  expect(state.advance).toHaveBeenCalledTimes(1);
  expect(state.push).toHaveBeenCalledWith("/career/career-1/offseason");
  expect(state.push).not.toHaveBeenCalledWith("/career/career-1/play");
});

it("shows the appropriate pending action and blocks advancement during season simulation", () => {
  render(true, false, true);
  expect(host.textContent).toContain("Continue event");
  expect(host.querySelector('a[href="/career/career-1/play"]')).not.toBeNull();
  render(true, true);
  expect(host.textContent).toContain("Finish event");
  state.operation = { state: "RUNNING" };
  render();
  expect(host.textContent).toContain("Check Schedule for season progression status");
  expect(host.querySelector("button")).toBeNull();
  expect(state.advance).not.toHaveBeenCalled();
});
