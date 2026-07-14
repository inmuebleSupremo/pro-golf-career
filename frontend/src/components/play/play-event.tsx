"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Select } from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { Field } from "@/components/ui/field";
import { CLUBS, STRATEGIES, humanize } from "@/lib/play/options";
import { usePlayerProfile } from "@/lib/api/queries";
import { isNotFound, isUnauthorized } from "@/lib/api/graphql-client";
import {
  useCompleteEvent,
  usePlayShot,
  usePlayState,
  useSimEvent,
  useSimRound,
  useSimShot,
} from "@/lib/api/play";

type Situation = {
  holeNumber: number;
  par: number;
  shotNumber: number;
  strokesThisHole: number;
  distanceToPin: number;
  lie: string;
  pinLateral: number;
  minReach: number;
  maxReach: number;
};
type LeaderboardRow = {
  position: number;
  golfer: { id: string; name: string };
  score: number;
  roundsPlayed: number;
};
type Outcome = {
  finalSurface: string;
  distanceRemaining: number;
  hazardEntered: boolean;
  penaltyStrokes: number;
  strokes: number;
};

export function PlayEvent({ id }: { id: string }) {
  const router = useRouter();
  const { data, isPending, isError, error } = usePlayState(id);
  const playerGolferId = usePlayerProfile(id).data?.playerProfile?.golferId ?? null;

  const hasPendingEvent = data?.world?.hasPendingEvent ?? false;

  useEffect(() => {
    if (isError && isUnauthorized(error)) {
      router.push("/login");
      router.refresh();
    }
  }, [isError, error, router]);

  // No event to play (or it just finished) → back to the hub.
  useEffect(() => {
    if (data?.world && !data.world.hasPendingEvent) {
      router.replace(`/career/${id}`);
    }
  }, [data, id, router]);

  if (isPending) return <PlaySkeleton />;

  if (isError) {
    if (isUnauthorized(error)) return null;
    return (
      <PlayMessage
        id={id}
        title={
          isNotFound(error) ? "This session is no longer available" : "Couldn't load the event"
        }
        body={
          isNotFound(error)
            ? "Loaded careers don't survive a restart. Reopen it from your saves."
            : "Something went wrong. Head back to the career and try again."
        }
      />
    );
  }

  if (!data.world || !hasPendingEvent) return null; // redirecting to the hub

  const situation = data.currentSituation as Situation | null;
  const leaderboard = (data.eventLeaderboard as LeaderboardRow[]) ?? [];

  return (
    <div className="flex flex-col gap-8">
      <div className="flex items-center justify-between">
        <Link
          href={`/career/${id}`}
          className="text-muted-foreground hover:text-foreground inline-flex items-center gap-1.5 text-sm transition-colors"
        >
          <ArrowLeft className="size-4" aria-hidden="true" />
          Career
        </Link>
        <span className="text-subtle-foreground font-mono text-sm">
          Season {data.world.season} · Week {data.world.week}
        </span>
      </div>

      {situation ? (
        <div className="grid gap-8 lg:grid-cols-[1fr_20rem]">
          <div className="flex flex-col gap-6">
            <SituationPanel situation={situation} />
            <ShotDecision
              key={`${situation.holeNumber}-${situation.shotNumber}`}
              id={id}
              situation={situation}
            />
            <SimControls id={id} />
          </div>
          <Leaderboard rows={leaderboard} playerGolferId={playerGolferId} />
        </div>
      ) : (
        <EventComplete id={id} leaderboard={leaderboard} playerGolferId={playerGolferId} />
      )}
    </div>
  );
}

function SituationPanel({ situation }: { situation: Situation }) {
  return (
    <section className="border-border bg-surface rounded-lg border p-6">
      <div className="flex items-baseline justify-between">
        <h1 className="font-serif text-3xl font-medium tracking-[-0.01em]">
          Hole {situation.holeNumber}
        </h1>
        <span className="text-muted-foreground">Par {situation.par}</span>
      </div>
      <div className="mt-5 grid grid-cols-3 gap-4">
        <Stat label="Distance to pin" value={String(Math.round(situation.distanceToPin))} />
        <Stat label="Lie" value={humanize(situation.lie)} />
        <Stat label="Shot" value={`#${situation.shotNumber}`} />
      </div>
    </section>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-1">
      <span className="text-subtle-foreground text-xs tracking-[0.08em] uppercase">{label}</span>
      <span className="text-foreground font-mono text-lg tabular-nums">{value}</span>
    </div>
  );
}

