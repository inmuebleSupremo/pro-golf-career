// @vitest-environment happy-dom
import { act } from "react";
import { createRoot, type Root } from "react-dom/client";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { CreateGolferForm } from "./create-golfer-form";

const state = vi.hoisted(() => ({
  create: vi.fn(),
  push: vi.fn(),
  refresh: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: state.push, refresh: state.refresh }),
}));
vi.mock("@/lib/api/mutations", () => ({
  useCreateCareer: () => ({ mutateAsync: state.create, isPending: false }),
}));

let host: HTMLDivElement;
let root: Root;

function button(label: string) {
  const found = [...host.querySelectorAll("button")].find(
    (item) => item.textContent?.trim() === label,
  );
  if (!found) throw new Error(`Missing button: ${label}`);
  return found;
}

async function click(label: string) {
  await act(async () => {
    button(label).click();
    await Promise.resolve();
  });
}

function input(id: string, value: string) {
  const element = host.querySelector<HTMLInputElement>(`#${id}`)!;
  const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, "value")!.set!;
  act(() => {
    setter.call(element, value);
    element.dispatchEvent(new Event("input", { bubbles: true }));
  });
}

function select(id: string, value: string) {
  const element = host.querySelector<HTMLSelectElement>(`#${id}`)!;
  act(() => {
    element.value = value;
    element.dispatchEvent(new Event("change", { bubbles: true }));
  });
}

async function details() {
  input("firstName", "Bez");
  input("lastName", "Inga");
  select("nationality", "GBR");
  await click("Next: Career setup");
}

function choose(group: string, value: string) {
  const item = host.querySelector<HTMLElement>(
    `[role="radiogroup"][aria-label="${group}"] [role="radio"][data-value="${value}"]`,
  );
  // Radix uses its value internally rather than a DOM data-value attribute.
  const radio =
    item ??
    [
      ...host.querySelectorAll<HTMLElement>(
        `[role="radiogroup"][aria-label="${group}"] [role="radio"]`,
      ),
    ].find((element) => element.textContent?.includes(value));
  if (!radio) throw new Error(`Missing choice: ${value}`);
  act(() => radio.click());
}

async function confirmation() {
  await details();
  choose("Starting Background", "Club Champion");
  choose("Playing-style archetype", "Mental Fortress");
  await click("Create golfer");
}

beforeEach(() => {
  (
    globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT: boolean }
  ).IS_REACT_ACT_ENVIRONMENT = true;
  state.create.mockReset();
  state.push.mockReset();
  state.refresh.mockReset();
  host = document.createElement("div");
  document.body.append(host);
  root = createRoot(host);
  act(() => root.render(<CreateGolferForm />));
});

afterEach(() => {
  act(() => root.unmount());
  host.remove();
});

describe("golfer creation steps", () => {
  it("validates details before advancing and preserves them on Back", async () => {
    await click("Next: Career setup");
    expect(host.textContent).toContain("Enter a first name.");
    expect(host.textContent).toContain("Enter a last name.");
    expect(host.textContent).toContain("Choose a nationality.");
    expect(button("Next: Career setup")).toBeTruthy();

    await details();
    expect(host.textContent).toContain("Your Starting Profile");
    expect(state.create).not.toHaveBeenCalled();
    await click("Back");
    expect(host.querySelector<HTMLInputElement>("#firstName")?.value).toBe("Bez");
    expect(host.querySelector<HTMLInputElement>("#lastName")?.value).toBe("Inga");
    expect(host.querySelector<HTMLSelectElement>("#nationality")?.value).toBe("GBR");
    expect(host.querySelector<HTMLInputElement>("#startAge")?.value).toBe("21");
  });

  it("keeps the existing starting-age limits and playing-hand choice", async () => {
    input("firstName", "Bez");
    input("lastName", "Inga");
    input("startAge", "30");
    select("nationality", "GBR");
    select("handedness", "LEFT");
    await click("Next: Career setup");
    expect(host.textContent).toContain("Between 16 and 22.");
    expect(button("Next: Career setup")).toBeTruthy();
    input("startAge", "16");
    await click("Next: Career setup");
    await click("Back");
    expect(host.querySelector<HTMLInputElement>("#startAge")?.value).toBe("16");
    expect(host.querySelector<HTMLSelectElement>("#handedness")?.value).toBe("LEFT");
  });

  it("keeps selections through confirmation and Back without persisting", async () => {
    await details();
    await click("Create golfer");
    expect(host.textContent).toContain("Choose a playing style.");
    expect(state.create).not.toHaveBeenCalled();

    choose("Starting Background", "Club Champion");
    choose("Playing-style archetype", "Mental Fortress");
    await click("Create golfer");
    expect(host.textContent).toContain("Bez Inga");
    expect(host.textContent).toContain("Club Champion · Mental Fortress · United Kingdom · Age 21");
    expect(host.textContent).toContain("Begins on the Development tour");
    expect(host.querySelectorAll(".golfer-creation__summary-card")).toHaveLength(4);
    expect(state.create).not.toHaveBeenCalled();

    await click("Back to setup");
    expect(host.querySelector('[role="radio"][aria-checked="true"]')?.textContent).toContain(
      "Club Champion",
    );
    expect(host.querySelectorAll('[role="radio"][aria-checked="true"]')).toHaveLength(2);
    await click("Back");
    expect(host.querySelector<HTMLInputElement>("#firstName")?.value).toBe("Bez");
  });

  it("creates only on Start, blocks duplicate clicks, and routes to the Hub", async () => {
    let resolve!: (value: { sessionId: string }) => void;
    state.create.mockImplementation(
      () =>
        new Promise((done) => {
          resolve = done;
        }),
    );
    await confirmation();
    await act(async () => {
      button("Start your career").click();
      button("Start your career").click();
      await Promise.resolve();
    });
    expect(state.create).toHaveBeenCalledTimes(1);
    expect(state.create).toHaveBeenCalledWith(
      expect.objectContaining({
        firstName: "Bez",
        lastName: "Inga",
        nationality: "GBR",
        background: "CLUB_CHAMPION",
        archetype: "MENTAL_FORTRESS",
        startAge: 21,
      }),
    );
    expect(button("Creating…").disabled).toBe(true);
    await act(async () => {
      resolve({ sessionId: "world-one" });
      await Promise.resolve();
    });
    expect(state.push).toHaveBeenCalledWith("/career/world-one");
    expect(state.refresh).toHaveBeenCalledTimes(1);
  });

  it("shows a creation error and permits a retry with the same choices", async () => {
    state.create
      .mockRejectedValueOnce(new Error("offline"))
      .mockResolvedValueOnce({ sessionId: "world-two" });
    await confirmation();
    await click("Start your career");
    expect(host.querySelector('[role="alert"]')?.textContent).toContain("Please try again.");
    expect(button("Start your career").disabled).toBe(false);
    await click("Start your career");
    expect(state.create).toHaveBeenCalledTimes(2);
    expect(state.create.mock.calls[1][0]).toEqual(state.create.mock.calls[0][0]);
    expect(state.push).toHaveBeenCalledWith("/career/world-two");
  });
});
