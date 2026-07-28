"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, Check, Flame, MapPin } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Select } from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { Field } from "@/components/ui/field";
import { CLUBS, STRATEGIES, humanize } from "@/lib/play/options";
import { sceneBackdrop, type SceneBackdrop } from "@/lib/play/scene";
import { usePlayerProfile } from "@/lib/api/queries";
import { isNotFound, isUnauthorized } from "@/lib/api/graphql-client";
import {
  useCompleteEvent,
  usePlayShot,
  usePlayState,
  useSimEvent,
  useSimHole,
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
type CurrentEvent = { name: string; location: string; courseType: string };
type HoleScore = { holeNumber: number; par: number; strokes: number };
type Scorecard = {
  roundNumber: number;
  currentHole: number;
  toPar: number;
  totalStrokes: number;
  holes: HoleScore[];
};

const ROUNDS_PER_EVENT = 4;
type Outcome = {
  finalSurface: string;
  carry: number;
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
  const event = (data.currentEvent as CurrentEvent | null) ?? null;
  const leaderboard = (data.eventLeaderboard as LeaderboardRow[]) ?? [];
  const scorecard = (data.playerScorecard as Scorecard | null) ?? null;
  const pressure = (data.playerPressure as number | null) ?? null;
  const backdrop = sceneBackdrop(event?.courseType, event?.name);
  const { season, week } = data.world;

  if (!situation) {
    return (
      <EventComplete
        id={id}
        event={event}
        backdrop={backdrop}
        season={season}
        week={week}
        leaderboard={leaderboard}
        playerGolferId={playerGolferId}
        madeCut={data.playerMadeCut ?? null}
      />
    );
  }

  return (
    <div className="flex flex-col gap-8">
      <EventHero id={id} event={event} backdrop={backdrop} season={season} week={week} />
      <div className="flex flex-col gap-6">
        {scorecard ? <RoundProgress scorecard={scorecard} /> : null}
        <PressureBanner pressure={pressure} round={scorecard?.roundNumber ?? null} />
        <div className="grid gap-8 lg:grid-cols-[1fr_20rem]">
          <div className="flex flex-col gap-6">
            {scorecard && scorecard.holes.length > 0 ? (
              <ScorecardStrip scorecard={scorecard} />
            ) : null}
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
      </div>
    </div>
  );
}

/**
 * The event's identity banner: the contextual course scene (parkland, links, desert, …) behind the event
 * name, place, and the season/week — with the Career back-link and a course-character chip. A scrim keeps
 * the text legible whatever the photograph. When the event context is missing it degrades to a plain header.
 */
function EventHero({
  id,
  event,
  backdrop,
  season,
  week,
}: {
  id: string;
  event: CurrentEvent | null;
  backdrop: SceneBackdrop;
  season: number;
  week: number;
}) {
  const back = (
    <Link
      href={`/career/${id}`}
      className="text-foreground/80 hover:text-foreground inline-flex items-center gap-1.5 text-sm transition-colors"
    >
      <ArrowLeft className="size-4" aria-hidden="true" />
      Career
    </Link>
  );
  const meta = (
    <span className="text-foreground/70 font-mono text-sm tabular-nums">
      Season {season} · Week {week}
    </span>
  );

  if (!event) {
    return (
      <div className="flex items-center justify-between">
        {back}
        {meta}
      </div>
    );
  }

  return (
    <section className="border-border relative isolate overflow-hidden rounded-xl border">
      <Image
        src={backdrop.src}
        alt=""
        fill
        priority
        sizes="(min-width: 1024px) 1500px, 100vw"
        className="object-cover object-center"
      />
      {/* Scrims: a strong bottom veil anchors the title, a gentle top veil keeps the controls legible, and a
          soft left veil steadies the title's edge — so text reads on any photo while the scene stays visible. */}
      <div className="from-background via-background/70 absolute inset-0 bg-gradient-to-t to-transparent" />
      <div className="from-background/75 absolute inset-x-0 top-0 h-24 bg-gradient-to-b to-transparent" />
      <div className="from-background/55 absolute inset-0 bg-gradient-to-r to-transparent" />
      <div className="relative flex min-h-44 flex-col justify-between gap-5 p-5 md:min-h-52 md:p-6">
        <div className="flex items-center justify-between gap-3">{back}</div>
        <div className="flex flex-wrap items-end justify-between gap-x-4 gap-y-2">
          <div className="flex min-w-0 flex-col gap-1.5">
            <h1 className="text-foreground font-serif text-3xl font-medium tracking-[-0.01em] md:text-4xl">
              {event.name}
            </h1>
            <p className="text-muted-foreground inline-flex items-center gap-1.5 text-sm">
              <MapPin className="size-3.5" aria-hidden="true" />
              {event.location}
            </p>
          </div>
          {meta}
        </div>
      </div>
    </section>
  );
}

/**
 * Situational-pressure indicator. The pressure model only bites on the closing rounds when the golfer is
 * in contention (0 otherwise), so this appears exactly when the round gets hard — teaching the player that
 * the difficulty is Sunday nerves, and that COMPOSURE (and a sports psychologist) is the counter. Escalates
 * amber → red with intensity; renders nothing when there's no pressure to feel.
 */
function PressureBanner({ pressure, round }: { pressure: number | null; round: number | null }) {
  if (pressure == null || pressure <= 0) return null;

  const intense = pressure >= 0.6;
  const label = intense ? "Intense pressure" : pressure >= 0.3 ? "High pressure" : "Pressure building";
  const roundContext = round === 4 ? "Final round" : round === 3 ? "Moving day" : "In contention";
  const tone = intense
    ? "border-destructive/30 bg-destructive/[0.08] text-destructive"
    : "border-warning/30 bg-warning/[0.08] text-warning";

  return (
    <div className={`flex items-center gap-3 rounded-lg border px-4 py-3 ${tone}`}>
      <Flame className="size-5 shrink-0" aria-hidden="true" />
      <div className="min-w-0">
        <p className="text-foreground text-sm font-semibold">
          {label} <span className="text-muted-foreground font-normal">· {roundContext}, in the hunt</span>
        </p>
        <p className="text-muted-foreground mt-0.5 text-xs">
          Nerves widen every shot now — Composure steadies your hands, and a sports psychologist blunts it.
        </p>
      </div>
    </div>
  );
}

function RoundProgress({ scorecard }: { scorecard: Scorecard }) {
  return (
    <div className="border-border bg-surface flex flex-wrap items-center justify-between gap-3 rounded-lg border px-5 py-3">
      <span className="text-sm font-medium">
        Round {scorecard.roundNumber} of {ROUNDS_PER_EVENT}
        <span className="text-muted-foreground font-normal"> · Hole {scorecard.currentHole} of 18</span>
      </span>
      <span className="text-subtle-foreground text-sm">
        Round{" "}
        <span className={`font-mono tabular-nums ${scoreToneClass(scorecard.toPar)}`}>
          {formatScore(scorecard.toPar)}
        </span>
      </span>
    </div>
  );
}

/** Colour for a score relative to par: under par reads as success, over par as destructive, level as neutral. */
function scoreToneClass(relToPar: number): string {
  if (relToPar < 0) return "text-success";
  if (relToPar > 0) return "text-destructive";
  return "text-foreground";
}

function ScorecardStrip({ scorecard }: { scorecard: Scorecard }) {
  return (
    <section className="border-border bg-surface overflow-x-auto rounded-lg border px-4 py-3">
      <div className="flex items-stretch gap-1">
        {scorecard.holes.map((hole) => (
          <HoleCell key={hole.holeNumber} hole={hole} />
        ))}
      </div>
    </section>
  );
}

function HoleCell({ hole }: { hole: HoleScore }) {
  const relToPar = hole.strokes - hole.par;
  return (
    <div className="flex min-w-8 flex-col items-center gap-1">
      <span className="text-subtle-foreground font-mono text-xs tabular-nums">{hole.holeNumber}</span>
      <span
        className={`flex size-7 items-center justify-center rounded font-mono text-sm tabular-nums ${
          relToPar < 0
            ? "bg-success/[0.12] text-success font-medium"
            : relToPar > 0
              ? "bg-destructive/[0.10] text-destructive"
              : "text-foreground"
        }`}
      >
        {hole.strokes}
      </span>
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
  const holed = outcome.distanceRemaining <= 0;
  return (
    <div className="border-border bg-background flex flex-wrap items-center gap-x-2 gap-y-1 rounded-md border px-4 py-3 text-sm">
      <span className="text-muted-foreground">
        Carried <span className="text-foreground font-mono tabular-nums">{Math.round(outcome.carry)}</span> yds to{" "}
      </span>
      <span className="font-medium">{humanize(outcome.finalSurface)}</span>
      {holed ? (
        <span className="text-success font-medium">· holed</span>
      ) : (
        <span className="text-muted-foreground">
          · <span className="text-foreground font-mono tabular-nums">{Math.round(outcome.distanceRemaining)}</span> to
          the pin
        </span>
      )}
      {outcome.hazardEntered ? (
        <span className="text-destructive font-medium">· hazard</span>
      ) : null}
      {outcome.penaltyStrokes > 0 ? (
        <span className="text-destructive"> · +{outcome.penaltyStrokes} penalty</span>
      ) : null}
    </div>
  );
}

function SimControls({ id }: { id: string }) {
  const simShot = useSimShot(id);
  const simHole = useSimHole(id);
  const simRound = useSimRound(id);
  const simEvent = useSimEvent(id);
  const busy = simShot.isPending || simHole.isPending || simRound.isPending || simEvent.isPending;

  return (
    <section className="flex flex-wrap items-center gap-3">
      <span className="text-muted-foreground text-sm">Skip ahead</span>
      <Button variant="secondary" size="sm" disabled={busy} onClick={() => simShot.mutate()}>
        Sim shot
      </Button>
      <Button variant="secondary" size="sm" disabled={busy} onClick={() => simHole.mutate()}>
        Sim hole
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
  embedded = false,
}: {
  rows: LeaderboardRow[];
  playerGolferId: string | null;
  /** When embedded in the translucent end-of-event scorecard, drop the solid card chrome so the scene shows through. */
  embedded?: boolean;
}) {
  const top = rows.slice(0, 10);
  const playerRow = playerGolferId ? rows.find((r) => r.golfer.id === playerGolferId) : null;
  const showPlayerBelow = playerRow && !top.some((r) => r.golfer.id === playerGolferId);
  const listClass = embedded
    ? "divide-border/50 flex flex-col divide-y"
    : "divide-divider border-border bg-surface flex flex-col divide-y overflow-hidden rounded-lg border";

  return (
    <aside className="flex flex-col gap-4">
      {embedded ? (
        <h2 className="text-subtle-foreground text-xs font-medium tracking-[0.12em] uppercase">
          Final standings
        </h2>
      ) : (
        <h2 className="font-serif text-xl font-medium">Leaderboard</h2>
      )}
      {rows.length === 0 ? (
        <p className="border-border text-muted-foreground rounded-lg border border-dashed px-5 py-8 text-center text-sm">
          No standings yet.
        </p>
      ) : (
        <ul className={listClass}>
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

/**
 * The end-of-event screen: the event's course scene fills the page, with the result and final standings
 * overlaid as one translucent scorecard so the photograph reads clearly through it (spec: play-event imagery).
 * A page-wide scrim keeps every figure legible whatever the scene. Bleeds past the content padding to the
 * shell edges for a full-frame finish, while leaving the persistent identity strip untouched.
 */
function EventComplete({
  id,
  event,
  backdrop,
  season,
  week,
  leaderboard,
  playerGolferId,
  madeCut,
}: {
  id: string;
  event: CurrentEvent | null;
  backdrop: SceneBackdrop;
  season: number;
  week: number;
  leaderboard: LeaderboardRow[];
  playerGolferId: string | null;
  madeCut: boolean | null;
}) {
  const router = useRouter();
  const complete = useCompleteEvent(id);
  const playerRow = playerGolferId ? leaderboard.find((r) => r.golfer.id === playerGolferId) : null;

  async function onFinish() {
    await complete.mutateAsync();
    router.push(`/career/${id}`);
    router.refresh();
  }

  return (
    <div className="relative isolate -mx-5 -my-6 min-h-[calc(100dvh-8rem)] overflow-hidden md:-mx-7 md:-my-7">
      <Image
        src={backdrop.src}
        alt=""
        fill
        priority
        sizes="100vw"
        className="object-cover object-center"
      />
      {/* Scrims: an even darken plus a bottom-weighted gradient keep the overlay legible over any scene. */}
      <div className="bg-background/45 absolute inset-0" />
      <div className="from-background via-background/40 absolute inset-0 bg-gradient-to-t to-transparent" />

      <div className="relative flex min-h-[calc(100dvh-8rem)] flex-col gap-8 px-5 py-6 md:px-7 md:py-7">
        <div className="flex items-center justify-between gap-3">
          <Link
            href={`/career/${id}`}
            className="text-foreground/80 hover:text-foreground inline-flex items-center gap-1.5 text-sm transition-colors"
          >
            <ArrowLeft className="size-4" aria-hidden="true" />
            Career
          </Link>
          <span className="text-foreground/70 font-mono text-sm tabular-nums">
            Season {season} · Week {week}
          </span>
        </div>

        <div className="flex flex-1 flex-col items-center justify-center gap-7 py-4">
          <div className="flex flex-col items-center gap-2 text-center">
            <p className="text-foreground/70 font-mono text-xs tracking-[0.18em] uppercase">
              Event complete
            </p>
            {event ? (
              <p className="text-foreground/90 inline-flex items-center gap-1.5 font-serif text-lg">
                {event.name}
              </p>
            ) : null}
          </div>

          {/* The scorecard: result + final standings on a translucent panel the scene shows through. */}
          <div className="border-border/60 bg-surface/50 w-full max-w-md rounded-xl border p-6 shadow-lg backdrop-blur-sm">
            <div className="flex flex-col items-center gap-2 text-center">
              {playerRow ? (
                <ResultSummary row={playerRow} madeCut={madeCut} />
              ) : (
                <h1 className="font-serif text-3xl font-medium tracking-[-0.01em]">
                  That&apos;s a wrap
                </h1>
              )}
            </div>
            <div className="border-border/50 mt-6 border-t pt-6">
              <Leaderboard rows={leaderboard} playerGolferId={playerGolferId} embedded />
            </div>
          </div>

          <Button size="lg" onClick={onFinish} disabled={complete.isPending}>
            {complete.isPending ? "Finishing…" : "Finish event"}
          </Button>
        </div>
      </div>
    </div>
  );
}

function ResultSummary({ row, madeCut }: { row: LeaderboardRow; madeCut: boolean | null }) {
  const missedCut = madeCut === false;
  return (
    <div className="flex flex-col items-center gap-2">
      <h1 className="font-serif text-3xl font-medium tracking-[-0.01em]">
        {missedCut ? "Missed the cut" : `Finished ${ordinal(row.position)}`}
      </h1>
      <p className="text-muted-foreground">
        <span className="text-foreground font-mono tabular-nums">{formatScore(row.score)}</span> for the event
      </p>
          {/* madeCut is only meaningful once the cut has been evaluated; null (early rounds) shows nothing. */}
      {madeCut === true ? (
        <span className="bg-success/[0.12] text-success mt-1 inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium">
          <Check className="size-3.5" aria-hidden="true" />
          Made the cut
        </span>
      ) : null}
    </div>
  );
}

/** A leaderboard position as an ordinal, e.g. 1 → "1st", 12 → "12th". */
function ordinal(position: number): string {
  const suffix =
    position % 100 >= 11 && position % 100 <= 13
      ? "th"
      : ["th", "st", "nd", "rd"][position % 10] ?? "th";
  return `${position}${suffix}`;
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
