"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { ArrowDownRight, ArrowRight, ArrowUpRight, Sparkles, Trophy } from "lucide-react";

import { Button } from "@/components/ui/button";
import { SpokeMessage, useSpokeGate } from "@/components/career/spoke";
import { useCareerInbox, useCareerOverview, useSeasonReview } from "@/lib/api/queries";
import { formatMoney, ordinalPosition } from "@/lib/career/labels";
import { humanize } from "@/lib/play/options";

type SeasonStat = {
  season: number;
  events: number;
  wins: number;
  topTens: number;
  cuts: number;
  bestFinish: number;
  earnings: number;
};
type Delta = { attribute: string; delta: number; season: number };
type Headline = {
  season: number;
  type: string;
  headline: string;
  prominence: number;
  subjectGolferId: string | null;
};
type Review = {
  season: number;
  rankStart: number | null;
  rankEnd: number | null;
  playerGolferId: string;
  stats: SeasonStat;
  development: Delta[];
  headlines: Headline[];
};

/**
 * The off-season moment (spec: player-experience — the end-of-season beat). Reached when the player
 * crosses a season boundary. It first *reviews* the season just completed — the golfer's own season and
 * the wider tour's. Any current career decisions remain in their authoritative management sections and are
 * surfaced together in the Inbox after this retrospective review.
 */
export function OffSeasonReview({ id }: { id: string }) {
  const router = useRouter();
  const query = useSeasonReview(id);
  const inbox = useCareerInbox(id);
  const nextSeason = useCareerOverview(id).data?.world?.season ?? null;

  // The off-season review is a blocking moment: crossing the season boundary has already advanced the
  // world, so the review covers the command shell and locks body scroll while it's up — the sidebar and
  // identity strip can't be reached behind it. The only way forward is to continue into the season, so the
  // player can never navigate on and silently find themselves a year later. Mirrors the
  // immersive Play Mode surface (spec: player-experience — the end-of-season beat).
  useEffect(() => {
    const previous = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = previous;
    };
  }, []);

  const reviewGate = useSpokeGate(query);
  const inboxGate = useSpokeGate(inbox);
  const gate = reviewGate ?? inboxGate;
  if (gate) return <OffSeasonShell>{gate}</OffSeasonShell>;

  const review = (query.data?.seasonReview as Review | null) ?? null;

  // No completed season yet (a brand-new career): nothing to review — the message offers its own way out.
  if (!review) {
    return (
      <OffSeasonShell>
        <SpokeMessage
          title="No season to review yet"
          body="Play through a full season and its review will be waiting here at the year's turn."
        />
      </OffSeasonShell>
    );
  }

  const player = review.playerGolferId;
  const yours = review.headlines.filter((h) => h.subjectGolferId === player);
  const tour = review.headlines.filter((h) => h.subjectGolferId !== player);
  const achievements = yours.filter((h) => h.type === "ACHIEVEMENT_UNLOCKED");

  const inboxCount = inbox.data?.careerInbox.items.length ?? 0;

  function continueCareer() {
    router.push(inboxCount > 0 ? `/career/${id}/inbox` : `/career/${id}`);
    router.refresh();
  }

  return (
    <OffSeasonShell>
      <Hero season={review.season} stats={review.stats} onContinue={continueCareer} />

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <YourSeason
          review={review}
          achievements={achievements}
          highlights={yours.filter((h) => h.type !== "ACHIEVEMENT_UNLOCKED")}
        />
        <AroundTheTour headlines={tour} />
      </div>

      <BeginBar
        season={review.season}
        nextSeason={nextSeason}
        inboxCount={inboxCount}
        onContinue={continueCareer}
      />
    </OffSeasonShell>
  );
}

/**
 * The blocking full-viewport surface the review lives in. It sits above the command shell (z-modal, like
 * Play Mode) so the sidebar/identity strip behind it are covered and unclickable, and it owns its own
 * vertical scroll so a tall review still reads. Inherits the command-centre tokens from its DOM ancestor.
 */
function OffSeasonShell({ children }: { children: React.ReactNode }) {
  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Off-season review"
      className="bg-background fixed inset-0 z-[var(--z-modal)] overflow-y-auto"
    >
      <div className="mx-auto flex w-full max-w-4xl flex-col gap-10 px-5 py-8 md:px-7 md:py-12">
        {children}
      </div>
    </div>
  );
}

/* ---------------------------------------------------------------- Hero */

