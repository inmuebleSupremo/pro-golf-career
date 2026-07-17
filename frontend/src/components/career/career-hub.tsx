"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, Check } from "lucide-react";

import { Button } from "@/components/ui/button";
import { GolferHeader } from "@/components/career/golfer-header";
import { GoalsEditor } from "@/components/career/goals-editor";
import { useCareerOverview, useHallOfFame, usePlayerProfile } from "@/lib/api/queries";
import { useAdvanceWeek } from "@/lib/api/play";
import { isNotFound, isUnauthorized } from "@/lib/api/graphql-client";
import {
  eventPrestigeLabel,
  formatMoney,
  goalLabel,
  goalProgressRatio,
  goalProgressText,
  isBooleanGoal,
  tourTierLabel,
} from "@/lib/career/labels";

type Goal = { type: string; target: string; current: string; achieved: boolean };
type ScheduleEntry = {
  tournamentId: string;
  week: number;
  tier: string;
  prestige: string;
  entered: boolean;
  name: string;
};
type NewsItem = {
  season: number;
  type: string;
  headline: string;
  prominence: number;
  subjectGolferId: string | null;
};
type SeasonStat = {
  season: number;
  events: number;
  wins: number;
  topTens: number;
  cuts: number;
  bestFinish: number;
  earnings: number;
};
type Induction = {
  golferId: string;
  name: string;
  season: number;
  score: number;
  careerWins: number;
};

export function CareerHub({ id }: { id: string }) {
  const router = useRouter();
  const { data, isPending, isError, error } = useCareerOverview(id);
  const profile = usePlayerProfile(id).data?.playerProfile ?? null;
  const hallOfFame = useHallOfFame(id).data?.hallOfFame ?? [];

  useEffect(() => {
    if (isError && isUnauthorized(error)) {
      router.push("/login");
      router.refresh();
    }
  }, [isError, error, router]);

  if (isPending) return <CareerHubSkeleton />;

  if (isError) {
    if (isUnauthorized(error)) return null;
    // An unknown session (e.g. after a restart) comes back NOT_FOUND — the ephemeral
    // session is gone; guide the player back to the durable save (design D1).
    if (isNotFound(error)) {
      return (
        <CareerHubMessage
          title="This session is no longer available"
          body="Loaded careers are held in memory and don't survive a restart. Reopen it from your saves."
        />
      );
    }
    return (
      <CareerHubMessage
        title="Couldn't load this career"
        body="Something went wrong. Head back to your saves and try again."
      />
    );
  }

  const { world, careerGoals, playerSchedule, newsFeed, playerSeasonStats } = data;
  const retired = profile?.retired ?? false;
  const inHallOfFame = profile != null && hallOfFame.some((i) => i.golferId === profile.golferId);

  // Defensive: the backend throws NOT_FOUND (handled above) rather than returning a
  // null world, but the field is nullable in the schema, so guard it.
  if (!world) {
    return (
      <CareerHubMessage
        title="This session is no longer available"
        body="Loaded careers are held in memory and don't survive a restart. Reopen it from your saves."
      />
    );
  }

  return (
    <div className="flex flex-col gap-10">
      <div className="flex flex-col gap-6">
        <Link
          href="/saves"
          className="text-muted-foreground hover:text-foreground inline-flex w-fit items-center gap-1.5 text-sm transition-colors"
        >
          <ArrowLeft className="size-4" aria-hidden="true" />
          Saves
        </Link>
        {profile ? <GolferHeader profile={profile} /> : null}
        {retired ? (
          <RetirementBanner profile={profile} inHallOfFame={inHallOfFame} />
        ) : (
          <div className="flex flex-wrap items-end justify-between gap-4">
            <p className="text-muted-foreground">
              Season {world.season} · Week {world.week} · {world.activePopulation} golfers active
            </p>
            <div className="flex items-center gap-3">
              <Button asChild variant="secondary" size="lg">
                <Link href={`/career/${id}/manage`}>Manage</Link>
              </Button>
              <AdvanceControl id={id} hasPendingEvent={world.hasPendingEvent} />
            </div>
          </div>
        )}
      </div>

      <CareerGoals id={id} goals={careerGoals} />
      <LatestNews news={newsFeed} playerGolferId={profile?.golferId ?? null} />
      <UpcomingSchedule id={id} schedule={playerSchedule} currentWeek={world.week} />
      <SeasonsTable seasons={playerSeasonStats} />
      <HallOfFameSection inductions={hallOfFame} playerGolferId={profile?.golferId ?? null} />
    </div>
  );
}

