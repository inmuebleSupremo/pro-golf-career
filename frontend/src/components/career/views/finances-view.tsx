"use client";

import { useState } from "react";

import { Button } from "@/components/ui/button";
import { SpokeShell, SpokeEmpty, useSpokeGate } from "@/components/career/spoke";
import {
  useAcceptSponsorship,
  usePendingSponsorships,
  useSponsorshipStatus,
} from "@/lib/api/manage";
import { useCareerOverview, usePlayerProfile } from "@/lib/api/queries";
import { formatMoney } from "@/lib/career/labels";

type SeasonStat = {
  season: number;
  events: number;
  wins: number;
  earnings: number;
};
type Offer = {
  sponsor: string;
  industry: string;
  perSeasonPayment: number;
  signingBonus: number;
  durationSeasons: number;
  grossValue: number;
};
type Active = {
  sponsor: string;
  industry: string;
  perSeasonPayment: number;
  seasonsRemaining: number;
};

/** Finances spoke: available funds, career earnings, and a per-season earnings ledger. */
export function FinancesView({ id }: { id: string }) {
  const query = usePlayerProfile(id);
  const overview = useCareerOverview(id);
  const gate = useSpokeGate(query);
  if (gate) return gate;

  const profile = query.data?.playerProfile ?? null;
  const seasons: SeasonStat[] = overview.data?.playerSeasonStats ?? [];

  if (!profile) {
    return (
      <SpokeShell title="Finances">
        <SpokeEmpty>No golfer is assigned to this career.</SpokeEmpty>
      </SpokeShell>
    );
  }

  return (
    <SpokeShell title="Finances" description="What you’ve earned and what you can spend.">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <FigureCard label="Available funds" value={formatMoney(profile.availableFunds)} accent />
        <FigureCard label="Career earnings" value={formatMoney(profile.careerEarnings)} />
      </div>

      <Sponsorships id={id} />

      <section className="flex flex-col gap-4">
        <h2 className="text-muted-foreground text-[0.6875rem] font-bold tracking-[0.12em] uppercase">
          Earnings by season
        </h2>
        {seasons.length === 0 ? (
          <SpokeEmpty>No prize money yet — earnings appear once you’ve played.</SpokeEmpty>
        ) : (
          <ul className="divide-divider border-border bg-surface flex flex-col divide-y overflow-hidden rounded-lg border">
            {[...seasons].reverse().map((s) => (
              <li key={s.season} className="flex items-center justify-between gap-4 px-5 py-4">
                <div className="flex min-w-0 flex-col gap-1">
                  <span className="font-medium">Season {s.season}</span>
                  <span className="text-muted-foreground text-sm">
                    {s.events} {s.events === 1 ? "event" : "events"} · {s.wins}{" "}
                    {s.wins === 1 ? "win" : "wins"}
                  </span>
                </div>
                <span className="text-foreground shrink-0 font-mono tabular-nums">
                  {formatMoney(s.earnings)}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </SpokeShell>
  );
}

function FigureCard({ label, value, accent }: { label: string; value: string; accent?: boolean }) {
  return (
    <div className="border-border from-surface-elevated to-surface flex flex-col gap-2 rounded-xl border bg-gradient-to-b p-5 shadow-[var(--shadow-md)]">
      <span className="text-muted-foreground text-[0.6875rem] font-bold tracking-[0.12em] uppercase">
        {label}
      </span>
      <span
        className={`text-[2rem] leading-none font-bold tabular-nums tracking-[-0.03em] ${accent ? "text-info" : ""}`}
      >
        {value}
      </span>
    </div>
  );
}

/** Pending endorsement offers — signing them pays a bonus and per-season income. */
function Sponsorships({ id }: { id: string }) {
  const offersQuery = usePendingSponsorships(id);
  const statusQuery = useSponsorshipStatus(id);
  const accept = useAcceptSponsorship(id);
  const [blocked, setBlocked] = useState(false);

  const offers: Offer[] = offersQuery.data?.pendingSponsorships ?? [];
  const status = statusQuery.data?.sponsorshipStatus ?? null;
  const active: Active[] = status?.active ?? [];
  const max = status?.maxConcurrent ?? 0;
  const full = max > 0 && active.length >= max;

  // Secondary to the figures above — stay quiet on error and when there's nothing to show.
  if (offersQuery.isError || (active.length === 0 && offers.length === 0)) return null;

  async function onAccept(index: number) {
    setBlocked(false);
    const result = await accept.mutateAsync(index);
    // The backend refuses a sign when the book is full (returns false) rather than doing nothing silently.
    if (!result.acceptSponsorship) setBlocked(true);
  }

  return (
    <section className="flex flex-col gap-4">
      <div className="flex items-baseline justify-between gap-3">
        <h2 className="text-muted-foreground text-[0.6875rem] font-bold tracking-[0.12em] uppercase">
          Sponsorships
        </h2>
        {max > 0 ? (
          <span className="text-subtle-foreground text-xs font-medium tabular-nums">
            {active.length} of {max}
          </span>
        ) : null}
      </div>

      {active.length > 0 ? (
        <ul className="divide-divider border-border bg-surface flex flex-col divide-y overflow-hidden rounded-lg border">
          {active.map((deal, i) => (
            <li key={`${deal.sponsor}:${i}`} className="flex items-center justify-between gap-3 px-4 py-3">
              <span className="flex min-w-0 items-center gap-2">
                <span className="truncate font-medium">{deal.sponsor}</span>
                <span className="text-subtle-foreground shrink-0 text-[0.7rem] tracking-[0.04em] uppercase">
                  {deal.industry}
                </span>
              </span>
              <span className="text-muted-foreground shrink-0 text-sm">
                <span className="text-foreground font-mono tabular-nums">
                  {formatMoney(deal.perSeasonPayment)}
                </span>
                /yr ·{" "}
                <span className="tabular-nums">
                  {deal.seasonsRemaining} {deal.seasonsRemaining === 1 ? "season" : "seasons"} left
                </span>
              </span>
            </li>
          ))}
        </ul>
      ) : null}

      {offers.length > 0 ? (
        <div className="flex flex-col gap-3">
          <div className="flex items-baseline justify-between gap-3">
            <h3 className="text-subtle-foreground text-[0.6875rem] font-bold tracking-[0.12em] uppercase">
              Offers
            </h3>
            {full ? (
              <span className="text-warning text-xs font-medium">
                Book full — a deal must expire to sign another
              </span>
            ) : null}
          </div>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            {offers.map((offer, index) => (
              <div
                key={`${offer.sponsor}:${index}`}
                className={`border-border bg-surface flex flex-col gap-3 rounded-lg border px-4 py-3 ${
                  full ? "opacity-60" : ""
                }`}
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="flex min-w-0 flex-col gap-0.5">
                    <span className="flex items-center gap-2">
                      <span className="truncate font-medium">{offer.sponsor}</span>
                      <span className="text-subtle-foreground shrink-0 text-[0.7rem] tracking-[0.04em] uppercase">
                        {offer.industry}
                      </span>
                    </span>
                    <span className="text-muted-foreground text-sm">
                      {offer.durationSeasons} {offer.durationSeasons === 1 ? "season" : "seasons"} ·{" "}
                      <span className="text-foreground font-mono tabular-nums">
                        {formatMoney(offer.grossValue)}
                      </span>{" "}
                      total
                    </span>
                  </div>
                  <Button
                    size="sm"
                    onClick={() => onAccept(index)}
                    disabled={accept.isPending || full}
                    title={full ? "Your sponsorship book is full" : undefined}
                    className="shrink-0"
                  >
                    {full ? "Full" : "Accept"}
                  </Button>
                </div>
                <dl className="grid grid-cols-2 gap-2">
                  <OfferStat label="Signing bonus" value={formatMoney(offer.signingBonus)} />
                  <OfferStat label="Per season" value={formatMoney(offer.perSeasonPayment)} />
                </dl>
              </div>
            ))}
          </div>
          {blocked ? (
            <p role="alert" className="text-warning text-sm">
              Couldn&apos;t sign — your sponsorship book is full. Wait for a deal to expire.
            </p>
          ) : null}
        </div>
      ) : null}
    </section>
  );
}

function OfferStat({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-0.5">
      <dt className="text-subtle-foreground text-xs tracking-[0.08em] uppercase">{label}</dt>
      <dd className="text-foreground font-mono text-sm tabular-nums">{value}</dd>
    </div>
  );
}
