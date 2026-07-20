"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowRight, Play } from "lucide-react";

import { Button } from "@/components/ui/button";
import { useCareerOverview, usePlayerProfile } from "@/lib/api/queries";
import { useAdvanceWeek } from "@/lib/api/play";
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
        <NextAction id={id} hasPendingEvent={Boolean(world?.hasPendingEvent)} />
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

function NextAction({ id, hasPendingEvent }: { id: string; hasPendingEvent: boolean }) {
  const router = useRouter();
  const advance = useAdvanceWeek(id);

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
    const result = await advance.mutateAsync();
    if (result.advanceWeek.hasPendingEvent) router.push(`/career/${id}/play`);
  }

  return (
    <Button variant="secondary" onClick={onAdvance} disabled={advance.isPending}>
      {advance.isPending ? "Advancing…" : "Advance week"}
      <ArrowRight className="size-4" aria-hidden="true" />
    </Button>
  );
}