function AdvanceControl({ id, hasPendingEvent }: { id: string; hasPendingEvent: boolean }) {
  const router = useRouter();
  const advance = useAdvanceWeek(id);

  if (hasPendingEvent) {
    return (
      <Button asChild size="lg">
        <Link href={`/career/${id}/play`}>Play your event</Link>
      </Button>
    );
  }

  async function onAdvance() {
    // On reaching the player's tournament, go straight to it; otherwise the
    // career query is invalidated and the hub re-renders with the new week.
    const result = await advance.mutateAsync();
    if (result.advanceWeek.hasPendingEvent) {
      router.push(`/career/${id}/play`);
    }
  }

  return (
    <Button variant="secondary" size="lg" onClick={onAdvance} disabled={advance.isPending}>
      {advance.isPending ? "Advancing…" : "Advance week"}
    </Button>
  );
}

function RetirementBanner({
  profile,
  inHallOfFame,
}: {
  profile: { firstName: string; wins: number; careerEarnings: number } | null;
  inHallOfFame: boolean;
}) {
  if (!profile) return null;

  return (
    <div className="border-border bg-surface flex flex-col items-center gap-3 rounded-lg border p-8 text-center">
      <p className="text-subtle-foreground font-mono text-xs tracking-[0.18em] uppercase">
        Career complete
      </p>
      <h2 className="font-serif text-2xl font-medium tracking-[-0.01em]">
        {profile.firstName} has retired
      </h2>
      <p className="text-muted-foreground max-w-md text-sm">
        A career of {profile.wins} {profile.wins === 1 ? "win" : "wins"} and{" "}
        {formatMoney(profile.careerEarnings)} earned. The record stands below.
      </p>
      {inHallOfFame ? (
        <span className="bg-accent/[0.12] text-accent inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-sm font-medium">
          <Check className="size-3.5" aria-hidden="true" />
          Hall of Fame inductee
        </span>
      ) : null}
    </div>
  );
}

function CareerGoals({ id, goals }: { id: string; goals: Goal[] }) {
  const [editing, setEditing] = useState(false);

  return (
    <section className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-4">
        <h2 className="font-serif text-xl font-medium">Career goals</h2>
        {!editing ? (
          <Button variant="ghost" size="sm" onClick={() => setEditing(true)}>
            {goals.length === 0 ? "Set goals" : "Edit"}
          </Button>
        ) : null}
      </div>
      {editing ? (
        <GoalsEditor id={id} goals={goals} onDone={() => setEditing(false)} />
      ) : goals.length === 0 ? (
        <EmptyNote>No goals set yet — choose what this career is chasing.</EmptyNote>
      ) : (
        <ul className="divide-divider border-border bg-surface flex flex-col divide-y overflow-hidden rounded-lg border">
          {goals.map((goal, i) => (
            <GoalRow key={`${goal.type}-${i}`} goal={goal} />
          ))}
        </ul>
      )}
    </section>
  );
}

function GoalRow({ goal }: { goal: Goal }) {
  const targeted = !isBooleanGoal(goal.type);

  return (
    <li className="flex items-center justify-between gap-4 px-5 py-4">
      <div className="flex min-w-0 flex-1 flex-col gap-2">
        <span className="font-medium">{goalLabel(goal.type)}</span>
        {targeted ? (
          <div className="flex max-w-xs flex-col gap-1.5">
            <div className="bg-divider h-1.5 w-full overflow-hidden rounded-full">
              <div
                className="bg-primary h-full rounded-full"
                style={{ width: `${goalProgressRatio(goal.current, goal.target) * 100}%` }}
              />
            </div>
            <span className="text-subtle-foreground font-mono text-xs tabular-nums">
              {goalProgressText(goal.type, goal.current, goal.target)}
            </span>
          </div>
        ) : goal.achieved ? null : (
          <span className="text-muted-foreground text-sm">In progress</span>
        )}
      </div>
      {goal.achieved ? (
        <span className="bg-success/[0.12] text-success inline-flex shrink-0 items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium">
          <Check className="size-3.5" aria-hidden="true" />
          Achieved
        </span>
      ) : null}
    </li>
  );
}

