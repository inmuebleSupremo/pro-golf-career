import { ordinalPosition } from "@/lib/career/labels";

type ScheduledEvent = { week: number; entered: boolean; name: string; location: string };
type CareerEvent = {
  eventName: string;
  results: readonly { season: number; date: string; position: number }[];
};

// Exact catalogue locations keep the Hub compact without guessing how unfamiliar locations are structured.
const TRAVEL_LOCATIONS: Readonly<Record<string, readonly [string, string?]>> = {
  "North Carolina, USA": ["North Carolina", "USA"],
  "Pinehurst region, North Carolina, USA": ["Pinehurst region", "North Carolina"],
  "Kiawah Island, South Carolina, USA": ["Kiawah Island", "South Carolina"],
  "North Carolina / Georgia, USA": ["North Carolina / Georgia", "USA"],
  "Georgia, USA": ["Georgia", "USA"],
  "Ohio, USA": ["Ohio", "USA"],
  "Michigan, USA": ["Michigan", "USA"],
  "New Jersey, USA": ["New Jersey", "USA"],
  "New York, USA": ["New York", "USA"],
  Belgium: ["Belgium"],
  "Surrey, England": ["Surrey", "England"],
  "Northumberland, England": ["Northumberland", "England"],
  "County Kildare, Ireland": ["County Kildare", "Ireland"],
  "Fife, Scotland": ["Fife", "Scotland"],
  "Monterey, California, USA": ["Monterey", "California"],
  "California, USA": ["California", "USA"],
  "Arizona, USA": ["Arizona", "USA"],
  "Albuquerque, New Mexico, USA": ["Albuquerque", "New Mexico"],
  "Carolinas, USA": ["Carolinas", "USA"],
};

/** Known catalogue places get two concise lines; unknown places retain their complete original text. */
export function formatTravelStop(location: string, week: number) {
  const [destination, region] = TRAVEL_LOCATIONS[location] ?? [location];
  return { destination, detail: `Week ${week}${region ? ` · ${region}` : ""}` };
}

/** Only a gap before a known later event is a verifiable open week in the current schedule. */
export function nextOpenWeek(
  schedule: readonly ScheduledEvent[],
  currentWeek: number,
): number | null {
  const lastKnownWeek = Math.max(0, ...schedule.map((event) => event.week));
  for (let week = currentWeek + 1; week < lastKnownWeek; week++) {
    if (!schedule.some((event) => event.week === week && event.entered)) return week;
  }
  return null;
}

/** Travel is the first confirmed entry after the event currently presented by the Hub. */
export function nextEnteredEventAfter(
  schedule: readonly ScheduledEvent[],
  currentEventWeek: number | null,
  currentWeek: number,
): ScheduledEvent | null {
  if (currentEventWeek == null || currentEventWeek < currentWeek) return null;
  return schedule
    .filter((event) => event.entered && event.week > currentEventWeek)
    .sort((a, b) => a.week - b.week)[0] ?? null;
}

/** Career records include prior seasons, unlike the current-season calendar. */
export function lastPlayerResult(events: readonly CareerEvent[]) {
  const latest = events
    .flatMap((event) => event.results.map((result) => ({ ...result, eventName: event.eventName })))
    .sort((a, b) => b.season - a.season || b.date.localeCompare(a.date))[0];
  return latest ? { finish: ordinalPosition(latest.position), eventName: latest.eventName } : null;
}
