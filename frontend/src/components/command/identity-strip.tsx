"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { ArrowRight, Check, Play } from "lucide-react";

import { Button } from "@/components/ui/button";
import { useForegroundOperation } from "@/components/loading/foreground-loading-provider";
import { useCareerOverview, usePlayerProfile } from "@/lib/api/queries";
import { useAdvanceSeason, useAdvanceWeek, useCompleteEvent } from "@/lib/api/play";
import { formatMoney, ordinalPosition, tourTierLabel } from "@/lib/career/labels";

/*
 * The persistent player identity strip. Lives in the shell (not the page), so it stays
 * mounted across navigation — the who-am-I and the primary "what's next" action are
 * always in the same place. Reads from the shared query cache the hub also uses.
 */
export function IdentityStrip({ id }: { id: string }) {
  const overview = useCareerOverview(id).data;
  const profile = usePlayerProfile(id).data?.playerProfile ?? null;

  const world = overview?.world;
  const seasonStat = overview?.playerSeasonStats?.find((s) => s.season === world?.season);
  const initials = profile
    ? `${profile.firstName[0] ?? ""}${profile.lastName[0] ?? ""}`.toUpperCase()
    : "··";

  return (
    <header className="border-border bg-surface/60 sticky top-0 z-[var(--z-sticky)] flex flex-wrap items-center gap-x-6 gap-y-3 border-b px-5 py-3.5 backdrop-blur-md md:px-7">
      <div className="flex items-center gap-3.5">
        <span className="border-border-strong from-surface-elevated to-surface text-primary grid size-12 place-items-center rounded-xl border bg-gradient-to-br text-base font-bold tracking-[-0.03em]">
          {initials}
        </span>
        <div className="leading-tight">
          <div className="text-[1.03rem] font-bold tracking-[-0.02em]">
            {profile ? `${profile.firstName} ${profile.lastName}` : "—"}
          </div>
          <div className="text-muted-foreground mt-0.5 text-[0.78rem]">
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

      <div className="ml-auto flex items-center gap-2.5">
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
  const [actionError, setActionError] = useState<string | null>(null);
  const seasonBefore = useCareerOverview(id).data?.world?.season ?? null;

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
 * Fast-forward to the end of the season. It sims every remaining event the player is
 * entered in, so it confirms in place first. Only reachable when no event is pending
 * (the engine cannot advance past one) — NextAction only renders it in that branch.
 */
function SimSeasonControl({ id }: { id: string }) {
  const router = useRouter();
  const advanceSeason = useAdvanceSeason(id);
  const foreground = useForegroundOperation();
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Simming the season always crosses the boundary, so it lands on the off-season review.
  async function onConfirm() {
    setError(null);
    try {
      const result = await foreground.run({ label: "Simulating season", presentation: "immediate" }, () => advanceSeason.mutateAsync());
      if (result) router.push(`/career/${id}/offseason`);
    } catch {
      setError("The season simulation couldn't be completed. Please try again.");
    }
  }

  if (!confirming) {
    return (
      <Button variant="ghost" onClick={() => setConfirming(true)} disabled={foreground.hasPendingForegroundOperation}>
        Sim season
      </Button>
    );
  }

  return (
    <span className="flex flex-wrap items-center gap-1.5">
      <span className="text-muted-foreground hidden text-sm sm:inline">Sim remaining events?</span>
      <Button
        variant="secondary"
        onClick={onConfirm}
        disabled={advanceSeason.isPending || foreground.isPending}
      >
        {advanceSeason.isPending || foreground.isPending ? "Simming…" : "Confirm"}
      </Button>
      <Button variant="ghost" onClick={() => setConfirming(false)} disabled={advanceSeason.isPending || foreground.isPending}>
        Cancel
      </Button>
      {error ? <span role="alert" className="text-destructive text-sm">{error}</span> : null}
    </span>
  );
}
