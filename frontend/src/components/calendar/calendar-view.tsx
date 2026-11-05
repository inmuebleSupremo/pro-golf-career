"use client";

import { useEffect, useRef, useState } from "react";

import { Button } from "@/components/ui/button";
import { SpokeEmpty, useSpokeGate } from "@/components/career/spoke";
import { SeasonCalendar, type CalendarEntry } from "@/components/calendar/season-calendar";
import { useCareerOverview, usePinPlacementStatus, usePlayerCalendar, usePlayerFitness } from "@/lib/api/queries";
import { useAcknowledgeScheduleReview, useAdoptV5PinPlacement, useSetResting } from "@/lib/api/manage";

export function CalendarView({ id }: { id: string }) {
  const query = usePlayerCalendar(id);
  const world = useCareerOverview(id).data?.world ?? null;
  const { mutate: acknowledgeScheduleReview } = useAcknowledgeScheduleReview(id);
  const leaveTimer = useRef<number | null>(null);

  // A short delay cancels React Strict Mode's development-only setup/cleanup probe. The real route unmount
  // then records that the player had the chance to review the authoritative Schedule screen.
  useEffect(() => {
    if (leaveTimer.current != null) {
      window.clearTimeout(leaveTimer.current);
      leaveTimer.current = null;
    }
    if (!query.isSuccess) return;
    return () => {
      leaveTimer.current = window.setTimeout(() => acknowledgeScheduleReview(), 0);
    };
  }, [acknowledgeScheduleReview, query.isSuccess]);

  const gate = useSpokeGate(query);
  if (gate) return gate;

  const entries = [...((query.data?.playerCalendar as CalendarEntry[]) ?? [])].sort(
    (a, b) => a.week - b.week,
  );
  const playedCount = entries.filter((e) => e.played).length;

  return (
    <div className="flex flex-col gap-4">
      <AvailabilityBar id={id} />
      <PinPlacementBar id={id} />

      {entries.length === 0 ? (
        <SpokeEmpty>No events on the calendar yet.</SpokeEmpty>
      ) : (
        <SeasonCalendar
          id={id}
          entries={entries}
          currentWeek={world?.week ?? 1}
          season={world?.season ?? null}
          playedCount={playedCount}
        />
      )}
    </div>
  );
}

/** A narrow career-management affordance; it never changes an active or completed event. */
function PinPlacementBar({ id }: { id: string }) {
  const status = usePinPlacementStatus(id);
  const adopt = useAdoptV5PinPlacement(id);
  if (!status.data) return null;
  const policy = status.data.pinPlacementStatus;
  const legacy = policy.defaultVersion === "LEGACY_V1";
  return (
    <div className="border-border bg-surface flex flex-wrap items-center justify-between gap-4 rounded-lg border px-5 py-4">
      <div className="flex flex-col gap-0.5">
        <span className="font-medium">{legacy ? "Historical flag placement" : "Corrected flag placement"}</span>
        <span className="text-muted-foreground text-sm">
          {legacy
            ? `${policy.legacyScheduledEvents} future event${policy.legacyScheduledEvents === 1 ? "" : "s"} still use historical pins.`
            : "Future events use effective-green flag placement; completed events remain unchanged."}
        </span>
      </div>
      {legacy ? (
        <Button
          variant="secondary"
          size="sm"
          onClick={() => adopt.mutate()}
          disabled={!policy.canAdoptV5 || adopt.isPending}
        >
          {policy.canAdoptV5 ? "Adopt corrected pins" : "Finish current event first"}
        </Button>
      ) : null}
    </div>
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
