"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { ArrowRight, Check, Play } from "lucide-react";

import { Button } from "@/components/ui/button";
import { MobileCareerNavigation } from "@/components/command/sidebar";
import { CareerAccountMenu } from "@/components/command/career-account-menu";
import { useForegroundOperation } from "@/components/loading/foreground-loading-provider";
import { useCareerOverview, usePlayerProfile } from "@/lib/api/queries";
import { useAdvanceSeason, useAdvanceWeek, useCompleteEvent, useSeasonAdvanceOperation } from "@/lib/api/play";
import { formatMoney, ordinalPosition, tourTierLabel } from "@/lib/career/labels";

/*
 * The persistent player identity strip. Lives in the shell (not the page), so it stays
 * mounted across navigation — the who-am-I and the primary "what's next" action are
 * always in the same place. Reads from the shared query cache the hub also uses.
 */
export function IdentityStrip({ id }: { id: string }) {
  const careerOverview = useCareerOverview(id);
  const overview = careerOverview.data;
  const profile = usePlayerProfile(id).data?.playerProfile ?? null;

  const world = overview?.world;
  const seasonStat = overview?.playerSeasonStats?.find((s) => s.season === world?.season);
  const initials = profile
    ? `${profile.firstName[0] ?? ""}${profile.lastName[0] ?? ""}`.toUpperCase()
    : "··";

  return (
    <header className="border-border bg-surface/60 sticky top-0 z-[var(--z-sticky)] flex flex-col items-stretch gap-x-6 gap-y-3 border-b px-5 py-3.5 backdrop-blur-md md:px-7 lg:flex-row lg:flex-wrap lg:items-center">
      <div className="flex w-full min-w-0 items-center gap-2 sm:gap-3.5 lg:w-auto lg:flex-none">
        <MobileCareerNavigation id={id} />
        <span className="border-border-strong from-surface-elevated to-surface text-primary grid size-10 shrink-0 place-items-center rounded-xl border bg-gradient-to-br text-base font-bold tracking-[-0.03em] sm:size-12">
          {initials}
        </span>
        <div className="min-w-0 leading-tight">
          <div className="truncate text-[1.03rem] font-bold tracking-[-0.02em]">
            {profile ? `${profile.firstName} ${profile.lastName}` : "—"}
          </div>
          <div className="text-muted-foreground mt-0.5 truncate text-[0.78rem]">
            {profile ? (
              <>
                <span className="text-primary font-semibold">
                  {profile.tour ? tourTierLabel(profile.tour) : "—"}
                </span>
                {" · "}
                {profile.archetype.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase())}
                {" · Age "}
                {profile.age}
                {world ? ` · Season ${world.season}` : ""}
              </>
            ) : (
              "Loading career…"
            )}
          </div>
        </div>
        <CareerAccountMenu id={id} hasPendingEvent={Boolean(world?.hasPendingEvent)}
          statusReady={Boolean(world)} statusError={careerOverview.isError} />
      </div>

      <div className="hidden items-stretch xl:flex">
        <TopStat k="World Rank" v={profile?.worldRanking ? `${profile.worldRanking}` : "—"} accent />
        <TopStat k="Bankroll" v={profile ? formatMoney(profile.availableFunds) : "—"} />
        <TopStat k="Season Wins" v={seasonStat ? `${seasonStat.wins}` : "0"} />
        <TopStat
          k="Best this yr"
          v={seasonStat && seasonStat.events > 0 ? ordinalPosition(seasonStat.bestFinish) : "—"}
        />
      </div>

      <div className="flex w-full min-w-0 flex-wrap items-center gap-2.5 lg:ml-auto lg:w-auto lg:flex-nowrap">
        <NextAction
          id={id}
          hasPendingEvent={Boolean(world?.hasPendingEvent)}
          pendingEventFinished={Boolean(world?.pendingEventFinished)}
        />
      </div>
    </header>
  );
}

