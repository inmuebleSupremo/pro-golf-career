"use client";

import { useEffect, useRef, useState } from "react";
import { AnimatePresence, motion, useReducedMotion } from "motion/react";
import { Check, ChevronLeft, ChevronRight, Plus, Trophy } from "lucide-react";

import { Button } from "@/components/ui/button";
import { useEnterEvent, useSkipEvent } from "@/lib/api/manage";
import { eventPrestigeLabel, formatScore, ordinalPosition } from "@/lib/career/labels";

export type Finisher = { position: number; name: string; score: number; madeCut: boolean; earnings: number };
export type EventResult = {
  winner: Finisher | null;
  topThree: Finisher[];
  playerFinish: Finisher | null;
};
export type CalendarEntry = {
  tournamentId: string;
  week: number;
  tier: string;
  prestige: string;
  entered: boolean;
  name: string;
  location: string;
  played: boolean;
  result: EventResult | null;
};

/*
 * A real month-grid calendar for the season. The engine tracks events by week only, so we lay them
 * on a nominal calendar: week 1 is the first Thursday of April, each event spans Thursday–Sunday. A
 * Monday-first week keeps that Thu–Sun run contiguous in one row. Months flip April → October and the
 * grid is a fixed six rows so the height never changes (zero CLS). The view snaps to the month the
 * game is currently in and resyncs as the weeks advance.
 */
const MONTH_FULL = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
const WEEKDAYS = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
const FIRST_MONTH = 3; // April
const LAST_MONTH = 9; // October
const YEAR = 2025;
const CELLS = 42; // 6 weeks × 7 days — fixed for a stable height

function eventThursday(week: number): Date {
  return new Date(YEAR, 3, 3 + (week - 1) * 7);
}

/** Monday-first weekday index (Mon = 0 … Sun = 6). */
function mondayIndex(date: Date): number {
  return (date.getDay() + 6) % 7;
}

function clampMonth(month: number): number {
  return Math.min(LAST_MONTH, Math.max(FIRST_MONTH, month));
}

export function SeasonCalendar({
  id,
  entries,
  currentWeek,
  season,
  playedCount,
}: {
  id: string;
  entries: CalendarEntry[];
  currentWeek: number;
  season: number | null;
  playedCount: number;
}) {
  // Anchor on the month the game is currently in.
  const currentMonth = clampMonth(eventThursday(currentWeek).getMonth());
  const [month, setMonth] = useState(currentMonth);
  const [direction, setDirection] = useState(0);
  const reduce = useReducedMotion();

  // Snap to the current month whenever the game clock crosses into a new one (e.g. advancing weeks
  // from the identity strip while this page is open, or `world` resolving after first paint).
  const trackedMonth = useRef(currentMonth);
  useEffect(() => {
    if (trackedMonth.current !== currentMonth) {
      setDirection(currentMonth >= trackedMonth.current ? 1 : -1);
      trackedMonth.current = currentMonth;
      setMonth(currentMonth);
    }
  }, [currentMonth]);

  function go(delta: number) {
    setDirection(delta);
    setMonth((m) => clampMonth(m + delta));
  }

  const nextUpId =
    (entries.filter((e) => !e.played && e.entered).sort((a, b) => a.week - b.week)[0] ??
      entries.filter((e) => !e.played).sort((a, b) => a.week - b.week)[0])?.tournamentId ?? null;

  return (
    <div className="border-border from-surface-elevated to-surface flex flex-col gap-3 rounded-xl border bg-gradient-to-b p-4 shadow-[var(--shadow-md)] md:p-5">
      <div className="flex items-center justify-between gap-3">
        <div className="flex flex-wrap items-baseline gap-x-3 gap-y-0.5">
          <h2 className="text-lg font-bold tracking-[-0.02em]">{MONTH_FULL[month]}</h2>
          <span className="text-muted-foreground text-sm tabular-nums">
            {season != null ? `Season ${season} · ` : ""}
            {playedCount}/{entries.length} played
          </span>
        </div>
        <div className="flex items-center gap-1">
          <Button variant="ghost" size="icon" onClick={() => go(-1)} disabled={month <= FIRST_MONTH} aria-label="Previous month">
            <ChevronLeft className="size-4" aria-hidden="true" />
          </Button>
          <Button variant="ghost" size="icon" onClick={() => go(1)} disabled={month >= LAST_MONTH} aria-label="Next month">
            <ChevronRight className="size-4" aria-hidden="true" />
          </Button>
        </div>
      </div>

      <div className="overflow-x-auto">
        <div className="min-w-[44rem]">
          <div className="grid grid-cols-7">
            {WEEKDAYS.map((d) => (
              <div key={d} className="text-subtle-foreground px-2 pb-2 text-center text-[0.6875rem] font-semibold tracking-[0.08em] uppercase">
                {d}
              </div>
            ))}
          </div>

          <div className="border-border overflow-hidden rounded-lg border-t border-l">
            <AnimatePresence mode="wait" initial={false} custom={direction}>
              <motion.div
                key={month}
                initial={reduce ? { opacity: 0 } : { opacity: 0, x: direction * 28 }}
                animate={{ opacity: 1, x: 0 }}
                exit={reduce ? { opacity: 0 } : { opacity: 0, x: direction * -28 }}
                transition={{ duration: 0.22, ease: "easeOut" }}
              >
                <MonthGrid id={id} month={month} entries={entries} currentWeek={currentWeek} nextUpId={nextUpId} />
              </motion.div>
            </AnimatePresence>
          </div>
        </div>
      </div>

      <Legend />
    </div>
  );
}