function defaultClub(s: Situation): string {
  if (s.lie.toUpperCase().includes("GREEN")) return "PUTTER";
  if (s.distanceToPin >= 220) return "DRIVER";
  if (s.distanceToPin >= 60) return "IRON";
  return "WEDGE";
}

function clampTarget(s: Situation): number {
  return Math.round(Math.min(Math.max(s.distanceToPin, s.minReach), s.maxReach));
}

function ShotDecision({ id, situation }: { id: string; situation: Situation }) {
  const play = usePlayShot(id);
  const [club, setClub] = useState(() => defaultClub(situation));
  const [strategy, setStrategy] = useState("BALANCED");
  const [target, setTarget] = useState(() => clampTarget(situation));
  const [outcome, setOutcome] = useState<Outcome | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  const min = Math.floor(situation.minReach);
  const max = Math.ceil(situation.maxReach);

  async function onPlay() {
    setFormError(null);
    try {
      const result = await play.mutateAsync({ club, targetDistance: target, strategy });
      setOutcome(result.playShot);
    } catch {
      setFormError("Couldn't play that shot. Try again.");
    }
  }

  return (
    <section className="border-border bg-surface flex flex-col gap-5 rounded-lg border p-6">
      <h2 className="font-serif text-xl font-medium">Your shot</h2>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field id="club" label="Club">
          <Select value={club} onChange={(e) => setClub(e.target.value)}>
            {CLUBS.map((c) => (
              <option key={c.value} value={c.value}>
                {c.label}
              </option>
            ))}
          </Select>
        </Field>
        <Field id="strategy" label="Strategy">
          <Select value={strategy} onChange={(e) => setStrategy(e.target.value)}>
            {STRATEGIES.map((s) => (
              <option key={s.value} value={s.value}>
                {s.label}
              </option>
            ))}
          </Select>
        </Field>
      </div>
      <Field id="target" label={`Target distance (${min}–${max})`}>
        <Input
          type="number"
          min={min}
          max={max}
          value={target}
          onChange={(e) => setTarget(Number(e.target.value))}
          className="max-w-32"
        />
      </Field>
      {formError ? (
        <p role="alert" className="text-destructive text-sm">
          {formError}
        </p>
      ) : null}
      <Button size="lg" onClick={onPlay} disabled={play.isPending} className="w-fit">
        {play.isPending ? "Playing…" : "Play shot"}
      </Button>
      {outcome ? <OutcomeNote outcome={outcome} /> : null}
    </section>
  );
}

function OutcomeNote({ outcome }: { outcome: Outcome }) {
  return (
    <div className="border-border bg-background rounded-md border px-4 py-3 text-sm">
      <span className="font-medium">{humanize(outcome.finalSurface)}</span>
      <span className="text-muted-foreground">
        {" "}
        · {Math.round(outcome.distanceRemaining)} to the pin
      </span>
      {outcome.penaltyStrokes > 0 ? (
        <span className="text-destructive"> · +{outcome.penaltyStrokes} penalty</span>
      ) : null}
    </div>
  );
}

function SimControls({ id }: { id: string }) {
  const simShot = useSimShot(id);
  const simRound = useSimRound(id);
  const simEvent = useSimEvent(id);
  const busy = simShot.isPending || simRound.isPending || simEvent.isPending;

  return (
    <section className="flex flex-wrap items-center gap-3">
      <span className="text-muted-foreground text-sm">Skip ahead</span>
      <Button variant="secondary" size="sm" disabled={busy} onClick={() => simShot.mutate()}>
        Sim shot
      </Button>
      <Button variant="secondary" size="sm" disabled={busy} onClick={() => simRound.mutate()}>
        Sim round
      </Button>
      <Button variant="secondary" size="sm" disabled={busy} onClick={() => simEvent.mutate()}>
        Sim to end
      </Button>
    </section>
  );
}