function UpcomingSchedule({
  id,
  schedule,
  currentWeek,
}: {
  id: string;
  schedule: ScheduleEntry[];
  currentWeek: number;
}) {
  const upcoming = [...schedule]
    .filter((e) => e.week >= currentWeek)
    .sort((a, b) => a.week - b.week)
    .slice(0, 6);

  return (
    <section className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-4">
        <h2 className="font-serif text-xl font-medium">Upcoming</h2>
        <Button asChild variant="ghost" size="sm">
          <Link href={`/career/${id}/calendar`}>Full calendar</Link>
        </Button>
      </div>
      {upcoming.length === 0 ? (
        <EmptyNote>No upcoming events on the calendar.</EmptyNote>
      ) : (
        <ul className="divide-divider border-border bg-surface flex flex-col divide-y overflow-hidden rounded-lg border">
          {upcoming.map((entry) => (
            <ScheduleRow key={entry.tournamentId} entry={entry} />
          ))}
        </ul>
      )}
    </section>
  );
}

function ScheduleRow({ entry }: { entry: ScheduleEntry }) {
  const marquee = entry.prestige === "MAJOR" || entry.prestige === "TOUR_CHAMPIONSHIP";

  return (
    <li className="flex items-center justify-between gap-4 px-5 py-4">
      <div className="flex min-w-0 flex-col gap-1">
        <span className={`truncate font-medium ${marquee ? "text-accent" : ""}`}>{entry.name}</span>
        <span className="text-muted-foreground text-sm">
          Week {entry.week} · {tourTierLabel(entry.tier)} tour · {eventPrestigeLabel(entry.prestige)}
        </span>
      </div>
      <span
        className={`shrink-0 text-sm ${entry.entered ? "text-foreground" : "text-subtle-foreground"}`}
      >
        {entry.entered ? "Entered" : "Skipped"}
      </span>
    </li>
  );
}

function HallOfFameSection({
  inductions,
  playerGolferId,
}: {
  inductions: Induction[];
  playerGolferId: string | null;
}) {
  if (inductions.length === 0) return null;

  return (
    <section className="flex flex-col gap-4">
      <h2 className="font-serif text-xl font-medium">Hall of Fame</h2>
      <ul className="divide-divider border-border bg-surface flex flex-col divide-y overflow-hidden rounded-lg border">
        {inductions.map((induction) => (
          <InductionRow
            key={induction.golferId}
            induction={induction}
            isPlayer={playerGolferId != null && induction.golferId === playerGolferId}
          />
        ))}
      </ul>
    </section>
  );
}

function InductionRow({ induction, isPlayer }: { induction: Induction; isPlayer: boolean }) {
  return (
    <li className={`flex items-center justify-between gap-4 px-5 py-4 ${isPlayer ? "bg-primary/[0.06]" : ""}`}>
      <div className="flex min-w-0 flex-col gap-1">
        <span className="flex items-center gap-2">
          <span className={`truncate font-medium ${isPlayer ? "text-primary" : ""}`}>{induction.name}</span>
          {isPlayer ? <span className="text-subtle-foreground text-xs">(you)</span> : null}
        </span>
        <span className="text-muted-foreground text-sm">
          Inducted Season {induction.season} · {induction.careerWins}{" "}
          {induction.careerWins === 1 ? "win" : "wins"}
        </span>
      </div>
      <span className="text-subtle-foreground shrink-0 font-mono text-sm tabular-nums">
        {Math.round(induction.score)}
      </span>
    </li>
  );
}

