// @vitest-environment happy-dom
import { act } from "react";
import { createRoot } from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, expect, it, vi } from "vitest";

import { LogoutButton } from "./logout-button";

const router = vi.hoisted(() => ({ push: vi.fn(), refresh: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => router }));

afterEach(() => {
  vi.unstubAllGlobals();
  vi.clearAllMocks();
});

it("reuses the established logout request, cache clear and login navigation", async () => {
  (globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT: boolean }).IS_REACT_ACT_ENVIRONMENT = true;
  const request = vi.fn().mockResolvedValue({ ok: true });
  vi.stubGlobal("fetch", request);
  const queryClient = new QueryClient();
  queryClient.setQueryData(["career", "active-session"], { world: {} });
  const host = document.createElement("div");
  document.body.append(host);
  const root = createRoot(host);
  act(() => root.render(<QueryClientProvider client={queryClient}><LogoutButton /></QueryClientProvider>));
  await act(async () => { host.querySelector("button")?.click(); await Promise.resolve(); });
  expect(request).toHaveBeenCalledExactlyOnceWith("/api/auth/logout", { method: "POST" });
  expect(queryClient.getQueryData(["career", "active-session"])).toBeUndefined();
  expect(router.push).toHaveBeenCalledExactlyOnceWith("/login");
  expect(router.refresh).toHaveBeenCalledTimes(1);
  act(() => root.unmount());
  host.remove();
  queryClient.clear();
});
