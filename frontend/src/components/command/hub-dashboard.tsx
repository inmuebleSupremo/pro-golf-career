"use client";

import Image from "next/image";
import { MapPin, Play, Trophy } from "lucide-react";

import { Card, CardHeader } from "@/components/command/card";
import { applySceneFallback, sceneBackdrop } from "@/lib/play/scene";
import { useAchievements, useCareerOverview, useHallOfFame, usePlayerProfile } from "@/lib/api/queries";
import {
  attributeShortLabel,
  eventPrestigeLabel,
  formatMoney,
  ordinalPosition,
  tourTierLabel,
} from "@/lib/career/labels";

type NewsTone = "gold" | "info" | "danger" | "neutral";
function newsTone(type: string): { tone: NewsTone; label: string } {
  switch (type) {
    case "TOURNAMENT_VICTORY":
      return { tone: "gold", label: "Win" };
    case "MAJOR_VICTORY":
      return { tone: "gold", label: "Major" };
    case "GOAL_ACHIEVED":
      return { tone: "gold", label: "Goal" };
    case "ACHIEVEMENT_UNLOCKED":
      return { tone: "gold", label: "Feat" };
    case "WORLD_NUMBER_ONE":
      return { tone: "info", label: "No. 1" };
    case "PROMOTION":
      return { tone: "info", label: "Tour" };
    case "RISING_PROSPECT":
      return { tone: "info", label: "Prospect" };
    case "MAJOR_UPSET":
      return { tone: "info", label: "Upset" };
    case "INJURY":
      return { tone: "danger", label: "Injury" };
    default:
      return { tone: "neutral", label: type.replace(/_/g, " ").toLowerCase() };
  }
}

