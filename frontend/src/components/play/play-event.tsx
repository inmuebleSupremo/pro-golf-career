"use client";

import { useEffect } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, Check } from "lucide-react";

import { Button } from "@/components/ui/button";
import { applySceneFallback, sceneBackdrop, type SceneBackdrop } from "@/lib/play/scene";
import type { HoleGeom } from "@/lib/play/hole-geometry";
import { PlayMode } from "@/components/play/play-mode";
import {
  formatScore,
  Leaderboard,
  ordinal,
  type CurrentEvent,
  type LeaderboardRow,
  type Scorecard,
  type Situation,
} from "@/components/play/play-shared";
import { usePlayerProfile } from "@/lib/api/queries";
import { isNotFound, isUnauthorized } from "@/lib/api/graphql-client";
import { useCompleteEvent, usePlayingHole, usePlayState } from "@/lib/api/play";

export function PlayEvent({ id }: { id: string }) {
  const router = useRouter();
  const { data, isPending, isError, error } = usePlayState(id);
  const playerGolferId = usePlayerProfile(id).data?.playerProfile?.golferId ?? null;

  const hasPendingEvent = data?.world?.hasPendingEvent ?? false;
  const playingHole = (usePlayingHole(id).data?.playingHole ?? null) as HoleGeom | null;

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
        title={isNotFound(error) ? "This session is no longer available" : "Couldn't load the event"}
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
  const { season, week } = data.world;
  const activeRound =
    scorecard?.roundNumber ?? Math.max(1, ...leaderboard.map((row) => row.roundsPlayed));
  const backdrop = sceneBackdrop(event?.courseType, event?.name, { season, round: activeRound });

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
    <PlayMode
      id={id}
      event={event}
      situation={situation}
      hole={playingHole}
      scorecard={scorecard}
      leaderboard={leaderboard}
      pressure={pressure}
      playerGolferId={playerGolferId}
    />
  );
}

/**
 * The end-of-event screen: the event's course scene fills the page, with the result and final standings
 * overlaid as one translucent scorecard so the photograph reads clearly through it (spec: play-event imagery).
 * This stays within the command shell (the immersive play surface has exited by now).
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
    const result = await complete.mutateAsync();
    // Finishing the last event of the calendar year resumes the week across the season boundary — that
    // crossing is the off-season moment, so it takes priority over the hub (and over any week-1 event,
    // which is still pending once they begin the new season), mirroring the "advance week" flow.
    if (result.completeEvent.season > season) {
      router.push(`/career/${id}/offseason`);
      router.refresh();
      return;
    }
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
        onError={
          backdrop.fallbackSrc
            ? (event) => applySceneFallback(event.currentTarget, backdrop.fallbackSrc!)
            : undefined
        }
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
            <p className="text-foreground/70 font-mono text-xs tracking-[0.18em] uppercase">Event complete</p>
            {event ? (
              <p className="text-foreground/90 inline-flex items-center gap-1.5 font-serif text-lg">{event.name}</p>
            ) : null}
          </div>

          {/* The scorecard: result + final standings on a translucent panel the scene shows through. */}
          <div className="border-border/60 bg-surface/50 w-full max-w-md rounded-xl border p-6 shadow-lg backdrop-blur-sm">
            <div className="flex flex-col items-center gap-2 text-center">
              {playerRow ? (
                <ResultSummary row={playerRow} madeCut={madeCut} />
              ) : (
                <h1 className="font-serif text-3xl font-medium tracking-[-0.01em]">That&apos;s a wrap</h1>
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
