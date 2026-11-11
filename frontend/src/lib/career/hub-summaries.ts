import { ordinalPosition } from "@/lib/career/labels";

type ScheduledEvent = { week: number; entered: boolean; name: string; location: string };
type CareerEvent = {
  eventName: string;
  results: readonly { season: number; date: string; position: number }[];
};

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

/** Career records include prior seasons, unlike the current-season calendar. */
export function lastPlayerResult(events: readonly CareerEvent[]) {
  const latest = events
    .flatMap((event) => event.results.map((result) => ({ ...result, eventName: event.eventName })))
    .sort((a, b) => b.season - a.season || b.date.localeCompare(a.date))[0];
  return latest ? { finish: ordinalPosition(latest.position), eventName: latest.eventName } : null;
}
