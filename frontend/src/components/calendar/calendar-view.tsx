"use client";

import { useState } from "react";
import { CalendarOff, CalendarPlus, Trophy } from "lucide-react";

import { Button } from "@/components/ui/button";
import { SpokeShell, SpokeEmpty, useSpokeGate } from "@/components/career/spoke";
import { useEnterEvent, useSetResting, useSkipEvent } from "@/lib/api/manage";
import { useCareerOverview, usePlayerCalendar, usePlayerFitness } from "@/lib/api/queries";
import {
  eventPrestigeLabel,
  formatMoney,
  formatScore,
  ordinalPosition,
  tourTierLabel,
} from "@/lib/career/labels";

type Finisher = { position: number; name: string; score: number; madeCut: boolean; earnings: number };
type EventResult = {
  winner: Finisher | null;
  topThree: Finisher[];
  playerFinish: Finisher | null;
};
type CalendarEntry = {
  tournamentId: string;
  week: number;
  tier: string;
  prestige: string;
  entered: boolean;
  name: string;
  played: boolean;
  result: EventResult | null;
};

/*
 * The season runs April → late October in real golf. The engine tracks events by week
 * only, so we lay them on a nominal calendar: week 1 is the first Thursday of April, and
 * each event spans Thursday–Sunday. This is presentation only — the engine is unchanged.
 */
const MONTH_NAMES = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
const SEASON_MONTHS = [3, 4, 5, 6, 7, 8, 9]; // April (3) … October (9)

function eventDates(week: number): { start: Date; end: Date } {
  const startDay = 3 + (week - 1) * 7; // 3 = first Thursday of April in the nominal year
  return { start: new Date(2025, 3, startDay), end: new Date(2025, 3, startDay + 3) };
}

/** The month bucket for an event (clamped to the April–October season window). */
function eventMonth(week: number): number {
  return Math.min(9, Math.max(3, eventDates(week).start.getMonth()));
}

/** "Apr 3–6" within a month, or "Apr 30 – May 3" across one. */
function formatDateRange(week: number): string {
  const { start, end } = eventDates(week);
  const sM = MONTH_NAMES[start.getMonth()];
  if (start.getMonth() === end.getMonth()) return `${sM} ${start.getDate()}–${end.getDate()}`;
  return `${sM} ${start.getDate()} – ${MONTH_NAMES[end.getMonth()]} ${end.getDate()}`;
}

export function CalendarView({ id }: { id: string }) {
  const query = usePlayerCalendar(id);
  const world = useCareerOverview(id).data?.world ?? null;
  const gate = useSpokeGate(query);
  if (gate) return gate;

  const entries = [...((query.data?.playerCalendar as CalendarEntry[]) ?? [])].sort(
    (a, b) => a.week - b.week,
  );
  const currentWeek = world?.week ?? 1;
  const playedCount = entries.filter((e) => e.played).length;
  const nextUpId = entries.find((e) => !e.played && e.entered)?.tournamentId ?? null;

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
        <div className="flex flex-col gap-5">
          {SEASON_MONTHS.map((month) => (
            <MonthSection
              key={month}
              month={month}
              entries={entries.filter((e) => eventMonth(e.week) === month)}
              currentWeek={currentWeek}
              nextUpId={nextUpId}
              id={id}
            />
          ))}
        </div>
      )}
    </SpokeShell>
  );
}

function MonthSection({
  month,
  entries,
  currentWeek,
  nextUpId,
  id,
}: {
  month: number;
  entries: CalendarEntry[];
  currentWeek: number;
  nextUpId: string | null;
  id: string;
}) {
  return (
    <section className="flex flex-col gap-2">
      <h2 className="text-subtle-foreground text-xs font-bold tracking-[0.12em] uppercase">
        {MONTH_NAMES[month]}
      </h2>
      {entries.length === 0 ? (
        <p className="border-border text-subtle-foreground rounded-lg border border-dashed px-4 py-3 text-sm">
          No events
        </p>
      ) : (
        <ul className="divide-divider border-border bg-surface flex flex-col divide-y overflow-hidden rounded-lg border">
          {entries.map((entry) => (
            <EventRow
              key={entry.tournamentId}
              id={id}
              entry={entry}
              currentWeek={currentWeek}
              isNext={entry.tournamentId === nextUpId}
            />
          ))}
        </ul>
      )}
    </section>
  );
}