function MonthGrid({
  id,
  month,
  entries,
  currentWeek,
  nextUpId,
}: {
  id: string;
  month: number;
  entries: CalendarEntry[];
  currentWeek: number;
  nextUpId: string | null;
}) {
  const firstDow = mondayIndex(new Date(YEAR, month, 1));
  const daysInMonth = new Date(YEAR, month + 1, 0).getDate();
  const days: (number | null)[] = Array.from({ length: CELLS }, (_, i) => {
    const day = i - firstDow + 1;
    return day >= 1 && day <= daysInMonth ? day : null;
  });

  // Place each event by the cell index of its Thursday, spanning Thu–Sun.
  const placed = entries
    .map((entry) => {
      const thu = eventThursday(entry.week);
      if (thu.getFullYear() !== YEAR || thu.getMonth() !== month) return null;
      const startIndex = firstDow + thu.getDate() - 1;
      const startCol = startIndex % 7;
      return { entry, startIndex, weekRow: Math.floor(startIndex / 7), startCol, span: Math.min(4, 7 - startCol) };
    })
    .filter((p): p is { entry: CalendarEntry; startIndex: number; weekRow: number; startCol: number; span: number } => p !== null);

  // Cells an event covers get no day number — the event owns that space.
  const covered = new Set<number>();
  for (const p of placed) for (let i = 0; i < p.span; i++) covered.add(p.startIndex + i);

  // The Thu–Sun cells of the week the world is currently in — a faint "you are here".
  const currentThu = eventThursday(currentWeek);
  const currentStart =
    currentThu.getFullYear() === YEAR && currentThu.getMonth() === month ? firstDow + currentThu.getDate() - 1 : -1;

  return (
    <div>
      {[0, 1, 2, 3, 4, 5].map((row) => (
        <div key={row} className="relative grid grid-cols-7">
          {days.slice(row * 7, row * 7 + 7).map((day, col) => {
            const index = row * 7 + col;
            const inCurrentWeek = currentStart >= 0 && index >= currentStart && index < currentStart + 4;
            return (
              <div
                key={col}
                className={`border-border min-h-[6rem] border-r border-b p-1.5 ${
                  day === null ? "bg-surface/40" : inCurrentWeek && !covered.has(index) ? "bg-primary/[0.05]" : ""
                }`}
              >
                {day !== null && !covered.has(index) ? (
                  <span className="text-subtle-foreground text-xs font-medium tabular-nums">{day}</span>
                ) : null}
              </div>
            );
          })}

          {placed
            .filter((p) => p.weekRow === row)
            .map((p) => (
              <div
                key={p.entry.tournamentId}
                className="pointer-events-none absolute inset-y-0 p-1"
                style={{ left: `${(p.startCol / 7) * 100}%`, width: `${(p.span / 7) * 100}%` }}
              >
                <EventChip id={id} entry={p.entry} currentWeek={currentWeek} isNext={p.entry.tournamentId === nextUpId} />
              </div>
            ))}
        </div>
      ))}
    </div>
  );
}

