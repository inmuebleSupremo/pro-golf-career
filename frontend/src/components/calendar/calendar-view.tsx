"use client";

import { useState } from "react";

import { Button } from "@/components/ui/button";
import { SpokeShell, SpokeEmpty, useSpokeGate } from "@/components/career/spoke";
import { SeasonCalendar, type CalendarEntry } from "@/components/calendar/season-calendar";
import { useCareerOverview, usePlayerCalendar, usePlayerFitness } from "@/lib/api/queries";
import { useSetResting } from "@/lib/api/manage";

export function CalendarView({ id }: { id: string }) {
  const query = usePlayerCalendar(id);
  const world = useCareerOverview(id).data?.world ?? null;
  const gate = useSpokeGate(query);
  if (gate) return gate;

  const entries = [...((query.data?.playerCalendar as CalendarEntry[]) ?? [])].sort(
    (a, b) => a.week - b.week,
  );
  const playedCount = entries.filter((e) => e.played).length;

  return (
    <SpokeShell
      title="Schedule"
      description="Your season, month by month — Thursday to Sunday."
      action={
        <span className="border-border bg-surface text-muted-foreground rounded-full border px-3 py-1.5 text-xs font-semibold tabular-nums">
          {world ? `Season ${world.season} · ` : ""}
          {playedCount}/{entries.length} played
        </span>
      }
    >
      <AvailabilityBar id={id} />

      {entries.length === 0 ? (
        <SpokeEmpty>No events on the calendar yet.</SpokeEmpty>
      ) : (
        <SeasonCalendar id={id} entries={entries} currentWeek={world?.week ?? 1} />
      )}
    </SpokeShell>
  );
}

const AVAILABILITY_TONE: Record<string, { label: string; dot: string }> = {
  AVAILABLE: { label: "Available", dot: "bg-success" },
  RESTING: { label: "Resting", dot: "bg-info" },
  RECOVERING: { label: "Recovering", dot: "bg-gold" },
  INJURED: { label: "Injured", dot: "bg-destructive" },
};

/**
 * Season availability: the golfer's current status (fitness-derived) plus the season-long rest
 * decision. Resting is write-only on the backend, so the toggle reflects the last choice made this
 * session (defaults to playing).
 */
function AvailabilityBar({ id }: { id: string }) {
  const availability = usePlayerFitness(id).data?.playerFitness?.availability ?? null;
  const tone = availability ? AVAILABILITY_TONE[availability] : null;

  const [resting, setResting] = useState(false);
  const setRestingMutation = useSetResting(id);

  function choose(next: boolean) {
    if (setRestingMutation.isPending) return;
    setResting(next);
    setRestingMutation.mutate(next);
  }

  return (
    <div className="border-border bg-surface flex flex-wrap items-center justify-between gap-4 rounded-lg border px-5 py-4">
      <div className="flex flex-col gap-0.5">
        <span className="flex items-center gap-2 font-medium">
          {tone ? <span className={`size-2 rounded-full ${tone.dot}`} aria-hidden="true" /> : null}
          {tone ? tone.label : "Availability"}
        </span>
        <span className="text-muted-foreground text-sm">
          {resting ? "Resting — sitting out every event this season." : "Playing your schedule."}
        </span>
      </div>
      <Button
        variant="secondary"
        size="sm"
        onClick={() => choose(!resting)}
        disabled={setRestingMutation.isPending}
      >
        {resting ? "Resume playing" : "Rest this season"}
      </Button>
    </div>
  );
}
