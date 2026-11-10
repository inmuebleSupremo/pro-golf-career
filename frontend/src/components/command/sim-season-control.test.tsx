// @vitest-environment happy-dom
import { act } from "react";
import { createRoot, type Root } from "react-dom/client";
import { afterEach, beforeEach, expect, it, vi } from "vitest";

import { SimSeasonControl } from "./identity-strip";

type Operation = { requestId: string; startingSeason: number; state: "RUNNING" | "COMPLETED" | "FAILED"; completedSeason: number | null };
const state = vi.hoisted(() => ({
  push: vi.fn(),
  invalidate: vi.fn(async () => {}),
  mutate: vi.fn(),
  refetch: vi.fn(),
  operation: null as Operation | null,
  statusError: false,
  season: 1,
}));

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: state.push }) }));
vi.mock("@tanstack/react-query", () => ({ useQueryClient: () => ({ invalidateQueries: state.invalidate }) }));
vi.mock("@/lib/api/queries", () => ({ useCareerOverview: () => ({ data: { world: { season: state.season } } }) }));
vi.mock("@/lib/api/play", () => ({
  useAdvanceSeason: () => ({ mutateAsync: state.mutate, isPending: false }),
  useSeasonAdvanceOperation: () => ({
    data: { seasonAdvanceOperation: state.operation },
    isPending: false,
    isError: state.statusError,
    refetch: state.refetch,
  }),
}));
vi.mock("@/components/loading/foreground-loading-provider", () => ({
  useForegroundOperation: () => ({ hasPendingForegroundOperation: false, isPending: false,
    run: async (_options: unknown, work: () => Promise<unknown>) => work() }),
}));

let host: HTMLDivElement;
let root: Root;

function render() {
  act(() => root.render(<SimSeasonControl id="career-session" />));
}

function button(name: string) {
  const found = [...host.querySelectorAll("button")].find((item) => item.textContent?.trim() === name);
  if (!found) throw new Error(`Missing ${name}`);
  return found;
}

function click(name: string) {
  act(() => button(name).click());
}

async function confirm() {
  click("Sim season");
  await act(async () => { button("Confirm").click(); await Promise.resolve(); });
}

beforeEach(() => {
  (globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT: boolean }).IS_REACT_ACT_ENVIRONMENT = true;
  state.push.mockClear();
  state.invalidate.mockClear();
  state.mutate.mockReset();
  state.refetch.mockReset().mockResolvedValue({ data: { seasonAdvanceOperation: null } });
  state.operation = null;
  state.statusError = false;
  state.season = 1;
  host = document.createElement("div");
  document.body.append(host);
  root = createRoot(host);
  render();
});

afterEach(() => {
  act(() => root.unmount());
  host.remove();
  vi.restoreAllMocks();
});

it("shows confirmed success and refreshes authoritative career queries", async () => {
  state.mutate.mockImplementation(async ({ requestId }: { requestId: string }) => ({
    advanceSeason: { requestId, startingSeason: 1, state: "COMPLETED", completedSeason: 2 },
  }));
  await confirm();
  expect(state.mutate).toHaveBeenCalledTimes(1);
  expect(state.mutate.mock.calls[0][0].expectedSeason).toBe(1);
  expect(state.invalidate).toHaveBeenCalledTimes(1);
  expect(state.push).toHaveBeenCalledExactlyOnceWith("/career/career-session/offseason");
  expect(host.textContent).toContain("Season complete");
});

it("permits an intentional later season with a new starting-season guard", async () => {
  state.season = 2;
  state.operation = { requestId: "prior-request", startingSeason: 1, state: "COMPLETED", completedSeason: 2 };
  state.mutate.mockImplementation(async ({ requestId }: { requestId: string }) => ({
    advanceSeason: { requestId, startingSeason: 2, state: "COMPLETED", completedSeason: 3 },
  }));
  render();
  await confirm();
  expect(state.mutate.mock.calls[0][0].expectedSeason).toBe(2);
  expect(state.mutate.mock.calls[0][0].requestId).not.toBe("prior-request");
  expect(state.push).toHaveBeenCalledExactlyOnceWith("/career/career-session/offseason");
});

