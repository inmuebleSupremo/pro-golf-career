"use client";

import { SpokeShell, SpokeEmpty, useSpokeGate } from "@/components/career/spoke";
import { useCareerOverview, usePlayerProfile } from "@/lib/api/queries";
import { formatMoney } from "@/lib/career/labels";

type SeasonStat = {
  season: number;
  events: number;
  wins: number;
  earnings: number;
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
