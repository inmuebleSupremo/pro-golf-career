// @vitest-environment happy-dom
import { act } from "react";
import { createRoot } from "react-dom/client";
import { expect, it, vi } from "vitest";

import { CalendarView } from "./calendar-view";

const acknowledge = vi.hoisted(() => vi.fn());
const state = vi.hoisted(() => ({ calendarUnavailable: false, resting: false }));
const setResting = vi.hoisted(() => vi.fn());

vi.mock("@/components/career/spoke", () => ({
  useSpokeGate: () => state.calendarUnavailable ? <p>Calendar unavailable</p> : null,
  SpokeEmpty: ({ children }: { children: React.ReactNode }) => <p>{children}</p>,
}));
vi.mock("@/components/calendar/season-calendar", () => ({
  SeasonCalendar: () => <div>Season calendar</div>,
}));
vi.mock("@/components/command/identity-strip", () => ({
  SimSeasonControl: ({ id }: { id: string }) => <button data-session={id}>Sim season</button>,
}));
vi.mock("@/lib/api/queries", () => ({
  usePlayerCalendar: () => ({ isSuccess: true, data: { playerCalendar: [], playerResting: state.resting } }),
  useCareerOverview: () => ({ data: { world: { season: 1, week: 4 } } }),
  usePinPlacementStatus: () => ({ data: null }),
  usePlayerFitness: () => ({ data: null }),
}));
vi.mock("@/lib/api/manage", () => ({
  useAcknowledgeScheduleReview: () => ({ mutate: acknowledge }),
  useAdoptV5PinPlacement: () => ({ mutate: vi.fn(), isPending: false }),
  useSetResting: () => ({ mutate: setResting, isPending: false }),
}));

it("places the existing Sim Season action on Schedule", () => {
  (globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT: boolean }).IS_REACT_ACT_ENVIRONMENT = true;
  const host = document.createElement("div");
  document.body.append(host);
  const root = createRoot(host);
  state.calendarUnavailable = false;
  act(() => root.render(<CalendarView id="career-session" />));
  expect(host.querySelector('button[data-session="career-session"]')?.textContent).toBe("Sim season");
  expect(host.textContent).toContain("Season simulation");
  state.calendarUnavailable = true;
  act(() => root.render(<CalendarView id="career-session" />));
  expect(host.textContent).toContain("Calendar unavailable");
  expect(host.querySelector('button[data-session="career-session"]')).not.toBeNull();
  act(() => root.unmount());
  host.remove();
});

it("shows the authoritative rest decision after Schedule remounts", () => {
  (globalThis as typeof globalThis & { IS_REACT_ACT_ENVIRONMENT: boolean }).IS_REACT_ACT_ENVIRONMENT = true;
  state.calendarUnavailable = false;
  state.resting = false;
  setResting.mockClear();
  const host = document.createElement("div");
  document.body.append(host);
  const first = createRoot(host);
  act(() => first.render(<CalendarView id="rested-career" />));
  const restButton = Array.from(host.querySelectorAll("button")).find((node) => node.textContent === "Rest this season");
  act(() => restButton?.click());
  expect(setResting).toHaveBeenCalledWith(true);
  expect(host.textContent).toContain("Playing your schedule.");
  act(() => first.unmount());

  state.resting = true;
  const reopened = createRoot(host);
  act(() => reopened.render(<CalendarView id="rested-career" />));
  expect(host.textContent).toContain("Resting — sitting out every event this season.");
  expect(host.textContent).toContain("Resume playing");
  const button = Array.from(host.querySelectorAll("button")).find((node) => node.textContent === "Resume playing");
  act(() => button?.click());
  expect(setResting).toHaveBeenCalledWith(false);
  act(() => reopened.unmount());
  host.remove();
  state.resting = false;
});
