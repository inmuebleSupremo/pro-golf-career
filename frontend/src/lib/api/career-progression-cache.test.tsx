// @vitest-environment happy-dom
import { act } from "react";
import { createRoot } from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { expect, it, vi } from "vitest";

import { useAdvanceWeek, useCompleteEvent } from "./play";

const state = vi.hoisted(() => ({ request: vi.fn() }));
vi.mock("@/lib/api/graphql-client", () => ({ gqlRequest: state.request }));

it("invalidates the profile and result history after completing an event", async () => {
  (
    globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT: boolean }
  ).IS_REACT_ACT_ENVIRONMENT = true;
  state.request.mockReset().mockResolvedValue({ completeEvent: { season: 1, week: 3 } });
  const id = "career-session";
  const client = new QueryClient({
    defaultOptions: { queries: { staleTime: 30_000 }, mutations: { retry: false } },
  });
  client.setQueryData(["career", id], { world: { week: 2 }, playerSeasonStats: [{ events: 1 }] });
  client.setQueryData(["profile", id], {
    playerProfile: { availableFunds: 30_000, worldRanking: null },
  });
  client.setQueryData(["careerRecords", id], {
    careerRecords: { events: [{ eventName: "Carolina Gateway Open" }] },
  });
  client.setQueryData(["profile", "other-session"], { playerProfile: { availableFunds: 50_000 } });

  const host = document.createElement("div");
  document.body.append(host);
  const root = createRoot(host);
  let complete!: ReturnType<typeof useCompleteEvent>;
  function Probe() {
    complete = useCompleteEvent(id);
    return null;
  }
  try {
    await act(async () =>
      root.render(
        <QueryClientProvider client={client}>
          <Probe />
        </QueryClientProvider>,
      ),
    );
    await act(async () => {
      await complete.mutateAsync();
    });

    expect(client.getQueryState(["career", id])?.isInvalidated).toBe(true);
    expect(client.getQueryState(["profile", id])?.isInvalidated).toBe(true);
    expect(client.getQueryState(["careerRecords", id])?.isInvalidated).toBe(true);
    expect(client.getQueryState(["profile", "other-session"])?.isInvalidated).toBe(false);
  } finally {
    act(() => root.unmount());
    host.remove();
    client.clear();
  }
});

it("invalidates the profile after advancing a week", async () => {
  (
    globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT: boolean }
  ).IS_REACT_ACT_ENVIRONMENT = true;
  state.request.mockReset().mockResolvedValue({ advanceWeek: { season: 1, week: 3 } });
  const id = "career-session";
  const client = new QueryClient({
    defaultOptions: { queries: { staleTime: 30_000 }, mutations: { retry: false } },
  });
  client.setQueryData(["profile", id], {
    playerProfile: { availableFunds: 30_000, worldRanking: null },
  });
  client.setQueryData(["careerRecords", id], { careerRecords: { events: [] } });

  const host = document.createElement("div");
  document.body.append(host);
  const root = createRoot(host);
  let advance!: ReturnType<typeof useAdvanceWeek>;
  function Probe() {
    advance = useAdvanceWeek(id);
    return null;
  }
  try {
    await act(async () =>
      root.render(
        <QueryClientProvider client={client}>
          <Probe />
        </QueryClientProvider>,
      ),
    );
    await act(async () => {
      await advance.mutateAsync();
    });

    expect(client.getQueryState(["profile", id])?.isInvalidated).toBe(true);
    expect(client.getQueryState(["careerRecords", id])?.isInvalidated).toBe(false);
  } finally {
    act(() => root.unmount());
    host.remove();
    client.clear();
  }
});