export function HubDashboard({ id }: { id: string }) {
  const { data, isPending, isError } = useCareerOverview(id);
  const profile = usePlayerProfile(id).data?.playerProfile ?? null;
  const inductions = useHallOfFame(id).data?.hallOfFame ?? [];
  const achievements = useAchievements(id).data?.achievements ?? [];

  if (isPending) return <HubSkeleton />;
  if (isError || !data?.world) {
    return (
      <p className="text-muted-foreground py-16 text-center text-sm">
        We couldn&apos;t load this career. Try again in a moment.
      </p>
    );
  }

  const world = data.world;
  const unlockedAchievements = achievements
    .filter((a) => a.unlocked)
    .sort((a, b) => (b.seasonUnlocked ?? 0) - (a.seasonUnlocked ?? 0));
  // The card only has five slots — show the most newsworthy of the recent items (a stable sort
  // keeps recency order among equal prominence, since the feed arrives most-recent-first).
  const news = [...(data.newsFeed ?? [])].sort((a, b) => b.prominence - a.prominence).slice(0, 5);
  const seasonStat = data.playerSeasonStats?.find((s) => s.season === world.season);
  const nextEvent = (data.playerSchedule ?? [])
    .filter((e) => e.entered && e.week >= world.week)
    .sort((a, b) => a.week - b.week)[0];
  const eventBackdrop = nextEvent ? sceneBackdrop(nextEvent.courseType, nextEvent.name, { season: world.season }) : null;
  const attrs = profile?.attributes ?? [];
  const overall = attrs.length
    ? (attrs.reduce((s, a) => s + a.value, 0) / attrs.length).toFixed(1)
    : "—";

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-baseline gap-3">
        <h1 className="text-[1.45rem] font-bold tracking-[-0.025em]">Hub</h1>
        <span className="text-muted-foreground text-sm">Everything at a glance — one tap to anywhere.</span>
        <span className="border-border bg-surface text-muted-foreground ml-auto rounded-full border px-3 py-1.5 text-xs font-semibold tabular-nums">
          Season {world.season} · Week {world.week}
        </span>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {/* Next event — the hero portal */}
        <Card href={`/career/${id}/play`} className="sm:col-span-2">
          <div className="relative isolate -m-5 mb-0 overflow-hidden rounded-t-xl border-b border-[var(--border)] p-5">
            {nextEvent && eventBackdrop ? (
              <>
                {/* The event's course scene, matched to its style/place — the same imagery the play screen uses. */}
                <Image
                  src={eventBackdrop.src}
                  alt=""
                  fill
                  sizes="(min-width: 640px) 50vw, 100vw"
                  className="object-cover object-center"
                  onError={
                    eventBackdrop.fallbackSrc
                      ? (event) =>
                          applySceneFallback(event.currentTarget, eventBackdrop.fallbackSrc!)
                      : undefined
                  }
                />
                <div className="from-surface-elevated via-surface-elevated/80 absolute inset-0 bg-gradient-to-t to-transparent" />
                <div className="from-surface-elevated/70 absolute inset-x-0 top-0 h-16 bg-gradient-to-b to-transparent" />
              </>
            ) : (
              <div className="absolute inset-0 bg-[radial-gradient(130%_170%_at_88%_0%,color-mix(in_oklch,var(--gold),transparent_86%),transparent_52%)]" />
            )}
            <div className="relative flex min-h-[8.5rem] flex-col gap-3">
              <div className="flex flex-wrap items-center gap-2">
                {nextEvent && nextEvent.prestige === "MAJOR" ? (
                  <span className="gold-metal gold-shine inline-flex rounded-md px-2 py-1 text-[0.65rem] font-bold tracking-wide uppercase">
                    ◆ Major
                  </span>
                ) : nextEvent ? (
                  <span className="border-info/30 bg-info/10 text-info inline-flex rounded-md border px-2 py-1 text-[0.65rem] font-bold tracking-wide uppercase backdrop-blur-sm">
                    {eventPrestigeLabel(nextEvent.prestige)}
                  </span>
                ) : null}
                {nextEvent && (
                  <span className="border-info/30 bg-info/10 text-info inline-flex rounded-md border px-2 py-1 text-[0.65rem] font-bold tracking-wide uppercase backdrop-blur-sm">
                    {tourTierLabel(nextEvent.tier)}
                  </span>
                )}
              </div>
              <h2 className="text-[1.55rem] leading-[1.08] font-bold tracking-[-0.03em] text-balance">
                {nextEvent ? nextEvent.name : "No upcoming events"}
              </h2>
              {nextEvent ? (
                <p className="text-muted-foreground flex items-center gap-1 text-[0.8rem] font-medium">
                  <MapPin className="size-3.5" aria-hidden="true" />
                  {nextEvent.location}
                </p>
              ) : null}
              <p className="text-subtle-foreground mt-auto text-[0.8rem]">
                {nextEvent
                  ? `Week ${nextEvent.week} · ${tourTierLabel(nextEvent.tier)}`
                  : "You've played or skipped every event on the calendar."}
              </p>
            </div>
          </div>
          <div className="mt-auto flex items-center gap-3 pt-5">
            {world.hasPendingEvent ? (
              <span className="text-primary inline-flex items-center gap-2 text-sm font-semibold">
                <Play className="size-4" aria-hidden="true" /> Ready to play — shot-by-shot or sim
              </span>
            ) : nextEvent ? (
              <span className="text-muted-foreground text-sm">Advance the week to reach it.</span>
            ) : (
              <span className="text-muted-foreground text-sm">Advance the season for a fresh calendar.</span>
            )}
          </div>
        </Card>

        {/* World rank */}
        <Card href={`/career/${id}/rankings`}>
          <CardHeader title="World Rank" portal />
          <div className="text-info text-[2.25rem] leading-none font-bold tabular-nums tracking-[-0.035em]">
            <span className="text-subtle-foreground align-[3px] text-xl">#</span>
            {profile?.worldRanking ?? "—"}
          </div>
          <p className="text-muted-foreground mt-2.5 text-[0.8rem]">
            of {world.activePopulation.toLocaleString()} professionals
          </p>
        </Card>

        {/* Finances */}
        <Card href={`/career/${id}/finances`}>
          <CardHeader title="Finances" portal />
          <div className="text-[2rem] leading-none font-bold tabular-nums tracking-[-0.03em]">
            {profile ? formatMoney(profile.availableFunds) : "—"}
          </div>
          <p className="text-muted-foreground mt-2.5 text-[0.8rem]">Available funds</p>
          <p className="text-subtle-foreground mt-1 text-[0.75rem] tabular-nums">
            Career earnings {profile ? formatMoney(profile.careerEarnings) : "—"}
          </p>
        </Card>

        {/* Achievements */}
        <Card href={`/career/${id}/achievements`} className="sm:col-span-2">
          <CardHeader
            title="Achievements"
            portal
            right={
              achievements.length > 0 ? (
                <span className="text-subtle-foreground text-xs tabular-nums">
                  <span className="text-gold font-semibold">{unlockedAchievements.length}</span> /{" "}
                  {achievements.length}
                </span>
              ) : undefined
            }
          />
          {unlockedAchievements.length === 0 ? (
            <p className="text-muted-foreground text-sm">
              None unlocked yet — silverware, majors, and feats will land here.
            </p>
          ) : (
            <div className="flex flex-col gap-2">
              {unlockedAchievements.slice(0, 4).map((a) => (
                <div key={a.id} className="flex items-center gap-2.5 text-sm">
                  <Trophy className="text-gold size-4 shrink-0" aria-hidden="true" />
                  <span className="min-w-0 flex-1 truncate font-medium">{a.title}</span>
                  {a.seasonUnlocked != null ? (
                    <span className="text-subtle-foreground shrink-0 font-mono text-[0.7rem] tabular-nums">
                      S{a.seasonUnlocked}
                    </span>
                  ) : null}
                </div>
              ))}
            </div>
          )}
        </Card>

        {/* This season */}
        <Card href={`/career/${id}/seasons`} className="sm:col-span-2">
          <CardHeader title={`Season ${world.season}`} portal />
          <div className="grid grid-cols-4 gap-2">
            <SeasonFig k="Events" v={seasonStat?.events ?? 0} />
            <SeasonFig k="Wins" v={seasonStat?.wins ?? 0} accent />
            <SeasonFig k="Top 10s" v={seasonStat?.topTens ?? 0} />
            <SeasonFig
              k="Best"
              v={seasonStat && seasonStat.events > 0 ? ordinalPosition(seasonStat.bestFinish) : "—"}
            />
          </div>
          <p className="text-subtle-foreground mt-4 text-[0.75rem] tabular-nums">
            Earnings this season {seasonStat ? formatMoney(seasonStat.earnings) : formatMoney(0)}
          </p>
        </Card>

        {/* Development */}
        <Card href={`/career/${id}/manage`} className="sm:col-span-2">
          <CardHeader
            title="Development"
            portal
            right={
              <span className="text-subtle-foreground text-xs tabular-nums">
                Overall <span className="text-foreground font-semibold">{overall}</span>
              </span>
            }
          />
          {attrs.length === 0 ? (
            <p className="text-muted-foreground text-sm">Attributes appear once your golfer is set.</p>
          ) : (
            <div className="grid grid-cols-1 gap-x-6 gap-y-3 sm:grid-cols-2">
              {attrs.map((a) => (
                <div key={a.attribute}>
                  <div className="mb-1.5 flex justify-between text-[0.72rem]">
                    <span className="text-muted-foreground">{attributeShortLabel(a.attribute)}</span>
                    <span className="font-bold tabular-nums">{a.value}</span>
                  </div>
                  <div className="bg-surface-3 h-[5px] overflow-hidden rounded-full">
                    <div
                      className="from-info/50 to-info h-full rounded-full bg-gradient-to-r"
                      style={{ width: `${a.value}%` }}
                    />
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>

        {/* News — fixed height so new headlines never grow the card (zero CLS). */}
        <Card href={`/career/${id}/news`} className="sm:col-span-2 xl:col-span-1">
          <CardHeader title="Tour News" portal />
          <div className="h-[13.5rem] overflow-hidden">
            {news.length === 0 ? (
              <p className="text-muted-foreground text-sm">No headlines yet — the season is young.</p>
            ) : (
              <div className="flex flex-col">
                {news.map((n, i) => (
                  <NewsRow key={i} type={n.type} headline={n.headline} season={n.season} />
                ))}
              </div>
            )}
          </div>
        </Card>

        {/* Hall of Fame */}
        <Card href={`/career/${id}/hall-of-fame`}>
          <CardHeader title="Hall of Fame" portal />
          {inductions.length === 0 ? (
            <p className="text-muted-foreground text-sm">No inductees yet.</p>
          ) : (
            <>
              <div className="gold-metal-text inline-flex items-center gap-2 text-[1.35rem] font-bold tracking-[-0.02em]">
                <Trophy className="text-gold size-5" aria-hidden="true" />
                {inductions.length}
              </div>
              <p className="text-muted-foreground mt-2 text-[0.8rem]">
                inducted · latest {inductions[inductions.length - 1]?.name}
              </p>
            </>
          )}
        </Card>
      </div>
    </div>
  );
}

function SeasonFig({ k, v, accent }: { k: string; v: string | number; accent?: boolean }) {
  return (
    <div className="border-border bg-surface rounded-lg border p-3 text-center">
      <div className={`text-xl font-bold tabular-nums tracking-[-0.02em] ${accent ? "text-primary" : ""}`}>{v}</div>
      <div className="text-subtle-foreground mt-1 text-[0.65rem] font-semibold tracking-wide uppercase">{k}</div>
    </div>
  );
}

function NewsRow({ type, headline, season }: { type: string; headline: string; season: number }) {
  const { tone, label } = newsTone(type);
  const toneClass =
    tone === "gold"
      ? "gold-metal text-[#3c2f12]"
      : tone === "info"
        ? "bg-info/12 text-info"
        : tone === "danger"
          ? "bg-destructive/14 text-destructive"
          : "bg-surface-3 text-muted-foreground";
  return (
    <div className="border-border flex gap-3 border-t py-3 first:border-t-0 first:pt-0.5">
      <span
        className={`mt-px h-fit shrink-0 rounded-md px-1.5 py-1 text-[0.6rem] font-bold tracking-wide uppercase ${toneClass}`}
      >
        {label}
      </span>
      <div className="min-w-0">
        <p className="truncate text-[0.82rem] leading-snug">{headline}</p>
        <p className="text-subtle-foreground mt-1 text-[0.7rem]">Season {season}</p>
      </div>
    </div>
  );
}

function HubSkeleton() {
  return (
    <div className="flex flex-col gap-6">
      <div className="h-8" />
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {[
          "sm:col-span-2 h-56",
          "h-56",
          "h-56",
          "sm:col-span-2 h-44",
          "sm:col-span-2 h-44",
          "sm:col-span-2 h-48",
          "sm:col-span-2 xl:col-span-1 h-48",
          "h-48",
        ].map((c, i) => (
          <div
            key={i}
            className={`border-border bg-surface/60 animate-pulse rounded-xl border ${c}`}
          />
        ))}
      </div>
    </div>
  );
}
