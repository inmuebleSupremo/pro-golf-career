"use client";

import { useState } from "react";
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
  played: boolean;
  result: EventResult | null;
};

/*
 * A real month-grid calendar for the season. The engine tracks events by week only, so we lay them
 * on a nominal calendar: week 1 is the first Thursday of April, each event spans Thursday–Sunday. A
 * Monday-first week keeps that Thu–Sun run contiguous in one row. Months flip April → October; the
 * grid is a fixed six rows so the height never changes (zero CLS).
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
}: {
  id: string;
  entries: CalendarEntry[];
  currentWeek: number;
}) {
  const nextUp =
    entries.filter((e) => !e.played && e.entered).sort((a, b) => a.week - b.week)[0] ??
    entries.filter((e) => !e.played).sort((a, b) => a.week - b.week)[0];
  const defaultMonth = clampMonth(eventThursday(nextUp?.week ?? currentWeek).getMonth());

  const [month, setMonth] = useState(defaultMonth);
  const [direction, setDirection] = useState(0);
  const reduce = useReducedMotion();

  function go(delta: number) {
    setDirection(delta);
    setMonth((m) => clampMonth(m + delta));
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center justify-between gap-3">
        <Button
          variant="ghost"
          size="icon"
          onClick={() => go(-1)}
          disabled={month <= FIRST_MONTH}
          aria-label="Previous month"
        >
          <ChevronLeft className="size-4" aria-hidden="true" />
        </Button>
        <h2 className="text-base font-bold tracking-[-0.01em]">{MONTH_FULL[month]}</h2>
        <Button
          variant="ghost"
          size="icon"
          onClick={() => go(1)}
          disabled={month >= LAST_MONTH}
          aria-label="Next month"
        >
          <ChevronRight className="size-4" aria-hidden="true" />
        </Button>
      </div>

      <div className="overflow-x-auto">
        <div className="min-w-[44rem]">
          <div className="grid grid-cols-7">
            {WEEKDAYS.map((d) => (
              <div
                key={d}
                className="text-subtle-foreground px-2 pb-2 text-center text-[0.6875rem] font-semibold tracking-[0.08em] uppercase"
              >
                {d}
              </div>
            ))}
          </div>

          <div className="border-border overflow-hidden rounded-lg border-t border-l">
            <AnimatePresence mode="wait" initial={false} custom={direction}>
              <motion.div
                key={month}
                custom={direction}
                initial={reduce ? { opacity: 0 } : { opacity: 0, x: direction * 28 }}
                animate={{ opacity: 1, x: 0 }}
                exit={reduce ? { opacity: 0 } : { opacity: 0, x: direction * -28 }}
                transition={{ duration: 0.22, ease: "easeOut" }}
              >
                <MonthGrid id={id} month={month} entries={entries} currentWeek={currentWeek} nextUpId={nextUp?.tournamentId ?? null} />
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

  // Place each event by the cell index of its Thursday.
  const placed = entries
    .map((entry) => {
      const thu = eventThursday(entry.week);
      if (thu.getFullYear() !== YEAR || thu.getMonth() !== month) return null;
      const cellIndex = firstDow + thu.getDate() - 1;
      const startCol = cellIndex % 7;
      return { entry, weekRow: Math.floor(cellIndex / 7), startCol, span: Math.min(4, 7 - startCol) };
    })
    .filter((p): p is { entry: CalendarEntry; weekRow: number; startCol: number; span: number } => p !== null);

  // The Thu–Sun cells of the week the world is currently in — a faint "you are here".
  const currentThu = eventThursday(currentWeek);
  const currentStart =
    currentThu.getFullYear() === YEAR && currentThu.getMonth() === month
      ? firstDow + currentThu.getDate() - 1
      : -1;

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
                className={`border-border min-h-[5.5rem] border-r border-b p-1.5 ${
                  day === null ? "bg-surface/40" : inCurrentWeek ? "bg-primary/[0.05]" : ""
                }`}
              >
                {day !== null ? (
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
                className="pointer-events-none absolute px-1"
                style={{ top: "1.55rem", left: `${(p.startCol / 7) * 100}%`, width: `${(p.span / 7) * 100}%` }}
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
  const marqueeText = marquee ? "text-accent" : "";

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
        className={`pointer-events-auto flex w-full flex-col gap-0.5 rounded-md border px-2 py-1.5 text-left transition-colors hover:border-primary/60 disabled:opacity-60 ${tone} ${ring}`}
      >
        <span className="flex items-center gap-1">
          {entry.entered ? (
            <Check className="text-primary size-3 shrink-0" aria-hidden="true" />
          ) : (
            <Plus className="size-3 shrink-0" aria-hidden="true" />
          )}
          <span className={`truncate text-[0.72rem] font-semibold leading-tight ${marqueeText}`}>
            {entry.name}
          </span>
        </span>
        <span className="text-[0.625rem] leading-tight">
          {eventPrestigeLabel(entry.prestige)} · {entry.entered ? "Entered" : "Skipped"}
        </span>
      </button>
    );
  }

  // Past / resolved event — read-only, showing the outcome.
  return (
    <div
      className={`pointer-events-auto flex w-full flex-col gap-0.5 rounded-md border px-2 py-1.5 ${
        entry.played ? "border-border bg-surface" : "border-border border-dashed bg-surface/60"
      } ${ring}`}
    >
      <span className="flex items-center gap-1">
        {entry.played && entry.result?.playerFinish?.position === 1 ? (
          <Trophy className="text-gold size-3 shrink-0" aria-hidden="true" />
        ) : null}
        <span className={`truncate text-[0.72rem] font-semibold leading-tight ${marqueeText}`}>
          {entry.name}
        </span>
      </span>
      <span className="text-subtle-foreground text-[0.625rem] leading-tight">{resultText(entry)}</span>
    </div>
  );
}

function resultText(entry: CalendarEntry): string {
  if (!entry.played) return "Skipped";
  const finish = entry.result?.playerFinish ?? null;
  if (!finish) return entry.entered ? "Did not play" : "Skipped";
  if (!finish.madeCut) return "Missed cut";
  return `${ordinalPosition(finish.position)} · ${formatScore(finish.score)}`;
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