function EventRow({
  id,
  entry,
  currentWeek,
  isNext,
}: {
  id: string;
  entry: CalendarEntry;
  currentWeek: number;
  isNext: boolean;
}) {
  const marquee = entry.prestige === "MAJOR" || entry.prestige === "TOUR_CHAMPIONSHIP";
  const upcoming = !entry.played && entry.week >= currentWeek;

  return (
    <li className={`flex flex-col gap-3 px-5 py-4 ${isNext ? "bg-primary/[0.06]" : ""}`}>
      <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-2">
        <div className="flex min-w-0 flex-col gap-1">
          <span className="flex min-w-0 items-center gap-2">
            <span className={`truncate font-medium ${marquee ? "text-accent" : ""}`}>{entry.name}</span>
            {isNext ? (
              <span className="text-subtle-foreground shrink-0 text-xs tracking-[0.08em] uppercase">
                Next
              </span>
            ) : null}
          </span>
          <span className="text-muted-foreground text-sm">
            <span className="text-foreground font-medium">{formatDateRange(entry.week)}</span> ·{" "}
            {tourTierLabel(entry.tier)} tour · {eventPrestigeLabel(entry.prestige)}
          </span>
        </div>
        {upcoming ? (
          <EnterSkipButton id={id} entry={entry} />
        ) : (
          <StatusBadge entry={entry} />
        )}
      </div>

      {entry.played && entry.result ? <ResultDetail result={entry.result} /> : null}
    </li>
  );
}

function EnterSkipButton({ id, entry }: { id: string; entry: CalendarEntry }) {
  const skip = useSkipEvent(id);
  const enter = useEnterEvent(id);
  const pending = skip.isPending || enter.isPending;

  function onToggle() {
    if (entry.entered) skip.mutate(entry.tournamentId);
    else enter.mutate(entry.tournamentId);
  }

  return (
    <Button
      variant={entry.entered ? "secondary" : "primary"}
      size="sm"
      onClick={onToggle}
      disabled={pending}
      className="shrink-0"
      aria-label={entry.entered ? `Skip ${entry.name}` : `Enter ${entry.name}`}
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
  );
}

/** The right-aligned headline for a past event: the player's result once played, else the status. */
function StatusBadge({ entry }: { entry: CalendarEntry }) {
  if (entry.played) {
    const finish = entry.result?.playerFinish ?? null;
    if (!finish) return <span className="text-subtle-foreground shrink-0 text-sm">Did not play</span>;
    if (!finish.madeCut) return <span className="text-muted-foreground shrink-0 text-sm">Missed the cut</span>;
    return (
      <span className="shrink-0 text-sm font-medium">
        {ordinalPosition(finish.position)}
        <span className="text-muted-foreground font-normal">
          {" "}
          · <span className="font-mono tabular-nums">{formatScore(finish.score)}</span> ·{" "}
          <span className="font-mono tabular-nums">{formatMoney(finish.earnings)}</span>
        </span>
      </span>
    );
  }
  // Not played and in the past — the player skipped it.
  return <span className="text-subtle-foreground shrink-0 text-sm">Skipped</span>;
}

/** Winner and leading finishers for a played event. */
function ResultDetail({ result }: { result: EventResult }) {
  if (!result.winner) return null;
  return (
    <div className="border-divider flex flex-col gap-1.5 border-t pt-3">
      <div className="flex items-center gap-2 text-sm">
        <Trophy className="text-accent size-4 shrink-0" aria-hidden="true" />
        <span className="min-w-0 truncate font-medium">{result.winner.name}</span>
        <span className="text-subtle-foreground font-mono text-xs tabular-nums">
          {formatScore(result.winner.score)}
        </span>
      </div>
      {result.topThree.length > 1 ? (
        <ol className="text-muted-foreground flex flex-col gap-1 text-sm">
          {result.topThree.slice(1).map((f) => (
            <li key={f.position} className="flex items-center gap-2">
              <span className="text-subtle-foreground w-6 shrink-0 font-mono text-xs tabular-nums">
                {ordinalPosition(f.position)}
              </span>
              <span className="min-w-0 truncate">{f.name}</span>
              <span className="text-subtle-foreground font-mono text-xs tabular-nums">
                {formatScore(f.score)}
              </span>
            </li>
          ))}
        </ol>
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
 * Season availability: the golfer's current status (fitness-derived) plus the season-long
 * rest decision. Resting is write-only on the backend, so the toggle reflects the last choice
 * made this session (defaults to playing).
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
          {tone ? (
            <span className={`size-2 rounded-full ${tone.dot}`} aria-hidden="true" />
          ) : null}
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
