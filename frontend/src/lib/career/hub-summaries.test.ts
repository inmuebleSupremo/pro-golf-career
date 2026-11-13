import { expect, it } from "vitest";

import { formatTravelStop, lastPlayerResult, nextEnteredEventAfter, nextOpenWeek } from "./hub-summaries";

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

it("selects the earliest later entered destination, not current, completed or unentered events", () => {
  const schedule = [
    { week: 8, entered: true, name: "Later Open", location: "Michigan" },
    { week: 2, entered: true, name: "Current Classic", location: "North Carolina" },
    { week: 5, entered: false, name: "Available Open", location: "Ohio" },
    { week: 7, entered: true, name: "Next Invitational", location: "Georgia" },
    { week: 1, entered: true, name: "Completed Open", location: "South Carolina" },
  ];
  expect(nextEnteredEventAfter(schedule, 2, 2)).toEqual(schedule[3]);
  expect(nextEnteredEventAfter(schedule, 7, 2)).toEqual(schedule[0]);
  expect(nextEnteredEventAfter(schedule, 8, 2)).toBeNull();
  expect(nextEnteredEventAfter(schedule, null, 2)).toBeNull();
  expect(nextEnteredEventAfter(schedule, 1, 2)).toBeNull();
});

it("treats a future entered event as current when this week has no entry", () => {
  const schedule = [
    { week: 3, entered: false, name: "Skipped Open", location: "Ohio" },
    { week: 5, entered: true, name: "Next Open", location: "Georgia" },
    { week: 9, entered: true, name: "Later Open", location: "Arizona" },
  ];
  expect(nextEnteredEventAfter(schedule, 5, 3)).toEqual(schedule[2]);
  expect(nextEnteredEventAfter(schedule, 5, 9)).toBeNull();
});

it.each([
  ["Kiawah Island, South Carolina, USA", 3, "Kiawah Island", "Week 3 · South Carolina"],
  ["Georgia, USA", 5, "Georgia", "Week 5 · USA"],
  ["Surrey, England", 17, "Surrey", "Week 17 · England"],
  ["County Kildare, Ireland", 19, "County Kildare", "Week 19 · Ireland"],
  ["Belgium", 15, "Belgium", "Week 15"],
  ["Monterey, California, USA", 23, "Monterey", "Week 23 · California"],
  ["Albuquerque, New Mexico, USA", 26, "Albuquerque", "Week 26 · New Mexico"],
  ["North Carolina / Georgia, USA", 4, "North Carolina / Georgia", "Week 4 · USA"],
] as const)("formats the known destination %s without losing its region", (location, week, destination, detail) => {
  expect(formatTravelStop(location, week)).toEqual({ destination, detail });
});

it("keeps unfamiliar and legacy locations intact instead of guessing from commas", () => {
  expect(formatTravelStop("Sandbelt", 6)).toEqual({ destination: "Sandbelt", detail: "Week 6" });
  expect(formatTravelStop("Old Coast, Eastern District, Newland", 8)).toEqual({
    destination: "Old Coast, Eastern District, Newland",
    detail: "Week 8",
  });
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
