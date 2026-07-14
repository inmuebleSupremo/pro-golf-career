"use client";

import { useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, Check } from "lucide-react";

import { Button } from "@/components/ui/button";
import { useCareerOverview } from "@/lib/api/queries";
import { isNotFound, isUnauthorized } from "@/lib/api/graphql-client";
import {
  eventPrestigeLabel,
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
};

export function CareerHub({ id }: { id: string }) {
  const router = useRouter();
  const { data, isPending, isError, error } = useCareerOverview(id);

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

  const { world, careerGoals, playerSchedule } = data;

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
      <div className="flex flex-col gap-3">
        <Link
          href="/saves"
          className="text-muted-foreground hover:text-foreground inline-flex w-fit items-center gap-1.5 text-sm transition-colors"
        >
          <ArrowLeft className="size-4" aria-hidden="true" />
          Saves
        </Link>
        <div className="flex flex-col gap-1">
          <h1 className="font-serif text-4xl font-medium tracking-[-0.02em]">
            Season {world.season}
          </h1>
          <p className="text-muted-foreground">
            Week {world.week} · {world.activePopulation} golfers active
          </p>
        </div>
      </div>

      <CareerGoals goals={careerGoals} />
      <UpcomingSchedule schedule={playerSchedule} />
    </div>
  );
}

function CareerGoals({ goals }: { goals: Goal[] }) {
  return (
    <section className="flex flex-col gap-4">
      <h2 className="font-serif text-xl font-medium">Career goals</h2>
      {goals.length === 0 ? (
        <EmptyNote>No goals set for this career.</EmptyNote>
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

function UpcomingSchedule({ schedule }: { schedule: ScheduleEntry[] }) {
  const upcoming = schedule.slice(0, 8);

  return (
    <section className="flex flex-col gap-4">
      <h2 className="font-serif text-xl font-medium">Upcoming</h2>
      {schedule.length === 0 ? (
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
  const isMajor = entry.prestige === "MAJOR";

  return (
    <li className="flex items-center justify-between gap-4 px-5 py-4">
      <div className="flex min-w-0 flex-col gap-1">
        <span className="font-medium">Week {entry.week}</span>
        <span className="text-muted-foreground text-sm">
          {tourTierLabel(entry.tier)} tour ·{" "}
          <span className={isMajor ? "text-accent font-medium" : undefined}>
            {eventPrestigeLabel(entry.prestige)}
          </span>
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
