// @vitest-environment happy-dom
import { act } from "react";
import { createRoot, type Root } from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, expect, it, vi } from "vitest";

import { SaveCareerDocument } from "@/lib/graphql/operations";
import { useSaveCareer } from "./mutations";

const request = vi.hoisted(() => vi.fn());
vi.mock("@/lib/api/graphql-client", () => ({ gqlRequest: request }));

let host: HTMLDivElement;
let root: Root;
let queryClient: QueryClient;
let savePromise: ReturnType<ReturnType<typeof useSaveCareer>["mutateAsync"]>;

function Harness() {
  const save = useSaveCareer("active-session");
  return <button type="button" onClick={() => { savePromise = save.mutateAsync(); }}>Save</button>;
}

beforeEach(() => {
  (globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT: boolean }).IS_REACT_ACT_ENVIRONMENT = true;
  request.mockReset();
  queryClient = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
  host = document.createElement("div");
  document.body.append(host);
  root = createRoot(host);
  act(() => root.render(<QueryClientProvider client={queryClient}><Harness /></QueryClientProvider>));
});

afterEach(() => {
  act(() => root.unmount());
  host.remove();
  queryClient.clear();
  vi.restoreAllMocks();
});

it.each(["career-original", "career-new-from-autosave"])(
  "saves to the backend-selected %s slot and refreshes My Careers",
  async (saveId) => {
    request.mockResolvedValue({ saveCareer: { saveId, savedAt: "2026-11-09T10:00:00Z" } });
    queryClient.setQueryData(["saves"], { listSaves: [] });
    const invalidate = vi.spyOn(queryClient, "invalidateQueries");
    let result: Awaited<typeof savePromise> | undefined;
    await act(async () => {
      host.querySelector("button")?.click();
      result = await savePromise;
    });
    expect(request).toHaveBeenCalledExactlyOnceWith(SaveCareerDocument, { id: "active-session" });
    expect(result?.saveId).toBe(saveId);
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["saves"] });
    expect(queryClient.getQueryState(["saves"])?.isInvalidated).toBe(true);
  },
);