function TopStat({ k, v, accent }: { k: string; v: string; accent?: boolean }) {
  return (
    <div className="border-border border-l px-[1.1rem] first:border-l-0">
      <div className="text-subtle-foreground text-[0.65rem] font-semibold tracking-[0.11em] uppercase">
        {k}
      </div>
      <div
        className={`mt-0.5 text-[1.15rem] font-bold tabular-nums tracking-[-0.02em] ${accent ? "text-info" : ""}`}
      >
        {v}
      </div>
    </div>
  );
}

function NextAction({
  id,
  hasPendingEvent,
  pendingEventFinished,
}: {
  id: string;
  hasPendingEvent: boolean;
  pendingEventFinished: boolean;
}) {
  const router = useRouter();
  const advance = useAdvanceWeek(id);
  const complete = useCompleteEvent(id);
  const foreground = useForegroundOperation();
  const seasonOperation = useSeasonAdvanceOperation(id);
  const [actionError, setActionError] = useState<string | null>(null);
  const seasonBefore = useCareerOverview(id).data?.world?.season ?? null;

  // A season simulation can remain active after its HTTP response is lost. Keep its status visible even if
  // a fresh overview read happens to observe an intermediate pending event.
  if (seasonOperation.isPending || seasonOperation.isError
      || seasonOperation.data?.seasonAdvanceOperation?.state === "RUNNING"
      || seasonOperation.data?.seasonAdvanceOperation?.state === "FAILED") {
    return <SimSeasonControl id={id} />;
  }

  // The pending event has been played to its end (the completion screen): the primary action is to finish
  // it — bank the result and resume the week — exactly like the "Finish event" button on that screen, not
  // to re-open a finished event. Crossing the season boundary lands on the off-season review (see play-event).
  if (hasPendingEvent && pendingEventFinished) {
    async function onFinish() {
      setActionError(null);
      try {
        const result = await foreground.run({ label: "Finalizing tournament", presentation: "immediate" }, () => complete.mutateAsync());
        if (!result) return;
        if (seasonBefore != null && result.completeEvent.season > seasonBefore) {
          router.push(`/career/${id}/offseason`);
        } else {
          router.push(`/career/${id}`);
        }
        router.refresh();
      } catch {
        setActionError("The event couldn't be finalized. Please try again.");
      }
    }
    return (
      <div className="flex items-center gap-2">
        {actionError ? <p role="alert" className="text-destructive text-sm">{actionError}</p> : null}
        <Button onClick={onFinish} disabled={complete.isPending || foreground.isPending}>
          <Check className="size-4" aria-hidden="true" />
          {complete.isPending || foreground.isPending ? "Finishing…" : "Finish event"}
        </Button>
      </div>
    );
  }

  if (hasPendingEvent) {
    return (
      <Button asChild>
        <Link href={`/career/${id}/play`}>
          <Play className="size-4" aria-hidden="true" />
          Play event
        </Link>
      </Button>
    );
  }

  async function onAdvance() {
    setActionError(null);
    try {
      const result = await foreground.run({ label: "Advancing week", presentation: "delayed" }, () => advance.mutateAsync());
      if (!result) return;
      // Crossing the season boundary is the off-season moment — the review takes priority over any
      // week-1 event pending in the new season (that event is still there once they begin the season).
      if (seasonBefore != null && result.advanceWeek.season > seasonBefore) {
        router.push(`/career/${id}/offseason`);
        return;
      }
      if (result.advanceWeek.hasPendingEvent) router.push(`/career/${id}/play`);
    } catch {
      setActionError("The week couldn't be advanced. Please try again.");
    }
  }

  return (
    <>
      <SimSeasonControl id={id} />
      {actionError ? <p role="alert" className="text-destructive text-sm">{actionError}</p> : null}
      <Button variant="secondary" onClick={onAdvance} disabled={advance.isPending || foreground.hasPendingForegroundOperation}>
        {advance.isPending || foreground.isPending ? "Advancing…" : "Advance week"}
        <ArrowRight className="size-4" aria-hidden="true" />
      </Button>
    </>
  );
}

/**
 * Fast-forward to the end of the season. It sims every remaining entered event and
 * confirms in place first. The operation status stays visible after a lost response.
 */
