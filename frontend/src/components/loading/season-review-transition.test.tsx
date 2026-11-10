// @vitest-environment happy-dom
import { act, useEffect } from "react";
import { createRoot, type Root } from "react-dom/client";
import { afterEach, beforeEach, expect, it, vi } from "vitest";

import { OffSeasonReview } from "@/components/offseason/off-season-review";
import { ForegroundLoadingProvider, useForegroundOperation, useSeasonReviewTransition } from "./foreground-loading-provider";

const state = vi.hoisted(() => ({
  reviewPending: true,
  reviewFetching: false,
  reviewError: false,
  reviewSeason: 2,
  inboxPending: false,
  push: vi.fn(),
  refresh: vi.fn(),
}));

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: state.push, refresh: state.refresh }) }));
vi.mock("@/components/career/spoke", () => ({
  useSpokeGate: (query: { isPending: boolean; isError: boolean }) =>
    query.isPending ? <p>Loading review data</p> : query.isError ? <p>Review data unavailable</p> : null,
  SpokeMessage: ({ title }: { title: string }) => <p>{title}</p>,
}));
vi.mock("@/lib/api/queries", () => ({
  useSeasonReview: () => ({
    isPending: state.reviewPending, isFetching: state.reviewFetching, isError: state.reviewError, error: null,
    data: state.reviewPending ? undefined : { seasonReview: {
      season: state.reviewSeason, rankStart: null, rankEnd: null, playerGolferId: "golfer",
      stats: { season: state.reviewSeason, events: 0, wins: 0, topTens: 0, cuts: 0, bestFinish: 0, earnings: 0 },
      development: [], headlines: [],
    } },
  }),
  useCareerInbox: () => ({ isPending: state.inboxPending, isFetching: false, isError: false,
    error: null, data: { careerInbox: { items: [] } } }),
  useCareerOverview: () => ({ data: { world: { season: 3 } } }),
}));

let host: HTMLDivElement;
let root: Root;
let transitionApi: ReturnType<typeof useSeasonReviewTransition>;
const checkStatus = vi.fn();
const openReview = vi.fn();

function CareerControls() {
  const api = useSeasonReviewTransition();
  const pending = useForegroundOperation().hasPendingForegroundOperation;
  useEffect(() => { transitionApi = api; }, [api]);
  return <button type="button" data-pending={pending}>Ordinary career action</button>;
}

function render(showReview = false) {
  act(() => root.render(
    <ForegroundLoadingProvider>
      <CareerControls />
      {showReview ? <OffSeasonReview id="session-one" /> : null}
    </ForegroundLoadingProvider>,
  ));
}

function begin() {
  act(() => {
    expect(transitionApi.start({ sessionId: "session-one", requestId: "request-one", expectedSeason: 2,
      checkStatus, openReview })).toBe(true);
  });
}

function overlay() {
  return host.querySelector('[role="status"][aria-label]')!;
}

beforeEach(() => {
  (globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT: boolean }).IS_REACT_ACT_ENVIRONMENT = true;
  vi.stubGlobal("matchMedia", () => ({ matches: true, addEventListener: vi.fn(), removeEventListener: vi.fn() }));
  state.reviewPending = true;
  state.reviewFetching = false;
  state.reviewError = false;
  state.reviewSeason = 2;
  state.inboxPending = false;
  checkStatus.mockClear();
  openReview.mockClear();
  host = document.createElement("div");
  document.body.append(host);
  root = createRoot(host);
  render();
});

afterEach(() => {
  act(() => root.unmount());
  host.remove();
  vi.unstubAllGlobals();
});

it("keeps ordinary career UI inert through simulation, reconciliation and delayed review data", () => {
  begin();
  expect(host.querySelector("button[data-pending]")?.getAttribute("data-pending")).toBe("true");
  expect(host.querySelector(".contents")?.hasAttribute("inert")).toBe(true);
  expect(overlay().getAttribute("aria-label")).toContain("Simulating season");
  expect(document.activeElement).toBe(overlay());
  act(() => transitionApi.update("request-one", "checking"));
  expect(overlay().getAttribute("aria-label")).toContain("Checking season simulation status");
  act(() => transitionApi.update("request-one", "running"));
  expect(overlay().getAttribute("aria-label")).toContain("still running");
  act(() => transitionApi.update("request-one", "opening"));
  render(true);
  expect(host.querySelector(".contents")?.hasAttribute("inert")).toBe(true);
  expect(overlay().getAttribute("aria-label")).toContain("Preparing Season Review");
  state.reviewPending = false;
  state.reviewFetching = true; // stale cached review while the authoritative read continues
  render(true);
  expect(host.querySelector(".contents")?.hasAttribute("inert")).toBe(true);
  state.reviewFetching = false;
  render(true);
  expect(overlay()).toBeNull();
  expect(host.querySelector(".contents")?.hasAttribute("inert")).toBe(false);
  expect(host.textContent).toContain("Season 2");
  expect(host.textContent).toContain("in review");
});

it("offers manual recovery for unknown and failed outcomes without starting a simulation", () => {
  begin();
  act(() => transitionApi.update("request-one", "unknown"));
  expect(host.querySelector(".contents")?.hasAttribute("inert")).toBe(true);
  act(() => (overlay().querySelector("button") as HTMLButtonElement).click());
  expect(checkStatus).toHaveBeenCalledTimes(1);
  act(() => transitionApi.update("request-one", "failed"));
  expect(overlay().textContent).toContain("progress may have changed");
  act(() => (overlay().querySelector("button") as HTMLButtonElement).click());
  expect(overlay()).toBeNull();
  expect(host.querySelector(".contents")?.hasAttribute("inert")).toBe(false);
});

it("keeps a completed transition recoverable when the expected review is unavailable", () => {
  begin();
  act(() => transitionApi.update("request-one", "opening"));
  state.reviewPending = false;
  state.reviewSeason = 1; // a cached or incorrect older review must not unlock the career
  render(true);
  expect(overlay().getAttribute("aria-label")).toContain("not available yet");
  expect(host.querySelector(".contents")?.hasAttribute("inert")).toBe(true);
  act(() => (overlay().querySelector("button") as HTMLButtonElement).click());
  expect(openReview).toHaveBeenCalledTimes(1);
  expect(overlay().textContent).toContain("Reload review page");
});
