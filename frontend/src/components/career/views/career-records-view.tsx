"use client";

import { useState } from "react";
import { motion, useReducedMotion } from "motion/react";
import { ChevronDown, Crown, Info, MapPin, Medal, Target, Trophy } from "lucide-react";

import { Card, CardHeader } from "@/components/command/card";
import { SpokeShell, SpokeEmpty, useSpokeGate } from "@/components/career/spoke";
import { useCareerRecords } from "@/lib/api/queries";
import { eventPrestigeLabel, formatMoney, formatScore, ordinalPosition } from "@/lib/career/labels";

type ScoringHighlight = {
  scoreToPar: number;
  eventName: string;
  location: string;
  season: number;
  date: string;
  round: number | null;
};

type Result = {
  season: number;
  date: string;
  position: number;
  scoreToPar: number;
  won: boolean;
  madeCut: boolean;
  location: string;
  prize: number;
  fairwaysHit: number;
  fairwaysPossible: number;
  greensInRegulation: number;
  holesPlayed: number;
  putts: number;
  roundScores: number[];
};

type EventHistory = {
  eventName: string;
  location: string;
  prestige: string;
  tier: string;
  tourTier: string;
  appearances: number;
  wins: number;
  bestPosition: number;
  results: Result[];
};

type Summary = {
  events: number;
  wins: number;
  majors: number;
  runnerUps: number;
  topTens: number;
  cutsMade: number;
  bestFinish: number | null;
  lowestRound: ScoringHighlight | null;
  lowestTournament: ScoringHighlight | null;
  careerEarnings: number;
};

/** A short calendar date from the ISO string the API stores (e.g. "6 Apr 2026"). */
function formatEventDate(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric", timeZone: "UTC" });
}

/**
 * Career Records — the player-specific counterpart to the world Record Book. A bento of headline totals and
 * scoring bests over a browsable, expandable per-event history, split by tour (Pro / Development): each event
 * reveals its best results, and any recurring event opens the full timeline of every victory.
 */
export function CareerRecordsView({ id }: { id: string }) {
  const query = useCareerRecords(id);
  const gate = useSpokeGate(query);
  if (gate) return gate;

  const records = query.data?.careerRecords ?? null;
  const summary: Summary | null = records?.summary ?? null;
  const events: EventHistory[] = records?.events ?? [];

  return (
    <SpokeShell
      title="Career Records"
      description="Your personal record book — the where, when and how of every result."
      action={
        summary ? (
          <span className="text-subtle-foreground font-mono text-sm tabular-nums">
            <span className="text-gold font-semibold">{summary.wins}</span> wins · {summary.events} events
          </span>
        ) : null
      }
    >
      {!summary || summary.events === 0 ? (
        <SpokeEmpty>Your records fill in as you compete — play an event to open your book.</SpokeEmpty>
      ) : (
        <div className="flex flex-col gap-9">
          <SummaryBento summary={summary} />
          <EventHistorySection events={events} />
        </div>
      )}
    </SpokeShell>
  );
}

/* ------------------------------------------------------------------ */
/* Headline bento                                                      */
/* ------------------------------------------------------------------ */

function SummaryBento({ summary }: { summary: Summary }) {
  return (
    <section className="flex flex-col gap-3">
      <SectionLabel>At a glance</SectionLabel>
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 xl:grid-cols-4">
        {/* Victories — the honours headline (gold), spanning two columns. */}
        <Card className="col-span-2 xl:col-span-2">
          <CardHeader title="Victories" />
          <div className="flex items-end gap-4">
            <div className="gold-metal-text text-[2.75rem] leading-none font-bold tracking-[-0.035em] tabular-nums">
              {summary.wins}
            </div>
            <div className="mb-1 flex flex-col gap-0.5">
              <span className="text-gold inline-flex items-center gap-1.5 text-sm font-semibold">
                <Crown className="size-4" aria-hidden="true" />
                {summary.majors} {summary.majors === 1 ? "major" : "majors"}
              </span>
              <span className="text-subtle-foreground text-xs tabular-nums">
                {summary.runnerUps} runner-up finishes
              </span>
            </div>
          </div>
        </Card>

        <StatTile
          label="Best Finish"
          value={summary.bestFinish != null ? ordinalPosition(summary.bestFinish) : "—"}
          icon={<Medal className="size-4" aria-hidden="true" />}
          accent={summary.bestFinish === 1 ? "gold" : "default"}
        />
        <StatTile label="Career Earnings" value={formatMoney(summary.careerEarnings)} accent="primary" />
        <ScoringTile
          label="Lowest Round"
          highlight={summary.lowestRound}
          icon={<Target className="size-4" aria-hidden="true" />}
        />
        <ScoringTile label="Lowest Tournament" highlight={summary.lowestTournament} />
        <StatTile label="Top 10s" value={summary.topTens} />
        <StatTile label="Cuts Made" value={summary.cutsMade} />
      </div>
    </section>
  );
}

