// @vitest-environment happy-dom
import { act } from "react";
import { createRoot, type Root } from "react-dom/client";
import { afterEach, beforeEach, expect, it, vi } from "vitest";

import { GraphQLRequestError } from "@/lib/api/graphql-client";
import { PlayEvent } from "./play-event";

const state = vi.hoisted(() => ({
  complete: vi.fn(),
  push: vi.fn(),
  replace: vi.fn(),
}));

vi.mock("next/image", () => ({ default: () => <div aria-hidden="true" /> }));
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: state.push, replace: state.replace, refresh: vi.fn() }),
}));
vi.mock("@/lib/api/queries", () => ({ usePlayerProfile: () => ({ data: null }) }));
vi.mock("@/lib/api/play", () => ({
  usePlayState: () => ({ data: {
    world: { season: 1, week: 1, hasPendingEvent: true, courseGeneratorVersion: 5 },
    currentSituation: null, currentEvent: null, eventLeaderboard: [], playerScorecard: null,
    playerPressure: null, playerMadeCut: null,
  }, isPending: false, isError: false, error: null }),
  usePlayingHole: () => ({ data: { playingHole: null } }),
  useCompleteEvent: () => ({ mutateAsync: state.complete, isPending: false }),
}));
vi.mock("@/components/loading/foreground-loading-provider", () => ({
  useForegroundOperation: () => ({ isPending: false, run: async (_: unknown, work: () => Promise<unknown>) => work() }),
}));

let host: HTMLDivElement;
let root: Root;

beforeEach(() => {
  (globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT: boolean }).IS_REACT_ACT_ENVIRONMENT = true;
  state.complete.mockReset().mockRejectedValue(
    new GraphQLRequestError("Player event completed, but checkpoint failed", 200, "COMPLETED_UNSAVED"));
  state.push.mockReset();
  state.replace.mockReset();
  host = document.createElement("div");
  document.body.append(host);
  root = createRoot(host);
});

afterEach(() => {
  act(() => root.unmount());
  host.remove();
});

it("warns on the finish screen and disables replay after an unsaved event completion", async () => {
  act(() => root.render(<PlayEvent id="career-session" />));
  const finish = [...host.querySelectorAll("button")].find((button) => button.textContent?.includes("Finish event"));
  expect(finish).toBeDefined();
  await act(async () => { finish!.click(); await Promise.resolve(); });
  expect(state.complete).toHaveBeenCalledTimes(1);
  expect(host.querySelector('[role="alert"]')?.textContent).toContain("event completed");
  expect(finish!.disabled).toBe(true);
  expect(state.replace).not.toHaveBeenCalled();
});