function formatScore(score: number): string {
  if (score === 0) return "E";
  return score > 0 ? `+${score}` : `${score}`;
}

function Leaderboard({
  rows,
  playerGolferId,
}: {
  rows: LeaderboardRow[];
  playerGolferId: string | null;
}) {
  const top = rows.slice(0, 10);
  const playerRow = playerGolferId ? rows.find((r) => r.golfer.id === playerGolferId) : null;
  const showPlayerBelow = playerRow && !top.some((r) => r.golfer.id === playerGolferId);

  return (
    <aside className="flex flex-col gap-4">
      <h2 className="font-serif text-xl font-medium">Leaderboard</h2>
      {rows.length === 0 ? (
        <p className="border-border bg-surface text-muted-foreground rounded-lg border border-dashed px-5 py-8 text-center text-sm">
          No standings yet.
        </p>
      ) : (
        <ul className="divide-divider border-border bg-surface flex flex-col divide-y overflow-hidden rounded-lg border">
          {top.map((row) => (
            <LeaderboardRowItem
              key={row.golfer.id}
              row={row}
              isPlayer={row.golfer.id === playerGolferId}
            />
          ))}
          {showPlayerBelow ? (
            <>
              <li className="text-subtle-foreground px-4 py-1 text-center text-xs">···</li>
              <LeaderboardRowItem row={playerRow} isPlayer />
            </>
          ) : null}
        </ul>
      )}
    </aside>
  );
}

function LeaderboardRowItem({ row, isPlayer }: { row: LeaderboardRow; isPlayer: boolean }) {
  return (
    <li
      className={`flex items-center justify-between gap-3 px-4 py-2.5 ${isPlayer ? "bg-primary/[0.08]" : ""}`}
    >
      <span className="flex min-w-0 items-center gap-3">
        <span className="text-subtle-foreground w-5 text-right font-mono text-sm tabular-nums">
          {row.position}
        </span>
        <span className={`truncate text-sm ${isPlayer ? "text-foreground font-medium" : ""}`}>
          {row.golfer.name}
          {isPlayer ? " (you)" : ""}
        </span>
      </span>
      <span className="font-mono text-sm tabular-nums">{formatScore(row.score)}</span>
    </li>
  );
}

function EventComplete({
  id,
  leaderboard,
  playerGolferId,
}: {
  id: string;
  leaderboard: LeaderboardRow[];
  playerGolferId: string | null;
}) {
  const router = useRouter();
  const complete = useCompleteEvent(id);

  async function onFinish() {
    await complete.mutateAsync();
    router.push(`/career/${id}`);
    router.refresh();
  }

  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-col items-center gap-3 py-8 text-center">
        <p className="text-subtle-foreground font-mono text-xs tracking-[0.18em] uppercase">
          Event complete
        </p>
        <h1 className="font-serif text-3xl font-medium tracking-[-0.01em]">That&apos;s a wrap</h1>
        <p className="text-muted-foreground max-w-sm text-sm">
          Record the result and resume your season.
        </p>
        <Button size="lg" onClick={onFinish} disabled={complete.isPending}>
          {complete.isPending ? "Finishing…" : "Finish event"}
        </Button>
      </div>
      <Leaderboard rows={leaderboard} playerGolferId={playerGolferId} />
    </div>
  );
}

function PlayMessage({ id, title, body }: { id: string; title: string; body: string }) {
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

function PlaySkeleton() {
  return (
    <div aria-hidden="true" className="flex flex-col gap-8">
      <div className="bg-divider h-5 w-24 animate-pulse rounded" />
      <div className="grid gap-8 lg:grid-cols-[1fr_20rem]">
        <div className="border-border bg-surface h-64 animate-pulse rounded-lg border" />
        <div className="border-border bg-surface h-64 animate-pulse rounded-lg border" />
      </div>
    </div>
  );
}
