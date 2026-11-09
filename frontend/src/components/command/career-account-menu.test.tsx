// @vitest-environment happy-dom
import { act } from "react";
import { createRoot, type Root } from "react-dom/client";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { GraphQLRequestError } from "@/lib/api/graphql-client";
import { CareerAccountMenu } from "./career-account-menu";

const state = vi.hoisted(() => ({
  push: vi.fn(),
  logout: vi.fn(async () => {}),
  save: vi.fn<() => Promise<{ saveId: string; savedAt: string }>>(),
  mutations: 0,
  foreground: false,
}));

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: state.push }) }));
vi.mock("@tanstack/react-query", () => ({ useIsMutating: () => state.mutations }));
vi.mock("@/components/app/logout-button", () => ({ useLogoutAction: () => ({ logout: state.logout }) }));
vi.mock("@/components/loading/foreground-loading-provider", () => ({
  useForegroundOperation: () => ({ hasPendingForegroundOperation: state.foreground }),
}));
vi.mock("@/lib/api/mutations", () => ({ useSaveCareer: () => ({ mutateAsync: state.save }) }));

let host: HTMLDivElement;
let root: Root;

function render(props: { hasPendingEvent?: boolean; statusReady?: boolean } = {}) {
  act(() => root.render(<CareerAccountMenu id="session-one" hasPendingEvent={props.hasPendingEvent ?? false}
    statusReady={props.statusReady ?? true} />));
}

function button(name: string): HTMLButtonElement {
  const found = [...host.querySelectorAll("button")].find((item) =>
    item.getAttribute("aria-label") === name || item.textContent?.trim() === name);
  if (!found) throw new Error(`Missing button: ${name}`);
  return found;
}

function click(name: string) {
  act(() => button(name).click());
}

function openExit(target: "My Careers" | "Sign Out" = "My Careers") {
  click("Account menu");
  click(target);
  expect(host.querySelector("dialog")?.open).toBe(true);
}

async function settle() {
  await act(async () => { await Promise.resolve(); });
}

beforeEach(() => {
  (globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT: boolean }).IS_REACT_ACT_ENVIRONMENT = true;
  state.push.mockClear();
  state.logout.mockReset().mockResolvedValue(undefined);
  state.save.mockReset().mockResolvedValue({ saveId: "career-original", savedAt: "2026-11-09T10:00:00Z" });
  state.mutations = 0;
  state.foreground = false;
  host = document.createElement("div");
  document.body.append(host);
  root = createRoot(host);
});

afterEach(() => {
  act(() => root.unmount());
  host.remove();
  document.body.style.overflow = "";
  vi.restoreAllMocks();
});