function SeasonsTable({ seasons }: { seasons: SeasonStat[] }) {
  if (seasons.length === 0) return null;
  const rows = [...seasons].reverse(); // most recent season first

  return (
    <section className="flex flex-col gap-4">
      <h2 className="font-serif text-xl font-medium">Seasons</h2>
      <div className="border-border bg-surface overflow-x-auto rounded-lg border">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-subtle-foreground border-divider border-b text-xs tracking-[0.08em] uppercase">
              <th className="px-4 py-3 text-left font-medium">Season</th>
              <NumHead>Events</NumHead>
              <NumHead>Wins</NumHead>
              <NumHead>Top 10s</NumHead>
              <NumHead>Cuts</NumHead>
              <NumHead>Best</NumHead>
              <NumHead>Earnings</NumHead>
            </tr>
          </thead>
          <tbody className="divide-divider divide-y">
            {rows.map((s) => (
              <tr key={s.season}>
                <td className="px-4 py-3 font-medium">Season {s.season}</td>
                <NumCell>{s.events}</NumCell>
                <NumCell>{s.wins}</NumCell>
                <NumCell>{s.topTens}</NumCell>
                <NumCell>{s.cuts}</NumCell>
                <NumCell>{ordinalFinish(s.bestFinish)}</NumCell>
                <NumCell>{formatMoney(s.earnings)}</NumCell>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function NumHead({ children }: { children: React.ReactNode }) {
  return <th className="px-4 py-3 text-right font-medium">{children}</th>;
}

function NumCell({ children }: { children: React.ReactNode }) {
  return <td className="text-foreground px-4 py-3 text-right font-mono tabular-nums">{children}</td>;
}

/** A finishing position as an ordinal (1 → "1st"); a win reads better than a bare "1". */
function ordinalFinish(position: number): string {
  const suffix =
    position % 100 >= 11 && position % 100 <= 13
      ? "th"
      : (["th", "st", "nd", "rd"][position % 10] ?? "th");
  return `${position}${suffix}`;
}

function LatestNews({
  news,
  playerGolferId,
}: {
  news: NewsItem[];
  playerGolferId: string | null;
}) {
  if (news.length === 0) return null;

  return (
    <section className="flex flex-col gap-4">
      <h2 className="font-serif text-xl font-medium">Latest</h2>
      <ul className="divide-divider border-border bg-surface flex flex-col divide-y overflow-hidden rounded-lg border">
        {news.map((item, i) => (
          <NewsRow
            key={`${item.season}-${i}`}
            item={item}
            isPlayer={playerGolferId != null && item.subjectGolferId === playerGolferId}
          />
        ))}
      </ul>
    </section>
  );
}

function NewsRow({ item, isPlayer }: { item: NewsItem; isPlayer: boolean }) {
  return (
    <li className={`flex items-center justify-between gap-4 px-5 py-3 ${isPlayer ? "bg-primary/[0.06]" : ""}`}>
      <span className="flex min-w-0 items-center gap-2 text-sm">
        {isPlayer ? <span className="bg-primary size-1.5 shrink-0 rounded-full" aria-hidden="true" /> : null}
        <span className={`truncate ${isPlayer ? "text-foreground font-medium" : "text-foreground"}`}>
          {item.headline}
        </span>
      </span>
      <span className="text-subtle-foreground shrink-0 font-mono text-xs tabular-nums">S{item.season}</span>
    </li>
  );
}

function EmptyNote({ children }: { children: React.ReactNode }) {
  return (
    <p className="border-border bg-surface text-muted-foreground rounded-lg border border-dashed px-5 py-8 text-center text-sm">
      {children}
    </p>
  );
}

function CareerHubMessage({ title, body }: { title: string; body: string }) {
  return (
    <div className="flex flex-col items-center gap-4 py-16 text-center">
      <div className="flex flex-col gap-2">
        <p className="text-foreground font-serif text-xl">{title}</p>
        <p className="text-muted-foreground max-w-sm text-sm">{body}</p>
      </div>
      <Button asChild>
        <Link href="/saves">Back to saves</Link>
      </Button>
    </div>
  );
}

function CareerHubSkeleton() {
  return (
    <div aria-hidden="true" className="flex flex-col gap-10">
      <div className="flex flex-col gap-3">
        <div className="bg-divider h-4 w-16 animate-pulse rounded" />
        <div className="bg-divider h-9 w-48 animate-pulse rounded" />
      </div>
      <div className="border-border bg-surface h-40 animate-pulse rounded-lg border" />
      <div className="border-border bg-surface h-40 animate-pulse rounded-lg border" />
    </div>
  );
}