it("reconciles a lost response when the backend already completed", async () => {
  state.mutate.mockRejectedValue(new TypeError("fetch failed"));
  state.refetch.mockImplementation(async () => {
    const requestId = state.mutate.mock.calls[0][0].requestId;
    state.operation = { requestId, startingSeason: 1, state: "COMPLETED", completedSeason: 2 };
    return { data: { seasonAdvanceOperation: state.operation } };
  });
  await confirm();
  render();
  await act(async () => { await Promise.resolve(); });
  expect(state.push).toHaveBeenCalledExactlyOnceWith("/career/career-session/offseason");
  expect(state.invalidate).toHaveBeenCalledTimes(1);
  expect(host.textContent).not.toContain("couldn't be completed");
});

it("shows a running operation, blocks resubmission and later completes", async () => {
  state.mutate.mockRejectedValue(new TypeError("fetch failed"));
  state.refetch.mockImplementation(async () => {
    const requestId = state.mutate.mock.calls[0][0].requestId;
    state.operation = { requestId, startingSeason: 1, state: "RUNNING", completedSeason: null };
    return { data: { seasonAdvanceOperation: state.operation } };
  });
  await confirm();
  render();
  expect(host.textContent).toContain("still running");
  expect(button("Confirm").disabled).toBe(true);
  expect(state.push).not.toHaveBeenCalled();
  state.operation = { ...state.operation!, state: "COMPLETED", completedSeason: 2 };
  render();
  await act(async () => { await Promise.resolve(); });
  expect(state.push).toHaveBeenCalledExactlyOnceWith("/career/career-session/offseason");
});

it("adopts a running backend operation after remount and recognizes its completion", async () => {
  state.operation = { requestId: "before-remount", startingSeason: 1, state: "RUNNING", completedSeason: null };
  render();
  expect(host.textContent).toContain("still running");
  await act(async () => { await new Promise((resolve) => setTimeout(resolve, 0)); });
  state.operation = { ...state.operation, state: "COMPLETED", completedSeason: 2 };
  render();
  await act(async () => { await Promise.resolve(); });
  expect(state.push).toHaveBeenCalledExactlyOnceWith("/career/career-session/offseason");
  expect(state.mutate).not.toHaveBeenCalled();
});

it("shows completion unknown when the operation record cannot be found", async () => {
  state.mutate.mockRejectedValue(new TypeError("fetch failed"));
  await confirm();
  render();
  expect(host.textContent).toContain("completion is unknown");
  expect(button("Confirm").disabled).toBe(true);
  expect(button("Check status")).toBeTruthy();
  expect(state.push).not.toHaveBeenCalled();
});

it("keeps Check status available after a failed status read without restarting Sim Season", async () => {
  state.mutate.mockRejectedValue(new TypeError("fetch failed"));
  state.refetch.mockImplementationOnce(async () => {
    state.statusError = true;
    return { data: { seasonAdvanceOperation: null } };
  });
  await confirm();
  render();
  expect(host.textContent).toContain("completion is unknown");
  const originalRequestId = state.mutate.mock.calls[0][0].requestId;
  state.refetch.mockImplementationOnce(async () => {
    state.statusError = false;
    state.operation = { requestId: originalRequestId, startingSeason: 1, state: "COMPLETED", completedSeason: 2 };
    return { data: { seasonAdvanceOperation: state.operation } };
  });
  await act(async () => { button("Check status").click(); await Promise.resolve(); });
  render();
  await act(async () => { await Promise.resolve(); });
  expect(state.refetch).toHaveBeenCalledTimes(2);
  expect(state.mutate).toHaveBeenCalledTimes(1);
  expect(state.push).toHaveBeenCalledExactlyOnceWith("/career/career-session/offseason");
});

it("keeps a failed post-progress attempt blocked without claiming rollback", async () => {
  state.mutate.mockRejectedValue(new Error("simulation failed"));
  state.refetch.mockImplementation(async () => {
    const requestId = state.mutate.mock.calls[0][0].requestId;
    state.operation = { requestId, startingSeason: 1, state: "FAILED", completedSeason: null };
    return { data: { seasonAdvanceOperation: state.operation } };
  });
  await confirm();
  render();
  expect(host.textContent).toContain("progress may have changed");
  expect(button("Confirm").disabled).toBe(true);
  expect(state.push).not.toHaveBeenCalled();
});

it("treats an unavailable session status as unknown and blocks a new attempt", () => {
  state.statusError = true;
  render();
  expect(button("Sim season").disabled).toBe(true);
  expect(host.textContent).toContain("completion is unknown");
});
