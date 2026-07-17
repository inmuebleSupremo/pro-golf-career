"use client";

import { useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, Trophy } from "lucide-react";

import { Button } from "@/components/ui/button";
import { isNotFound, isUnauthorized } from "@/lib/api/graphql-client";
import { useCareerOverview, usePlayerCalendar } from "@/lib/api/queries";
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

export function CalendarView({ id }: { id: string }) {
  const router = useRouter();
  const { data, isPending, isError, error } = usePlayerCalendar(id);
  // Season/week context, already cached by the hub's overview query.
  const world = useCareerOverview(id).data?.world ?? null;

  useEffect(() => {
    if (isError && isUnauthorized(error)) {
      router.push("/login");
      router.refresh();
    }
  }, [isError, error, router]);

  if (isPending) return <CalendarSkeleton />;

  if (isError) {
    if (isUnauthorized(error)) return null;
    return (
      <CalendarMessage
        id={id}
        title={isNotFound(error) ? "This session is no longer available" : "Couldn't load the calendar"}
        body={
          isNotFound(error)
            ? "Loaded careers don't survive a restart. Reopen it from your saves."
            : "Something went wrong. Head back to the career and try again."
        }
      />
    );
  }

  const entries = [...(data.playerCalendar as CalendarEntry[])].sort((a, b) => a.week - b.week);
  // The event the player is heading into next — the first entered event still to come.
  const nextUpId = entries.find((e) => !e.played && e.entered)?.tournamentId ?? null;
  const playedCount = entries.filter((e) => e.played).length;

  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-col gap-3">
        <Link
          href={`/career/${id}`}
          className="text-muted-foreground hover:text-foreground inline-flex w-fit items-center gap-1.5 text-sm transition-colors"
        >
          <ArrowLeft className="size-4" aria-hidden="true" />
          Career
        </Link>
        <div className="flex flex-wrap items-end justify-between gap-4">
          <h1 className="font-serif text-3xl font-medium">Season calendar</h1>
          <p className="text-muted-foreground text-sm">
            {world ? `Season ${world.season} · Week ${world.week} · ` : ""}
            {playedCount} of {entries.length} played
          </p>
        </div>
      </div>

      {entries.length === 0 ? (
        <p className="border-border bg-surface text-muted-foreground rounded-lg border border-dashed px-5 py-8 text-center text-sm">
          No events on the calendar yet.
        </p>
      ) : (
        <ul className="divide-divider border-border bg-surface flex flex-col divide-y overflow-hidden rounded-lg border">
          {entries.map((entry) => (
            <CalendarRow
              key={entry.tournamentId}
              entry={entry}
              isNext={entry.tournamentId === nextUpId}
            />
          ))}
        </ul>
      )}
    </div>
  );
}

function CalendarRow({ entry, isNext }: { entry: CalendarEntry; isNext: boolean }) {
  const marquee = entry.prestige === "MAJOR" || entry.prestige === "TOUR_CHAMPIONSHIP";

  return (
    <li className={`flex flex-col gap-3 px-5 py-4 ${isNext ? "bg-primary/[0.06]" : ""}`}>
      <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
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
            Week {entry.week} · {tourTierLabel(entry.tier)} tour · {eventPrestigeLabel(entry.prestige)}
          </span>
        </div>
        <StatusBadge entry={entry} />
      </div>

      {entry.played && entry.result ? <ResultDetail result={entry.result} /> : null}
    </li>
  );
}

/** The right-aligned headline: the player's result once played, else the entry status. */
function StatusBadge({ entry }: { entry: CalendarEntry }) {
  if (entry.played) {
    const finish = entry.result?.playerFinish ?? null;
    if (!finish) {
      return <span className="text-subtle-foreground shrink-0 text-sm">Did not play</span>;
    }
    if (!finish.madeCut) {
      return <span className="text-muted-foreground shrink-0 text-sm">Missed the cut</span>;
    }
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
  return (
    <span className={`shrink-0 text-sm ${entry.entered ? "text-foreground" : "text-subtle-foreground"}`}>
      {entry.entered ? "Entered" : "Skipped"}
    </span>
  );
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

function CalendarMessage({ id, title, body }: { id: string; title: string; body: string }) {
  return (
    <div className="flex flex-col items-center gap-4 py-16 text-center">
      <div className="flex flex-col gap-2">
        <p className="text-foreground font-serif text-xl">{title}</p>
        <p className="text-muted-foreground max-w-sm text-sm">{body}</p>
      </div>
      <Button asChild>
        <Link href={`/career/${id}`}>Back to career</Link>
      </Button>
    </div>
  );
}

function CalendarSkeleton() {
  return (
    <div aria-hidden="true" className="flex flex-col gap-8">
      <div className="bg-divider h-9 w-56 animate-pulse rounded" />
      <div className="border-border bg-surface h-96 animate-pulse rounded-lg border" />
    </div>
  );
}