function Hero({
  season,
  stats,
  onContinue,
}: {
  season: number;
  stats: SeasonStat;
  onContinue: () => void;
}) {
  const line =
    stats.events === 0
      ? "A season on the sidelines."
      : stats.wins > 0
        ? `${stats.wins} ${stats.wins === 1 ? "win" : "wins"} across ${stats.events} events.`
        : `Best finish ${ordinalPosition(stats.bestFinish)} across ${stats.events} events.`;

  return (
    <header className="border-border from-surface-elevated to-surface relative overflow-hidden rounded-2xl border bg-gradient-to-b p-8 text-center shadow-[var(--shadow-md)]">
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 top-0 h-40 bg-[radial-gradient(120%_140%_at_50%_0%,color-mix(in_oklch,var(--gold),transparent_88%),transparent_60%)]"
      />
      <p className="text-subtle-foreground relative font-mono text-xs tracking-[0.2em] uppercase">
        Off-Season
      </p>
      <h1 className="relative mt-3 font-serif text-4xl font-medium tracking-[-0.02em] sm:text-5xl">
        Season {season}
        <span className="text-muted-foreground"> in review</span>
      </h1>
      <p className="text-muted-foreground relative mt-3 text-base">{line}</p>
      <div className="relative mt-6 flex justify-center">
        <Button size="lg" onClick={onContinue}>
          Continue
          <ArrowRight className="size-4" aria-hidden="true" />
        </Button>
      </div>
    </header>
  );
}

/* -------------------------------------------------------- Your season */

function YourSeason({
  review,
  achievements,
  highlights,
}: {
  review: Review;
  achievements: Headline[];
  highlights: Headline[];
}) {
  const s = review.stats;
  const gains = [...review.development].sort((a, b) => b.delta - a.delta);

  return (
    <Panel title="Your season" accent="you">
      <RankMovement start={review.rankStart} end={review.rankEnd} />

      <div className="grid grid-cols-3 gap-2">
        <Figure k="Events" v={s.events} />
        <Figure k="Wins" v={s.wins} accent={s.wins > 0} />
        <Figure k="Top 10s" v={s.topTens} />
        <Figure k="Cuts" v={s.cuts} />
        <Figure k="Best" v={s.events > 0 ? ordinalPosition(s.bestFinish) : "—"} />
        <Figure k="Earned" v={formatMoney(s.earnings)} />
      </div>

      {achievements.length > 0 ? (
        <div className="flex flex-col gap-2">
          {achievements.map((g, i) => (
            <div
              key={i}
              className="gold-metal flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium text-[#3c2f12]"
            >
              <Trophy className="size-4 shrink-0" aria-hidden="true" />
              <span className="min-w-0 truncate">{g.headline}</span>
            </div>
          ))}
        </div>
      ) : null}

      {gains.length > 0 ? (
        <div className="flex flex-col gap-2.5">
          <h3 className="text-subtle-foreground flex items-center gap-1.5 text-xs font-bold tracking-[0.12em] uppercase">
            <Sparkles className="text-info size-3.5" aria-hidden="true" />
            Development
          </h3>
          <ul className="grid grid-cols-2 gap-x-5 gap-y-1.5">
            {gains.map((g) => (
              <li key={g.attribute} className="flex items-center justify-between gap-2 text-sm">
                <span className="text-muted-foreground truncate">{humanize(g.attribute)}</span>
                <span className="text-success inline-flex items-center gap-0.5 font-mono text-sm font-semibold tabular-nums">
                  <ArrowUpRight className="size-3.5" aria-hidden="true" />
                  {g.delta}
                </span>
              </li>
            ))}
          </ul>
        </div>
      ) : null}

      {highlights.length > 0 ? (
        <div className="border-divider flex flex-col gap-2 border-t pt-4">
          {highlights.slice(0, 4).map((h, i) => (
            <p key={i} className="text-muted-foreground text-sm leading-snug">
              {h.headline}
            </p>
          ))}
        </div>
      ) : null}
    </Panel>
  );
}

function RankMovement({ start, end }: { start: number | null; end: number | null }) {
  if (end == null) {
    return (
      <div className="border-border bg-surface flex items-center justify-between rounded-lg border px-4 py-3">
        <span className="text-muted-foreground text-sm">World ranking</span>
        <span className="text-subtle-foreground text-sm">Unranked</span>
      </div>
    );
  }

  // A lower position number is better, so an improvement is a decrease.
  const moved = start != null ? start - end : null;
  const improved = moved != null && moved > 0;

  return (
    <div className="border-border bg-surface flex items-center justify-between rounded-lg border px-4 py-3">
      <span className="text-muted-foreground text-sm">World ranking</span>
      <span className="flex items-center gap-2.5 font-mono tabular-nums">
        {start != null ? (
          <>
            <span className="text-subtle-foreground text-sm">#{start}</span>
            <ArrowRight className="text-subtle-foreground size-3.5" aria-hidden="true" />
          </>
        ) : (
          <span className="text-subtle-foreground text-xs">new</span>
        )}
        <span className="text-info text-lg font-bold">#{end}</span>
        {moved != null && moved !== 0 ? (
          <span
            className={`inline-flex items-center gap-0.5 text-xs font-semibold ${
              improved ? "text-success" : "text-destructive"
            }`}
          >
            {improved ? (
              <ArrowUpRight className="size-3.5" aria-hidden="true" />
            ) : (
              <ArrowDownRight className="size-3.5" aria-hidden="true" />
            )}
            {Math.abs(moved)}
          </span>
        ) : null}
      </span>
    </div>
  );
}

