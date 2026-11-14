// @vitest-environment happy-dom
import { act } from "react";
import { createRoot, type Root } from "react-dom/client";
import { afterEach, beforeEach, expect, it, vi } from "vitest";

import { OffSeasonReview } from "./off-season-review";

const state = vi.hoisted(() => ({ operation: "COMPLETED_UNSAVED" }));

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn(), refresh: vi.fn() }) }));
vi.mock("@/components/career/spoke", () => ({
  useSpokeGate: () => null,
  SpokeMessage: () => null,
}));
vi.mock("@/components/loading/foreground-loading-provider", () => ({
  useSeasonReviewTransition: () => ({ transition: null, update: vi.fn(), finish: vi.fn() }),
}));
vi.mock("@/lib/api/play", () => ({
  useSeasonAdvanceOperation: () => ({ data: { seasonAdvanceOperation: { state: state.operation } } }),
}));
vi.mock("@/lib/api/queries", () => ({
  useSeasonReview: () => ({ data: { seasonReview: {
    season: 1, rankStart: null, rankEnd: null, playerGolferId: "player",
    stats: { season: 1, events: 0, wins: 0, topTens: 0, cuts: 0, bestFinish: 0, earnings: 0 },
    development: [], headlines: [],
  } }, isPending: false, isFetching: false, isError: false }),
  useCareerInbox: () => ({ data: { careerInbox: { items: [] } }, isPending: false, isFetching: false,
    isError: false }),
  useCareerOverview: () => ({ data: { world: { season: 2 } } }),
}));

let host: HTMLDivElement;
let root: Root;

beforeEach(() => {
  (globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT: boolean }).IS_REACT_ACT_ENVIRONMENT = true;
  host = document.createElement("div");
  document.body.append(host);
  root = createRoot(host);
});

afterEach(() => {
  act(() => root.unmount());
  host.remove();
});

it("keeps Season Review visible while warning that its autosave failed", () => {
  act(() => root.render(<OffSeasonReview id="career-session" />));
  expect(host.querySelector('[aria-label="Off-season review"]')).not.toBeNull();
  expect(host.querySelector('[role="alert"]')?.textContent).toContain("recovery autosave was not updated");
  expect(host.textContent).toContain("Save & Return");
});