function StatTile({
  label,
  value,
  icon,
  accent = "default",
}: {
  label: string;
  value: string | number;
  icon?: React.ReactNode;
  accent?: "default" | "gold" | "primary";
}) {
  const valueClass =
    accent === "gold" ? "gold-metal-text" : accent === "primary" ? "text-primary" : "text-foreground";
  return (
    <Card>
      <CardHeader title={label} right={icon ? <span className="text-subtle-foreground">{icon}</span> : undefined} />
      <div className={`text-[1.7rem] leading-none font-bold tracking-[-0.03em] tabular-nums ${valueClass}`}>
        {value}
      </div>
    </Card>
  );
}

/**
 * A scoring-record tile whose score reveals its story on hover (desktop) or tap (mobile): which round, at
 * which tournament, and when. The reveal is a plain disclosure so it stays keyboard- and screen-reader-safe.
 */
function ScoringTile({
  label,
  highlight,
  icon,
}: {
  label: string;
  highlight: ScoringHighlight | null;
  icon?: React.ReactNode;
}) {
  const [open, setOpen] = useState(false);
  const value = highlight != null ? formatScore(highlight.scoreToPar) : "—";

  // Reveal on hover (mouse) and on focus (keyboard Tab, or a mobile tap — which focuses the button);
  // tapping elsewhere blurs it shut. No click-toggle, so focus + click can't cancel each other out.
  return (
    <div
      className="relative"
      onPointerEnter={(e) => e.pointerType === "mouse" && highlight && setOpen(true)}
      onPointerLeave={(e) => e.pointerType === "mouse" && setOpen(false)}
    >
      <Card>
        <CardHeader
          title={label}
          right={
            highlight ? (
              <button
                type="button"
                aria-expanded={open}
                aria-label={`${label} details`}
                onFocus={() => setOpen(true)}
                onBlur={() => setOpen(false)}
                className="text-subtle-foreground hover:text-foreground focus-visible:text-foreground -m-1 rounded-full p-1 transition-colors"
              >
                <Info className="size-4" aria-hidden="true" />
              </button>
            ) : icon ? (
              <span className="text-subtle-foreground">{icon}</span>
            ) : undefined
          }
        />
        <div className="text-foreground text-[1.7rem] leading-none font-bold tracking-[-0.03em] tabular-nums">
          {value}
        </div>
      </Card>

      {open && highlight ? (
        <div
          role="tooltip"
          className="border-border-strong bg-surface-elevated absolute top-full left-0 z-20 mt-2 w-64 max-w-[80vw] rounded-lg border p-3.5 shadow-[var(--shadow-md)]"
        >
          <div className="flex items-baseline justify-between gap-3">
            <span className="text-subtle-foreground text-[0.6rem] font-bold tracking-[0.12em] uppercase">
              {highlight.round != null ? `Round ${highlight.round}` : "Tournament total"}
            </span>
            <span className="font-mono text-base font-bold tabular-nums">{formatScore(highlight.scoreToPar)}</span>
          </div>
          <p className="text-foreground mt-1.5 text-sm font-medium">{highlight.eventName}</p>
          <p className="text-subtle-foreground mt-1 flex items-center gap-1.5 text-xs">
            <MapPin className="size-3" aria-hidden="true" />
            {highlight.location}
          </p>
          <p className="text-subtle-foreground mt-1 text-xs tabular-nums">
            Season {highlight.season} · {formatEventDate(highlight.date)}
          </p>
        </div>
      ) : null}
    </div>
  );
}

/* ------------------------------------------------------------------ */
/* Per-event history (split by tour, each section collapsible)         */
/* ------------------------------------------------------------------ */

function EventHistorySection({ events }: { events: EventHistory[] }) {
  // Backend already orders prestige-then-result; filtering by tour preserves that order per section.
  const proEvents = events.filter((e) => e.tourTier !== "DEVELOPMENT");
  const devEvents = events.filter((e) => e.tourTier === "DEVELOPMENT");

  return (
    <div className="flex flex-col gap-6">
      {proEvents.length > 0 ? (
        <TourSection
          title="Pro Tour"
          subtitle="Majors and signatures first — your marquee events."
          events={proEvents}
          defaultOpen
        />
      ) : null}
      {devEvents.length > 0 ? (
        <TourSection
          title="Development Tour"
          subtitle="Where the career began."
          events={devEvents}
          // Collapsed by default once there are pro events, so the record book leads with the headline tour.
          defaultOpen={proEvents.length === 0}
        />
      ) : null}
    </div>
  );
}

