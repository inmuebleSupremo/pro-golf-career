"use client";

import { useEffect, useState } from "react";
import { CalendarOff, CalendarPlus } from "lucide-react";

import { Button } from "@/components/ui/button";
import { isNotFound, isUnauthorized } from "@/lib/api/graphql-client";
import { useEnterEvent, usePlayerSchedule, useSetResting, useSkipEvent } from "@/lib/api/manage";
import { eventPrestigeLabel, tourTierLabel } from "@/lib/career/labels";

type ScheduleEntry = {
  tournamentId: string;
  week: number;
  tier: string;
  prestige: string;
  entered: boolean;
  name: string;
};

/**
 * Schedule & availability: toggle individual events on/off and rest the golfer for
 * the season. Entered state is read from the schedule; the rest flag is write-only,
 * so it is tracked locally (see useSetResting).
 */
export function ScheduleSection({ id, onUnauthorized }: { id: string; onUnauthorized: () => void }) {
  const { data, isPending, isError, error } = usePlayerSchedule(id);

  useEffect(() => {
    if (isError && isUnauthorized(error)) onUnauthorized();
  }, [isError, error, onUnauthorized]);

  return (
    <section className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <h2 className="font-serif text-xl font-medium">Schedule &amp; availability</h2>
        <p className="text-muted-foreground text-sm">
          Choose which events to enter, or rest your golfer for the season.
        </p>
      </div>

      <RestControl id={id} />

      {isPending ? (
        <SectionSkeleton />
      ) : isError ? (
        <SectionNote>
          {isNotFound(error)
            ? "This session is no longer available. Reopen the career from your saves."
            : "Couldn't load your schedule. Try again."}
        </SectionNote>
      ) : data.playerSchedule.length === 0 ? (
        <SectionNote>No upcoming events on the calendar.</SectionNote>
      ) : (
        <ul className="divide-divider border-border bg-surface flex flex-col divide-y overflow-hidden rounded-lg border">
          {data.playerSchedule.map((entry) => (
            <ScheduleRow key={entry.tournamentId} id={id} entry={entry} />
          ))}
        </ul>
      )}
    </section>
  );
}

function ScheduleRow({ id, entry }: { id: string; entry: ScheduleEntry }) {
  const skip = useSkipEvent(id);
  const enter = useEnterEvent(id);
  const isMajor = entry.prestige === "MAJOR";
  const pending = skip.isPending || enter.isPending;

  function onToggle() {
    if (entry.entered) skip.mutate(entry.tournamentId);
    else enter.mutate(entry.tournamentId);
  }

  return (
    <li className="flex items-center justify-between gap-4 px-5 py-4">
      <div className="flex min-w-0 flex-col gap-1">
        <span className={`truncate font-medium ${isMajor ? "text-accent" : ""}`}>{entry.name}</span>
        <span className="text-muted-foreground text-sm">
          Week {entry.week} · {tourTierLabel(entry.tier)} tour ·{" "}
          <span className={isMajor ? "text-accent font-medium" : undefined}>
            {eventPrestigeLabel(entry.prestige)}
          </span>
        </span>
      </div>
      <Button
        variant={entry.entered ? "secondary" : "primary"}
        size="sm"
        onClick={onToggle}
        disabled={pending}
        aria-label={entry.entered ? `Skip week ${entry.week}` : `Enter week ${entry.week}`}
      >
        {entry.entered ? (
          <>
            <CalendarOff className="size-4" aria-hidden="true" />
            Skip
          </>
        ) : (
          <>
            <CalendarPlus className="size-4" aria-hidden="true" />
            Enter
          </>
        )}
      </Button>
    </li>
  );
}

function RestControl({ id }: { id: string }) {
  // Write-only on the backend, so the toggle reflects the last choice made this
  // session rather than a persisted value (defaults to playing).
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
        <span className="font-medium">Season availability</span>
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

function SectionNote({ children }: { children: React.ReactNode }) {
  return (
    <p className="border-border bg-surface text-muted-foreground rounded-lg border border-dashed px-5 py-8 text-center text-sm">
      {children}
    </p>
  );
}

function SectionSkeleton() {
  return <div aria-hidden="true" className="border-border bg-surface h-48 animate-pulse rounded-lg border" />;
}
