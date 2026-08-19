"use client";

import { useState } from "react";
import { ChevronDown, Crown, MapPin, Medal, Target, Trophy } from "lucide-react";

import { Card, CardHeader } from "@/components/command/card";
import { SpokeShell, SpokeEmpty, useSpokeGate } from "@/components/career/spoke";
import { useCareerRecords } from "@/lib/api/queries";
import { eventPrestigeLabel, formatMoney, formatScore, ordinalPosition } from "@/lib/career/labels";

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
  lowestRoundToPar: number | null;
  lowestTournamentToPar: number | null;
  careerEarnings: number;
};

/** A short calendar date from the ISO string the API stores (e.g. "6 Apr 2000"). */
function formatEventDate(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric", timeZone: "UTC" });
}

/**
 * Career Records — the player-specific counterpart to the world Record Book. A bento of headline totals and
 * scoring bests over a browsable, expandable per-event history: each event reveals its best results, and any
 * recurring event opens into the full timeline of every victory (the historical-dominance deep-dive).
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
            <span className="text-gold font-semibold">{summary.wins}</span> wins ·{" "}
            {summary.events} events
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
        <StatTile
          label="Career Earnings"
          value={formatMoney(summary.careerEarnings)}
          accent="primary"
        />
        <StatTile
          label="Lowest Round"
          value={summary.lowestRoundToPar != null ? formatScore(summary.lowestRoundToPar) : "—"}
          icon={<Target className="size-4" aria-hidden="true" />}
        />
        <StatTile
          label="Lowest Tournament"
          value={summary.lowestTournamentToPar != null ? formatScore(summary.lowestTournamentToPar) : "—"}
        />
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

/* ------------------------------------------------------------------ */
/* Per-event history (expandable)                                      */
/* ------------------------------------------------------------------ */

function EventHistorySection({ events }: { events: EventHistory[] }) {
  return (
    <section className="flex flex-col gap-3">
      <SectionLabel>Events · {events.length}</SectionLabel>
      <p className="text-muted-foreground -mt-1 text-sm">
        Every tournament you&apos;ve teed up in. Open one for your best results there — and any you&apos;ve won
        for the full run of victories.
      </p>
      <ul className="flex flex-col gap-3">
        {events.map((event) => (
          <EventRow key={event.eventName} event={event} />
        ))}
      </ul>
    </section>
  );
}

function EventRow({ event }: { event: EventHistory }) {
  const [open, setOpen] = useState(false);
  const [showAllWins, setShowAllWins] = useState(false);
  const decorated = event.wins > 0;
  const isMajor = event.prestige === "MAJOR";
  const topResults = event.results.slice(0, 3);
  const victories = event.results.filter((r) => r.won);

  return (
    <li
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
            <span className="uppercase tracking-[0.06em]">{eventPrestigeLabel(event.prestige)}</span>
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
    </li>
  );
}

/** A compact best-result row: placement, score, and when. */
function ResultLine({ result }: { result: Result }) {
  return (
    <li className="flex items-center gap-3 text-sm">
      <span
        className={`w-12 shrink-0 font-bold tabular-nums ${result.won ? "text-gold" : "text-foreground"}`}
      >
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
      <span className="text-subtle-foreground text-[0.6rem] font-semibold tracking-[0.1em] uppercase">
        {label}
      </span>
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