function TourSection({
  title,
  subtitle,
  events,
  defaultOpen,
}: {
  title: string;
  subtitle: string;
  events: EventHistory[];
  defaultOpen: boolean;
}) {
  const [open, setOpen] = useState(defaultOpen);
  const reduce = useReducedMotion();

  return (
    <section className="flex flex-col gap-3">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
        className="group flex items-center gap-3 text-left"
      >
        <h2 className="text-base font-bold tracking-[-0.015em]">{title}</h2>
        <span className="text-subtle-foreground font-mono text-xs tabular-nums">{events.length}</span>
        <span className="text-subtle-foreground group-hover:text-muted-foreground hidden text-sm sm:inline">
          {subtitle}
        </span>
        <ChevronDown
          className={`text-subtle-foreground ml-auto size-5 shrink-0 transition-transform duration-[var(--duration-base)] ${open ? "rotate-180" : ""}`}
          aria-hidden="true"
        />
      </button>
      {open ? (
        <ul className="flex flex-col gap-3">
          {events.map((event, i) => (
            <EventRow key={event.eventName} event={event} index={i} reduce={!!reduce} />
          ))}
        </ul>
      ) : null}
    </section>
  );
}

function EventRow({ event, index, reduce }: { event: EventHistory; index: number; reduce: boolean }) {
  const [open, setOpen] = useState(false);
  const [showAllWins, setShowAllWins] = useState(false);
  const decorated = event.wins > 0;
  const isMajor = event.prestige === "MAJOR";
  const topResults = event.results.slice(0, 3);
  const victories = event.results.filter((r) => r.won);

  return (
    <motion.li
      initial={reduce ? false : { opacity: 0, y: 8 }}
      whileInView={reduce ? undefined : { opacity: 1, y: 0 }}
      viewport={{ once: true, margin: "-40px" }}
      transition={{ duration: 0.25, ease: "easeOut", delay: Math.min(index * 0.03, 0.15) }}
      className={[
        "overflow-hidden rounded-xl border",
        decorated ? "border-gold/25 bg-gold/[0.05]" : "border-border bg-surface",
      ].join(" ")}
    >
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
        className="hover:bg-surface-elevated/60 flex w-full items-center gap-4 px-5 py-4 text-left transition-colors"
      >
        <span
          className={[
            "grid size-10 shrink-0 place-items-center rounded-full",
            decorated ? "bg-gold/15 text-gold" : "bg-surface-3 text-subtle-foreground",
          ].join(" ")}
          aria-hidden="true"
        >
          {decorated ? <Trophy className="size-5" /> : <Medal className="size-5" />}
        </span>

        <div className="flex min-w-0 flex-1 flex-col gap-1">
          <span className={`truncate font-semibold ${isMajor ? "text-gold" : ""}`}>{event.eventName}</span>
          <span className="text-subtle-foreground flex items-center gap-1.5 text-xs">
            <MapPin className="size-3" aria-hidden="true" />
            <span className="truncate">{event.location}</span>
            <span aria-hidden="true">·</span>
            <span className="tracking-[0.06em] uppercase">{eventPrestigeLabel(event.prestige)}</span>
          </span>
        </div>

        <div className="flex shrink-0 items-center gap-4">
          <div className="text-right">
            {event.wins > 0 ? (
              <span className="gold-metal-text text-lg font-bold tabular-nums">
                {event.wins}×<span className="text-sm font-semibold"> win{event.wins > 1 ? "s" : ""}</span>
              </span>
            ) : (
              <span className="text-foreground text-lg font-bold tabular-nums">
                {ordinalPosition(event.bestPosition)}
              </span>
            )}
            <p className="text-subtle-foreground text-[0.7rem] tabular-nums">
              {event.appearances} {event.appearances === 1 ? "appearance" : "appearances"}
            </p>
          </div>
          <ChevronDown
            className={`text-subtle-foreground size-5 shrink-0 transition-transform duration-[var(--duration-base)] ${open ? "rotate-180" : ""}`}
            aria-hidden="true"
          />
        </div>
      </button>

      {open ? (
        <div className="border-border flex flex-col gap-5 border-t px-5 py-5">
          {/* Top results — the best the player has done in this event. */}
          <div className="flex flex-col gap-2.5">
            <SubLabel>Best results</SubLabel>
            <ul className="flex flex-col gap-1.5">
              {topResults.map((r, i) => (
                <ResultLine key={i} result={r} />
              ))}
            </ul>
          </div>

          {/* Victory deep-dive — every win, with the full detail of each. */}
          {victories.length > 0 ? (
            <div className="flex flex-col gap-2.5">
              <div className="flex items-center justify-between gap-3">
                <SubLabel>
                  <span className="text-gold">Victories · {victories.length}</span>
                </SubLabel>
                {victories.length > 1 ? (
                  <button
                    type="button"
                    onClick={() => setShowAllWins((v) => !v)}
                    aria-expanded={showAllWins}
                    className="text-primary hover:text-primary/80 text-xs font-semibold"
                  >
                    {showAllWins ? "Show less" : `View all ${victories.length} wins`}
                  </button>
                ) : null}
              </div>
              <div className="flex flex-col gap-2.5">
                {(showAllWins ? victories : victories.slice(0, 1)).map((r, i) => (
                  <VictoryCard key={i} result={r} />
                ))}
              </div>
            </div>
          ) : null}
        </div>
      ) : null}
    </motion.li>
  );
}

