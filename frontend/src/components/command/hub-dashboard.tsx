"use client";

import Image from "next/image";
import {
  Activity,
  ArrowUpRight,
  CalendarDays,
  Flag,
  HeartPulse,
  MapPin,
  Plane,
  Target,
  Trophy,
} from "lucide-react";

import { Card } from "@/components/command/card";
import { NextAction } from "@/components/command/identity-strip";
import {
  useAchievements,
  useCareerOverview,
  useCareerRecords,
  usePlayerFitness,
  usePlayerProfile,
} from "@/lib/api/queries";
import {
  attributeShortLabel,
  formatMoney,
  ordinalPosition,
  tourTierLabel,
} from "@/lib/career/labels";
import { formatTravelStop, lastPlayerResult, nextEnteredEventAfter, nextOpenWeek } from "@/lib/career/hub-summaries";
import { applySceneFallback, sceneBackdrop } from "@/lib/play/scene";

export function HubDashboard({ id }: { id: string }) {
  const { data, isPending, isError } = useCareerOverview(id);
  const profile = usePlayerProfile(id).data?.playerProfile ?? null;
  const fitness = usePlayerFitness(id).data?.playerFitness ?? null;
  const records = useCareerRecords(id);
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
  const schedule = data.playerSchedule ?? [];
  const upcoming = schedule
    .filter((entry) => entry.entered && entry.week >= world.week)
    .sort((a, b) => a.week - b.week)[0];
  const pending = world.hasPendingEvent ? data.currentEvent : null;
  const pendingSchedule = pending
    ? schedule.find((entry) => entry.name === pending.name && entry.week === world.week)
    : null;
  const event = world.hasPendingEvent
    ? pending
      ? { ...pending, week: world.week, tier: pendingSchedule?.tier }
      : pendingSchedule
    : upcoming;
  const travel = nextEnteredEventAfter(schedule, event?.week ?? null, world.week);
  const travelSummary = travel ? formatTravelStop(travel.location, travel.week) : null;
  const backdrop = event
    ? sceneBackdrop(event.courseType, event.name, { season: world.season })
    : null;
  const seasonStat = data.playerSeasonStats?.find((stat) => stat.season === world.season);
  const attributes = profile?.attributes ?? [];
  // Match the existing Profile spoke's unweighted overall, without merging individual abilities.
  const overallValue = attributes.length
    ? attributes.reduce((sum, attribute) => sum + attribute.value, 0) / attributes.length
    : null;
  const overall = overallValue?.toFixed(1) ?? "—";
  const result = lastPlayerResult(records.data?.careerRecords?.events ?? []);
  const restWeek = nextOpenWeek(schedule, world.week);
  const unlocked = achievements
    .filter((achievement) => achievement.unlocked)
    .sort((a, b) => (b.seasonUnlocked ?? 0) - (a.seasonUnlocked ?? 0));
  const news = [...(data.newsFeed ?? [])].sort((a, b) => b.prominence - a.prominence).slice(0, 3);
  const scorecard = data.playerScorecard;
  const eventInProgress = Boolean(
    scorecard &&
    (scorecard.roundNumber > 1 || scorecard.currentHole > 1 || scorecard.totalStrokes > 0),
  );
  const eventMessage = world.pendingEventFinished
    ? "Tournament complete. Finish the event to record your result."
    : world.hasPendingEvent
      ? eventInProgress
        ? "Your tournament is in progress. Continue when ready."
        : "Your tournament is ready to play."
      : upcoming
        ? null
        : "No entered tournament is ahead on the current schedule. You can still advance your career.";

  return (
    <main className="career-hub flex min-w-0 flex-col gap-3.5 xl:gap-4">
      <section
        aria-label="Next event and career progression"
        className="career-hub-hero border-border bg-surface relative isolate grid min-w-0 overflow-hidden rounded-xl border shadow-[var(--shadow-md)] lg:grid-cols-[minmax(0,1fr)_minmax(16rem,19rem)]"
      >
        <div className="relative isolate flex min-w-0 flex-col justify-center gap-2.5 overflow-hidden p-5 sm:p-6">
          {backdrop && (
            <>
              <Image
                src={backdrop.src}
                alt=""
                fill
                sizes="(min-width: 1024px) 70vw, 100vw"
                className="object-cover object-[center_58%]"
                onError={
                  backdrop.fallbackSrc
                    ? (image) => applySceneFallback(image.currentTarget, backdrop.fallbackSrc!)
                    : undefined
                }
              />
              <div className="from-surface/95 via-surface/65 absolute inset-0 bg-gradient-to-r to-transparent" />
              <div className="from-surface/30 absolute inset-0 bg-gradient-to-t to-transparent" />
            </>
          )}
          <div className="relative flex flex-col gap-2.5">
            <p className="text-primary text-xs font-bold tracking-[0.16em] uppercase">Next event</p>
            <h1 className="max-w-3xl text-2xl leading-tight font-bold tracking-[-0.025em] text-balance sm:text-3xl">
              {event?.name ?? (world.hasPendingEvent ? "Current event" : "No upcoming event")}
            </h1>
            {event && (
              <div className="flex min-w-0 flex-col gap-1.5 text-sm">
                <p className="inline-flex items-start gap-2">
                  <MapPin className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
                  <span>{event.location}</span>
                </p>
                <p className="text-muted-foreground flex flex-wrap items-center gap-x-2 gap-y-1">
                  {event.tier && <span>{tourTierLabel(event.tier)} Tour</span>}
                  {event.tier && <span aria-hidden="true">·</span>}
                  <span>Week {event.week}</span>
                </p>
                {event.hostName && (
                  <p className="text-subtle-foreground text-xs sm:text-sm">Host: {event.hostName}</p>
                )}
              </div>
            )}
            {eventMessage && <p className="text-muted-foreground max-w-xl text-sm">{eventMessage}</p>}
          </div>
        </div>
        <div className="border-border bg-surface-elevated relative flex flex-col justify-center gap-2.5 border-t p-5 sm:p-6 lg:border-t-0 lg:border-l">
          <p className="text-muted-foreground text-xs font-bold tracking-[0.16em] uppercase">
            Career progression
          </p>
          <h2 className="text-2xl font-bold tabular-nums">
            Season {world.season} · Week {world.week}
          </h2>
          <p className="text-muted-foreground text-sm">
            {world.pendingEventFinished
              ? "Bank your result before moving on."
              : world.hasPendingEvent
                ? "Play the event to continue your career."
                : "Advance to the next week when you’re ready."}
          </p>
          <div className="flex flex-wrap items-center gap-3 [&_a]:w-full [&_button]:w-full">
            <NextAction
              id={id}
              hasPendingEvent={world.hasPendingEvent}
              pendingEventFinished={world.pendingEventFinished}
              eventInProgress={eventInProgress}
              hub
            />
          </div>
        </div>
      </section>

      <section
        aria-labelledby="this-week-heading"
        className="career-hub-week border-border bg-surface-elevated rounded-xl border px-4 py-3 shadow-[var(--shadow-md)] sm:px-5"
      >
        <div className="mb-2 flex items-center gap-2.5">
          <CalendarDays className="text-primary size-4" aria-hidden="true" />
          <h2 id="this-week-heading" className="text-sm font-bold tracking-[0.09em] uppercase">
            This week
          </h2>
        </div>
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4 xl:gap-0">
          <WeekItem
            icon={HeartPulse}
            label="Condition"
            value={fitness ? `${Math.round(fitness.fitness * 100)}%` : "—"}
            detail={
              fitness
                ? fitness.availability.toLowerCase().replace(/\b\w/g, (char) => char.toUpperCase())
                : "Condition unavailable"
            }
          />
          <WeekItem
            icon={Plane}
            label="Travel"
            value={travelSummary?.destination ?? "No next stop scheduled"}
            detail={travelSummary?.detail ?? ""}
            compact
          />
          <WeekItem
            icon={Activity}
            label="Next open week"
            value={restWeek ? `Week ${restWeek}` : "—"}
            detail={restWeek ? "No entered event that week" : "No open week confirmed"}
          />
          <WeekItem
            icon={Flag}
            label="Last result"
            value={result?.finish ?? (records.isPending || records.isError ? "—" : "Debut pending")}
            detail={
              result?.eventName ??
              (records.isPending || records.isError
                ? "Result unavailable"
                : "No completed tournament yet")
            }
          />
        </div>
      </section>

      <div className="grid min-w-0 items-start gap-3.5 xl:grid-cols-2 xl:gap-4">
        <Card href={`/career/${id}/profile`} className="hover:border-primary/50 min-w-0 p-4">
          <PortalHeading title="Your golfer" />
          <div className="grid min-w-0 gap-4 sm:grid-cols-[minmax(8rem,10rem)_minmax(0,1fr)] sm:items-center 2xl:grid-cols-[minmax(8rem,11rem)_minmax(0,1fr)]">
            <div
              className="flex aspect-square max-w-36 rounded-full p-2 xl:max-w-40 2xl:max-w-44"
              style={{
                background:
                  overallValue == null
                    ? "var(--border-strong)"
                    : `conic-gradient(var(--primary) ${overallValue}%, var(--border-strong) 0)`,
              }}
            >
              <div className="bg-surface flex flex-1 flex-col items-center justify-center rounded-full text-center">
                <Target className="text-primary mb-2 size-5" aria-hidden="true" />
                <span className="text-muted-foreground text-xs">Overall</span>
                <strong className="text-3xl tabular-nums">{overall}</strong>
              </div>
            </div>
            <div className="min-w-0 space-y-1.5">
              {attributes.length ? (
                attributes.slice(0, 5).map((attribute) => (
                  <div
                    key={attribute.attribute}
                    className="grid grid-cols-[minmax(6rem,auto)_minmax(0,1fr)_2rem] items-center gap-2 text-xs sm:text-sm"
                  >
                    <span className="text-muted-foreground truncate">
                      {attributeShortLabel(attribute.attribute)}
                    </span>
                    <div className="bg-surface-3 h-2 overflow-hidden rounded-full">
                      <div
                        className="bg-info h-full rounded-full"
                        style={{ width: `${attribute.value}%` }}
                      />
                    </div>
                    <strong className="text-right tabular-nums">{attribute.value}</strong>
                  </div>
                ))
              ) : (
                <p className="text-muted-foreground text-sm">
                  Attributes appear once your golfer is set.
                </p>
              )}
              {attributes.length > 5 && (
                <p className="text-subtle-foreground pt-1 text-xs">
                  Showing 5 of {attributes.length} attributes · View full profile
                </p>
              )}
            </div>
          </div>
        </Card>

        <Card href={`/career/${id}/seasons`} className="hover:border-primary/50 min-w-0 p-4">
          <PortalHeading title="Season progress" />
          <div className="border-border grid grid-cols-2 gap-y-3 border-b pb-3 sm:grid-cols-4">
            <SeasonFig icon={Trophy} label="Events" value={seasonStat?.events ?? 0} />
            <SeasonFig icon={Trophy} label="Wins" value={seasonStat?.wins ?? 0} />
            <SeasonFig icon={Target} label="Top 10s" value={seasonStat?.topTens ?? 0} />
            <SeasonFig
              icon={Flag}
              label="Best finish"
              value={
                seasonStat && seasonStat.events > 0 ? ordinalPosition(seasonStat.bestFinish) : "—"
              }
            />
          </div>
          <p className="mt-3 text-sm">
            Earnings this season:{" "}
            <strong className="ml-1 text-lg tabular-nums">
              {formatMoney(seasonStat?.earnings ?? 0)}
            </strong>
          </p>
        </Card>

        <Card href={`/career/${id}/achievements`} className="hover:border-primary/50 min-w-0 p-4">
          <PortalHeading
            title="Achievements"
            icon={Trophy}
            right={
              <span className="text-muted-foreground ml-2 text-sm tabular-nums">
                <span className="text-gold">{unlocked.length}</span> / {achievements.length}
              </span>
            }
          />
          {unlocked.length ? (
            <ul className="space-y-2 text-sm">
              {unlocked.slice(0, 3).map((achievement) => (
                <li key={achievement.id} className="flex items-center gap-2">
                  <Trophy className="text-gold size-4 shrink-0" aria-hidden="true" />
                  <span className="min-w-0 truncate">{achievement.title}</span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-muted-foreground text-sm">
              None unlocked yet — silverware, majors and feats will land here.
            </p>
          )}
        </Card>

        <Card href={`/career/${id}/news`} className="hover:border-primary/50 min-w-0 p-4">
          <PortalHeading title="Tour news" icon={CalendarDays} />
          {news.length ? (
            <ul className="divide-border divide-y text-sm">
              {news.map((entry, index) => (
                <li key={`${entry.season}-${index}`} className="truncate py-2 first:pt-0 last:pb-0">
                  {entry.headline}
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-muted-foreground text-sm">No headlines yet — the season is young.</p>
          )}
        </Card>
      </div>
    </main>
  );
}

function WeekItem({
  icon: Icon,
  label,
  value,
  detail,
  compact = false,
}: {
  icon: typeof Activity;
  label: string;
  value: string;
  detail: string;
  compact?: boolean;
}) {
  return (
    <div className="xl:border-border flex min-w-0 items-start gap-2.5 xl:border-l xl:px-4 xl:first:border-l-0 xl:first:pl-0 xl:last:pr-0">
      <Icon className="text-primary mt-0.5 size-5 shrink-0" aria-hidden="true" />
      <div className="min-w-0">
        <h3 className="text-muted-foreground text-xs font-bold tracking-wide uppercase">{label}</h3>
        <p className={`mt-0.5 text-base font-semibold tabular-nums ${compact ? "overflow-x-auto whitespace-nowrap" : "break-words"}`} title={compact ? value : undefined}>{value}</p>
        {detail && <p className={`text-muted-foreground text-xs ${compact ? "overflow-x-auto whitespace-nowrap" : "break-words"}`}>{detail}</p>}
      </div>
    </div>
  );
}

function PortalHeading({
  title,
  icon: Icon,
  right,
}: {
  title: string;
  icon?: typeof Trophy;
  right?: React.ReactNode;
}) {
  return (
    <div className="border-border mb-3 flex items-center gap-2 border-b pb-2">
      {Icon && <Icon className="text-primary size-5" aria-hidden="true" />}
      <h2 className="text-base font-bold tracking-[0.08em] uppercase">{title}</h2>
      {right}
      <ArrowUpRight
        className="text-muted-foreground group-hover:text-primary ml-auto size-5"
        aria-hidden="true"
      />
    </div>
  );
}

function SeasonFig({
  icon: Icon,
  label,
  value,
}: {
  icon: typeof Trophy;
  label: string;
  value: string | number;
}) {
  return (
    <div className="border-border flex flex-col items-center gap-0.5 border-l px-2 text-center first:border-l-0">
      <Icon className="text-primary size-5" aria-hidden="true" />
      <strong className="text-xl tabular-nums">{value}</strong>
      <span className="text-muted-foreground text-xs">{label}</span>
    </div>
  );
}

function HubSkeleton() {
  return (
    <div className="flex animate-pulse flex-col gap-4">
      <div className="border-border bg-surface min-h-56 rounded-xl border" />
      <div className="border-border bg-surface min-h-28 rounded-xl border" />
      <div className="grid gap-4 xl:grid-cols-2">
        {Array.from({ length: 4 }, (_, index) => (
          <div key={index} className="border-border bg-surface min-h-40 rounded-xl border" />
        ))}
      </div>
    </div>
  );
}