function EventChip({
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
  const skip = useSkipEvent(id);
  const enter = useEnterEvent(id);
  const marquee = entry.prestige === "MAJOR" || entry.prestige === "TOUR_CHAMPIONSHIP";
  const actionable = !entry.played && entry.week >= currentWeek;
  const pending = skip.isPending || enter.isPending;

  const ring = isNext ? "ring-1 ring-primary/50" : "";
  const nameClass = `truncate text-sm font-semibold leading-tight ${marquee ? "text-accent" : ""}`;

  if (actionable) {
    const tone = entry.entered
      ? "border-primary/40 bg-primary/[0.12] text-foreground"
      : "border-border border-dashed bg-surface text-muted-foreground";
    return (
      <button
        type="button"
        onClick={() => (entry.entered ? skip.mutate(entry.tournamentId) : enter.mutate(entry.tournamentId))}
        disabled={pending}
        aria-label={entry.entered ? `Skip ${entry.name}` : `Enter ${entry.name}`}
        className={`pointer-events-auto flex h-full w-full flex-col justify-between gap-1 rounded-md border px-2.5 py-2 text-left transition-colors hover:border-primary/60 disabled:opacity-60 ${tone} ${ring}`}
      >
        <span className="flex items-start gap-1.5">
          {entry.entered ? (
            <Check className="text-primary mt-0.5 size-3.5 shrink-0" aria-hidden="true" />
          ) : (
            <Plus className="mt-0.5 size-3.5 shrink-0" aria-hidden="true" />
          )}
          <span className="flex min-w-0 flex-col">
            <span className={nameClass}>{entry.name}</span>
            <span className="text-subtle-foreground truncate text-[0.7rem] leading-tight">{entry.location}</span>
          </span>
        </span>
        <span className="text-xs font-medium">
          {eventPrestigeLabel(entry.prestige)} · {entry.entered ? "Entered" : "Skipped"}
        </span>
      </button>
    );
  }

  // Past / resolved event — read-only, with the outcome written across the span.
  const won = entry.played && entry.result?.playerFinish?.position === 1;
  return (
    <div
      className={`pointer-events-auto flex h-full w-full flex-col rounded-md border px-2.5 py-2 ${
        entry.played ? "border-border bg-surface" : "border-border border-dashed bg-surface/60"
      } ${ring}`}
    >
      <span className="flex items-center gap-1.5">
        {won ? <Trophy className="text-gold size-3.5 shrink-0" aria-hidden="true" /> : null}
        <span className="flex min-w-0 flex-col">
          <span className={nameClass}>{entry.name}</span>
          <span className="text-subtle-foreground truncate text-[0.7rem] leading-tight">{entry.location}</span>
        </span>
      </span>
      <div className="flex flex-1 items-center justify-center">
        <ResultDisplay entry={entry} />
      </div>
    </div>
  );
}

function ResultDisplay({ entry }: { entry: CalendarEntry }) {
  if (!entry.played) {
    return <span className="text-subtle-foreground text-sm">Skipped</span>;
  }
  const finish = entry.result?.playerFinish ?? null;
  if (!finish) {
    return <span className="text-subtle-foreground text-sm">{entry.entered ? "Did not play" : "Skipped"}</span>;
  }
  if (!finish.madeCut) {
    return <span className="text-muted-foreground text-sm">Missed cut</span>;
  }
  return (
    <div className="flex items-baseline gap-2">
      <span className="text-lg font-bold tabular-nums">{ordinalPosition(finish.position)}</span>
      <span className="text-muted-foreground font-mono text-sm tabular-nums">{formatScore(finish.score)}</span>
    </div>
  );
}

function Legend() {
  return (
    <div className="text-subtle-foreground flex flex-wrap items-center gap-x-4 gap-y-1 px-1 text-[0.6875rem]">
      <LegendDot className="border-primary/40 bg-primary/[0.12]" label="Entered" />
      <LegendDot className="border-border border-dashed bg-surface" label="Skipped" />
      <span className="flex items-center gap-1.5">
        <span className="text-accent font-semibold">◆</span> Major / Championship
      </span>
      <span className="flex items-center gap-1.5">
        <Trophy className="text-gold size-3" aria-hidden="true" /> You won
      </span>
    </div>
  );
}

function LegendDot({ className, label }: { className: string; label: string }) {
  return (
    <span className="flex items-center gap-1.5">
      <span className={`size-3 rounded-[3px] border ${className}`} aria-hidden="true" />
      {label}
    </span>
  );
}
