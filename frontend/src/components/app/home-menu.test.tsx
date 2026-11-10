// @vitest-environment happy-dom
import { act } from "react";
import { createRoot, type Root } from "react-dom/client";
import { afterEach, beforeEach, expect, it, vi } from "vitest";

import { featuredAndOtherSaves, HomeMenu } from "./home-menu";

const state = vi.hoisted(() => ({
  saves: [] as Array<{ saveId: string; savedAt: string; season: number; week: number; playerGolferId: string | null }>,
  load: vi.fn(async () => "session-one"),
  remove: vi.fn(async () => true),
  push: vi.fn(),
  refresh: vi.fn(),
}));

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: state.push, refresh: state.refresh }) }));
vi.mock("@/lib/api/queries", () => ({ useSaves: () => ({
  data: { listSaves: state.saves }, isPending: false, isError: false, error: null,
}) }));
vi.mock("@/lib/api/mutations", () => ({
  useLoadCareer: () => ({ mutateAsync: state.load, isPending: false }),
  useDeleteSave: () => ({ mutateAsync: state.remove, isPending: false, isError: false }),
}));

const savedAt = "2026-11-09T10:00:00Z";
function save(saveId: string) {
  return { saveId, savedAt, season: 2, week: 1, playerGolferId: "golfer-one" };
}

let host: HTMLDivElement;
let root: Root;

function render() {
  act(() => root.render(<HomeMenu />));
}

function click(container: Element, label: string) {
  const button = [...container.querySelectorAll("button")].find((item) => item.textContent?.trim() === label);
  if (!button) throw new Error(`Missing button: ${label}`);
  act(() => button.click());
}

beforeEach(() => {
  (globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT: boolean }).IS_REACT_ACT_ENVIRONMENT = true;
  state.saves = [];
  state.load.mockClear();
  state.remove.mockClear();
  state.push.mockClear();
  state.refresh.mockClear();
  host = document.createElement("div");
  document.body.append(host);
  root = createRoot(host);
});

afterEach(() => {
  act(() => root.unmount());
  host.remove();
});

it("excludes the featured save from Other saves only when its ID matches", () => {
  const named = save("career-one");
  const selection = featuredAndOtherSaves([named, { ...named }, save("career-two")]);
  expect(selection.featured.saveId).toBe("career-one");
  expect(selection.others.map((entry) => entry.saveId)).toEqual(["career-two"]);
});

it("retains distinct named and autosave slots with identical date and progress", () => {
  state.saves = [save("career-one"), save("autosave"), save("career-two")];
  render();
  const other = host.querySelector("section ul")!;
  expect(other.querySelectorAll("li")).toHaveLength(2);
  expect(host.textContent).toContain("Named career");
  expect(other.textContent).toContain("Autosave · recovery");
  expect(other.textContent).toContain("9 Nov 2026");
  expect(other.textContent).toContain("Season 2 · Week 1");
  expect(host.querySelector('a[href="/new"]')?.textContent).toContain("New career");
});

it("keeps Continue, Resume and Delete tied to their original save IDs", async () => {
  state.saves = [save("career-one"), save("autosave")];
  render();
  await act(async () => { click(host, "Continue"); await Promise.resolve(); });
  expect(state.load).toHaveBeenCalledWith("career-one");
  expect(state.push).toHaveBeenCalledWith("/career/session-one");

  const other = host.querySelector("section ul")!;
  await act(async () => { click(other, "Resume"); await Promise.resolve(); });
  expect(state.load).toHaveBeenCalledWith("autosave");
  click(other, "Delete");
  expect(state.remove).not.toHaveBeenCalled();
  const confirm = [...other.querySelectorAll("button")].find((item) => item.textContent?.trim() === "Delete");
  act(() => confirm?.click());
  await act(async () => { await Promise.resolve(); });
  expect(state.remove).toHaveBeenCalledExactlyOnceWith("autosave");
});