/* ---------------------------------------------------- Around the tour */

function AroundTheTour({ headlines }: { headlines: Headline[] }) {
  const top = headlines.slice(0, 8);
  return (
    <Panel title="Around the tour" accent="world">
      {top.length === 0 ? (
        <p className="text-muted-foreground text-sm">A quiet season across the tour.</p>
      ) : (
        <ul className="flex flex-col">
          {top.map((h, i) => (
            <li
              key={i}
              className="border-divider flex gap-3 border-t py-3 first:border-t-0 first:pt-0"
            >
              <NewsChip type={h.type} />
              <p className="text-foreground min-w-0 text-sm leading-snug">{h.headline}</p>
            </li>
          ))}
        </ul>
      )}
    </Panel>
  );
}

const NEWS_TONE: Record<string, { label: string; cls: string }> = {
  TOURNAMENT_VICTORY: { label: "Win", cls: "gold-metal text-[#3c2f12]" },
  MAJOR_VICTORY: { label: "Major", cls: "gold-metal text-[#3c2f12]" },
  HALL_OF_FAME: { label: "HoF", cls: "gold-metal text-[#3c2f12]" },
  WORLD_NUMBER_ONE: { label: "No. 1", cls: "bg-info/12 text-info" },
  PROMOTION: { label: "Tour", cls: "bg-info/12 text-info" },
  RISING_PROSPECT: { label: "Prospect", cls: "bg-info/12 text-info" },
  MAJOR_UPSET: { label: "Upset", cls: "bg-info/12 text-info" },
  RETIREMENT: { label: "Retires", cls: "bg-surface-3 text-muted-foreground" },
  INJURY: { label: "Injury", cls: "bg-destructive/14 text-destructive" },
};

function NewsChip({ type }: { type: string }) {
  const tone = NEWS_TONE[type] ?? {
    label: type.replace(/_/g, " ").toLowerCase(),
    cls: "bg-surface-3 text-muted-foreground",
  };
  return (
    <span
      className={`mt-px h-fit shrink-0 rounded-md px-1.5 py-1 text-[0.6rem] font-bold tracking-wide uppercase ${tone.cls}`}
    >
      {tone.label}
    </span>
  );
}

/* --------------------------------------------------------- Begin bar */

function BeginBar({
  season,
  nextSeason,
  inboxCount,
  onContinue,
}: {
  season: number;
  nextSeason: number | null;
  inboxCount: number;
  onContinue: () => void;
}) {
  return (
    <div className="border-border bg-surface/60 sticky bottom-4 z-[var(--z-sticky)] flex flex-wrap items-center justify-between gap-4 rounded-xl border px-5 py-4 backdrop-blur-md">
      <div className="flex flex-col">
        <span className="font-medium">Ready for Season {nextSeason ?? season + 1}?</span>
        <span className="text-muted-foreground text-sm">
          {inboxCount > 0
            ? `${inboxCount} career ${inboxCount === 1 ? "item needs" : "items need"} your attention.`
            : "You can always manage your career from the hub."}
        </span>
      </div>
      <Button size="lg" onClick={onContinue}>
        Continue
        <ArrowRight className="size-4" aria-hidden="true" />
      </Button>
    </div>
  );
}

/* ----------------------------------------------------------- Shared */

function Panel({
  title,
  accent,
  children,
}: {
  title: string;
  accent: "you" | "world";
  children: React.ReactNode;
}) {
  const bar = accent === "you" ? "bg-success" : "bg-info";
  return (
    <section className="border-border bg-surface flex flex-col gap-4 rounded-xl border p-5 sm:p-6">
      <div className="flex items-center gap-2.5">
        <span className={`h-4 w-1 rounded-full ${bar}`} aria-hidden="true" />
        <h2 className="text-lg font-bold tracking-[-0.01em]">{title}</h2>
      </div>
      {children}
    </section>
  );
}

function Figure({ k, v, accent }: { k: string; v: string | number; accent?: boolean }) {
  return (
    <div className="border-border bg-background rounded-lg border p-3 text-center">
      <div
        className={`text-xl font-bold tracking-[-0.02em] tabular-nums ${accent ? "text-primary" : ""}`}
      >
        {v}
      </div>
      <div className="text-subtle-foreground mt-1 text-[0.65rem] font-semibold tracking-wide uppercase">
        {k}
      </div>
    </div>
  );
}