/** A compact best-result row: placement, score, and when. */
function ResultLine({ result }: { result: Result }) {
  return (
    <li className="flex items-center gap-3 text-sm">
      <span className={`w-12 shrink-0 font-bold tabular-nums ${result.won ? "text-gold" : "text-foreground"}`}>
        {ordinalPosition(result.position)}
      </span>
      <span className="text-muted-foreground w-14 shrink-0 font-mono tabular-nums">
        {formatScore(result.scoreToPar)}
      </span>
      <span className="text-subtle-foreground truncate text-xs">
        Season {result.season} · {formatEventDate(result.date)}
      </span>
    </li>
  );
}

/** A full victory record: when, where, the winning score, round card, and performance metrics. */
function VictoryCard({ result }: { result: Result }) {
  return (
    <div className="border-gold/20 bg-gold/[0.04] flex flex-col gap-3 rounded-lg border p-4">
      <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
        <span className="text-gold inline-flex items-center gap-2 font-semibold">
          <Trophy className="size-4" aria-hidden="true" />
          Season {result.season}
          <span className="text-subtle-foreground text-xs font-normal">{formatEventDate(result.date)}</span>
        </span>
        <span className="font-mono text-lg font-bold tabular-nums">{formatScore(result.scoreToPar)}</span>
      </div>

      <div className="text-subtle-foreground flex items-center gap-1.5 text-xs">
        <MapPin className="size-3" aria-hidden="true" />
        {result.location}
        {result.prize > 0 ? (
          <>
            <span aria-hidden="true">·</span>
            <span className="text-primary">{formatMoney(result.prize)}</span>
          </>
        ) : null}
      </div>

      {result.roundScores.length > 0 ? (
        <div className="flex items-center gap-2">
          <span className="text-subtle-foreground text-[0.65rem] font-semibold tracking-[0.1em] uppercase">
            Rounds
          </span>
          <div className="flex flex-wrap gap-1.5">
            {result.roundScores.map((r, i) => (
              <span
                key={i}
                className="border-border bg-surface inline-flex min-w-8 justify-center rounded-md border px-2 py-1 font-mono text-xs font-semibold tabular-nums"
              >
                {formatScore(r)}
              </span>
            ))}
          </div>
        </div>
      ) : null}

      {result.holesPlayed > 0 ? (
        <div className="grid grid-cols-3 gap-3 border-t border-[var(--border)] pt-3">
          <Metric label="Fairways" value={`${result.fairwaysHit}/${result.fairwaysPossible}`} />
          <Metric label="Greens" value={`${result.greensInRegulation}/${result.holesPlayed}`} />
          <Metric label="Putts" value={result.putts} />
        </div>
      ) : null}
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="flex flex-col gap-0.5">
      <span className="text-base font-bold tabular-nums">{value}</span>
      <span className="text-subtle-foreground text-[0.6rem] font-semibold tracking-[0.1em] uppercase">{label}</span>
    </div>
  );
}

/* ------------------------------------------------------------------ */

function SectionLabel({ children }: { children: React.ReactNode }) {
  return (
    <h2 className="text-subtle-foreground text-[0.6875rem] font-bold tracking-[0.14em] uppercase">{children}</h2>
  );
}

function SubLabel({ children }: { children: React.ReactNode }) {
  return (
    <span className="text-subtle-foreground text-[0.65rem] font-bold tracking-[0.12em] uppercase">{children}</span>
  );
}