export function SimSeasonControl({ id }: { id: string }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const advanceSeason = useAdvanceSeason(id);
  const operation = useSeasonAdvanceOperation(id);
  const startingSeason = useCareerOverview(id).data?.world?.season;
  const foreground = useForegroundOperation();
  const [confirming, setConfirming] = useState(false);
  const [requestId, setRequestId] = useState<string | null>(null);
  const [uncertain, setUncertain] = useState(false);
  const [completed, setCompleted] = useState(false);
  const completedRef = useRef(false);
  const latest = operation.data?.seasonAdvanceOperation;
  const trackedId = requestId ?? (latest?.state === "RUNNING" || latest?.state === "FAILED" ? latest.requestId : null);
  const trackedState = latest?.requestId === trackedId ? latest.state : null;
  const running = trackedState === "RUNNING" && !operation.isError;
  const failed = trackedState === "FAILED";
  const unknown = !completed && !running && !failed && (uncertain || operation.isError);
  const blocked = completed || running || failed || unknown || operation.isPending || advanceSeason.isPending
    || foreground.hasPendingForegroundOperation || startingSeason == null;

  const finishCompleted = useCallback(async () => {
    if (completedRef.current) return;
    completedRef.current = true;
    setCompleted(true);
    await queryClient.invalidateQueries();
    router.push(`/career/${id}/offseason`);
  }, [id, queryClient, router]);

  useEffect(() => {
    if (!requestId && latest?.state === "RUNNING") {
      const timer = setTimeout(() => setRequestId(latest.requestId), 0);
      return () => clearTimeout(timer);
    }
  }, [latest?.requestId, latest?.state, requestId]);

  useEffect(() => {
    if (trackedState === "COMPLETED" && requestId && latest?.requestId === requestId) {
      void finishCompleted();
    }
  }, [finishCompleted, latest?.requestId, requestId, trackedState]);

  // Simming the season always crosses the boundary, so it lands on the off-season review.
  async function onConfirm() {
    if (blocked || startingSeason == null) return;
    const nextRequestId = crypto.randomUUID();
    setRequestId(nextRequestId);
    setUncertain(false);
    try {
      const result = await foreground.run({ label: "Simulating season", presentation: "immediate" },
        () => advanceSeason.mutateAsync({ requestId: nextRequestId, expectedSeason: startingSeason }));
      if (result?.advanceSeason.state === "COMPLETED") {
        await finishCompleted();
      } else if (result) {
        setUncertain(true);
        await operation.refetch();
      }
    } catch {
      // A lost HTTP response says nothing about whether the backend finished. Read its operation record.
      setUncertain(true);
      await operation.refetch();
    }
  }

  const notice = completed ? "Season complete. Opening the off-season review…"
    : running ? "Season simulation is still running. Another simulation cannot start."
    : failed ? "Season simulation stopped after progress may have changed. This session cannot safely retry."
    : unknown ? "Season completion is unknown. Check status before taking another action."
    : operation.isPending ? "Checking season simulation status…"
    : null;
  const noticeContent = notice ? (
    <span role={failed || unknown ? "alert" : "status"}
      className={`${failed || unknown ? "text-destructive" : "text-muted-foreground"} text-sm`}>
      {notice}
      {unknown ? <Button variant="ghost" size="sm" onClick={() => void operation.refetch()}>Check status</Button> : null}
    </span>
  ) : null;

  if (!confirming) {
    return (
      <span className="flex flex-wrap items-center gap-2">
        <Button variant="ghost" onClick={() => setConfirming(true)} disabled={blocked}>Sim season</Button>
        {noticeContent}
      </span>
    );
  }

  return (
    <span className="flex flex-wrap items-center gap-1.5">
      <span className="text-muted-foreground hidden text-sm sm:inline">Sim remaining events?</span>
      <Button
        variant="secondary"
        onClick={onConfirm}
        disabled={blocked}
      >
        {advanceSeason.isPending || foreground.isPending ? "Simming…" : "Confirm"}
      </Button>
      <Button variant="ghost" onClick={() => setConfirming(false)} disabled={advanceSeason.isPending || foreground.isPending}>
        Cancel
      </Button>
      {noticeContent}
    </span>
  );
}