describe("career account exits", () => {
  it("offers exactly two keyboard-accessible actions from one compact desktop/mobile trigger", () => {
    render();
    const trigger = button("Account menu");
    expect(trigger.className).toContain("size-10");
    expect(trigger.className).toContain("sm:w-auto");
    expect(trigger.className).not.toContain("lg:hidden");
    expect(trigger.getAttribute("aria-haspopup")).toBe("menu");
    expect(trigger.getAttribute("aria-expanded")).toBe("false");
    click("Account menu");
    const menu = host.querySelector('[role="menu"]')!;
    expect(trigger.getAttribute("aria-expanded")).toBe("true");
    expect([...menu.querySelectorAll('[role="menuitem"]')].map((item) => item.textContent?.trim()))
      .toEqual(["My Careers", "Sign Out"]);
    expect(document.activeElement).toBe(menu.querySelector("button"));
    act(() => menu.dispatchEvent(new KeyboardEvent("keydown", { key: "ArrowDown", bubbles: true })));
    expect(document.activeElement).toBe(menu.querySelectorAll("button")[1]);
    act(() => menu.dispatchEvent(new KeyboardEvent("keydown", { key: "Escape", bubbles: true })));
    expect(host.querySelector('[role="menu"]')).toBeNull();
    expect(document.activeElement).toBe(trigger);
    click("Account menu");
    act(() => document.body.dispatchEvent(new PointerEvent("pointerdown", { bubbles: true })));
    expect(host.querySelector('[role="menu"]')).toBeNull();
  });

  it("uses one confirmation for either destination and Cancel preserves the career", () => {
    render();
    openExit();
    expect(host.querySelector("dialog")?.textContent).toContain("Go to My Careers?");
    expect(document.activeElement).toBe(button("Cancel"));
    expect(document.body.style.overflow).toBe("hidden");
    click("Cancel");
    expect(host.querySelector("dialog")?.open).toBe(false);
    expect(state.push).not.toHaveBeenCalled();
    expect(state.save).not.toHaveBeenCalled();
    expect(document.body.style.overflow).toBe("");
    openExit("Sign Out");
    expect(host.querySelector("dialog")?.textContent).toContain("Go to Sign Out?");
    click("Cancel");
    expect(state.logout).not.toHaveBeenCalled();
  });

  it("waits for a confirmed named-career save and rejects duplicate submissions", async () => {
    let resolveSave!: (value: { saveId: string; savedAt: string }) => void;
    state.save.mockImplementation(() => new Promise((resolve) => { resolveSave = resolve; }));
    render();
    openExit();
    click("Save & Exit");
    click("Saving…");
    expect(state.save).toHaveBeenCalledTimes(1);
    expect(state.push).not.toHaveBeenCalled();
    expect(button("Cancel").disabled).toBe(true);
    await act(async () => resolveSave({ saveId: "career-original", savedAt: "2026-11-09T10:00:00Z" }));
    await settle();
    expect(state.push).toHaveBeenCalledExactlyOnceWith("/saves");
  });

  it("accepts an autosave-recovered named result before sign-out", async () => {
    state.save.mockResolvedValue({ saveId: "career-new-from-autosave", savedAt: "2026-11-09T10:00:00Z" });
    render();
    openExit("Sign Out");
    click("Save & Exit");
    await settle();
    expect(state.save).toHaveBeenCalledTimes(1);
    expect(state.logout).toHaveBeenCalledTimes(1);
    expect(state.push).not.toHaveBeenCalled();
  });

  it("keeps the session open after a save failure and offers Retry and Cancel", async () => {
    state.save.mockRejectedValueOnce(new Error("disk failed"));
    render();
    openExit();
    click("Save & Exit");
    await settle();
    expect(state.push).not.toHaveBeenCalled();
    expect(host.querySelector('[role="alert"]')?.textContent).toContain("could not be saved");
    click("Retry Save & Exit");
    await settle();
    expect(state.save).toHaveBeenCalledTimes(2);
    expect(state.push).toHaveBeenCalledExactlyOnceWith("/saves");
  });

  it("blocks stale-save conflicts without forcing an overwrite or exit", async () => {
    state.save.mockRejectedValue(new GraphQLRequestError("changed", 200, "CONFLICT"));
    render();
    openExit();
    click("Save & Exit");
    await settle();
    expect(host.querySelector('[role="alert"]')?.textContent).toContain("another session");
    expect(button("Save & Exit").disabled).toBe(true);
    expect(state.push).not.toHaveBeenCalled();
    expect(state.logout).not.toHaveBeenCalled();
    click("Cancel");
    expect(state.save).toHaveBeenCalledTimes(1);
  });

  it("explains pending-event restrictions and does not try to save", () => {
    render({ hasPendingEvent: true });
    openExit();
    expect(host.querySelector('[role="status"]')?.textContent).toContain("Finish the current event");
    expect(button("Save & Exit").disabled).toBe(true);
    click("Save & Exit");
    expect(state.save).not.toHaveBeenCalled();
    click("Cancel");
    expect(state.push).not.toHaveBeenCalled();
  });

  it("handles a pending-event rejection from the backend without exiting", async () => {
    state.save.mockRejectedValue(new GraphQLRequestError("Cannot snapshot while a player event is pending", 200, "BAD_REQUEST"));
    render();
    openExit();
    click("Save & Exit");
    await settle();
    expect(host.querySelector('[role="alert"]')?.textContent).toContain("Finish the current event");
    expect(button("Save & Exit").disabled).toBe(true);
    expect(state.push).not.toHaveBeenCalled();
  });

  it("blocks account entry and exit during in-flight career actions", () => {
    render();
    state.mutations = 1;
    render();
    expect(button("Account menu").disabled).toBe(true);
    state.mutations = 0;
    render();
    openExit();
    state.foreground = true;
    render();
    expect(button("Save & Exit").disabled).toBe(true);
    expect(button("Exit without Saving").disabled).toBe(true);
    expect(host.textContent).toContain("Wait for the current action");
    click("Cancel");
    expect(state.push).not.toHaveBeenCalled();
  });

  it("requires a separate progress-loss confirmation before either unsaved exit", async () => {
    render();
    openExit();
    click("Exit without Saving");
    expect(state.push).not.toHaveBeenCalled();
    expect(host.textContent).toContain("Unsaved progress in this career will be lost");
    click("Keep playing");
    expect(state.push).not.toHaveBeenCalled();
    click("Exit without Saving");
    click("Confirm exit without saving");
    await settle();
    expect(state.push).toHaveBeenCalledExactlyOnceWith("/saves");
    expect(state.save).not.toHaveBeenCalled();
  });

  it("requires the same second confirmation before signing out without saving", async () => {
    render();
    openExit("Sign Out");
    click("Exit without Saving");
    expect(state.logout).not.toHaveBeenCalled();
    click("Confirm exit without saving");
    await settle();
    expect(state.logout).toHaveBeenCalledTimes(1);
    expect(state.save).not.toHaveBeenCalled();
  });
});
