import { expect, it } from "vitest";

import { lastPlayerResult, nextOpenWeek } from "./hub-summaries";

it("finds only a verifiable future week without an entered event", () => {
  const schedule = [
    { week: 4, entered: true, name: "Open A", location: "North" },
    { week: 5, entered: false, name: "Open B", location: "South" },
    { week: 7, entered: true, name: "Open C", location: "West" },
  ];
  expect(nextOpenWeek(schedule, 4)).toBe(5);
  expect(nextOpenWeek(schedule, 6)).toBeNull();
  expect(nextOpenWeek([], 4)).toBeNull();
});

it("uses the most recent career finish across seasons, rather than the best finish", () => {
  expect(
    lastPlayerResult([
      { eventName: "Old Major", results: [{ season: 1, date: "2026-06-01", position: 1 }] },
      { eventName: "Recent Open", results: [{ season: 2, date: "2027-04-10", position: 24 }] },
    ]),
  ).toEqual({ finish: "24th", eventName: "Recent Open" });
  expect(lastPlayerResult([])).toBeNull();
});
