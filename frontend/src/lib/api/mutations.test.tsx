// @vitest-environment happy-dom
import { act } from "react";
import { createRoot } from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { expect, it, vi } from "vitest";

import { useCreateCareer, type CreateCareerInput } from "./mutations";

const state = vi.hoisted(() => ({ request: vi.fn() }));
vi.mock("@/lib/api/graphql-client", () => ({ gqlRequest: state.request }));

it("reuses the save slot when retrying after an uncertain save response", async () => {
  (
    globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT: boolean }
  ).IS_REACT_ACT_ENVIRONMENT = true;
  state.request
    .mockReset()
    .mockResolvedValueOnce({ createWorld: { id: "world-one", courseGeneratorVersion: 1 } })
    .mockResolvedValueOnce({ createPlayer: "golfer-one" })
    .mockRejectedValueOnce(new Error("response lost"))
    .mockResolvedValueOnce({ createWorld: { id: "world-two", courseGeneratorVersion: 1 } })
    .mockResolvedValueOnce({ createPlayer: "golfer-two" })
    .mockResolvedValueOnce({ save: true });

  const host = document.createElement("div");
  document.body.append(host);
  const root = createRoot(host);
  const client = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
  let create!: ReturnType<typeof useCreateCareer>;
  function Probe() {
    create = useCreateCareer();
    return null;
  }
  await act(async () =>
    root.render(
      <QueryClientProvider client={client}>
        <Probe />
      </QueryClientProvider>,
    ),
  );

  const input: CreateCareerInput = {
    firstName: "Bez",
    lastName: "Inga",
    nationality: "GBR",
    startAge: 21,
    handedness: "RIGHT",
    archetype: "MENTAL_FORTRESS",
    background: "ELITE_AMATEUR",
  };
  await act(async () => {
    await expect(create.mutateAsync(input)).rejects.toThrow("response lost");
  });
  await act(async () => {
    await create.mutateAsync(input);
  });

  const firstSave = state.request.mock.calls[2][1] as { saveId: string };
  const retrySave = state.request.mock.calls[5][1] as { saveId: string };
  expect(firstSave.saveId).toMatch(/^career-/);
  expect(retrySave.saveId).toBe(firstSave.saveId);
  act(() => root.unmount());
  host.remove();
  client.clear();
});
