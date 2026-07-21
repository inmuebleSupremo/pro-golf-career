"use client";

import { SpokeShell, SpokeEmpty, useSpokeGate } from "@/components/career/spoke";
import { useCareerOverview } from "@/lib/api/queries";
import { formatMoney, ordinalPosition } from "@/lib/career/labels";

type SeasonStat = {
  season: number;
  events: number;
  wins: number;
  topTens: number;
  cuts: number;
  bestFinish: number;
  earnings: number;
};

/** Season-stats spoke: one row per season competed, most recent first. */
export function SeasonsView({ id }: { id: string }) {
  const query = useCareerOverview(id);
  const gate = useSpokeGate(query);
  if (gate) return gate;

  const seasons: SeasonStat[] = query.data?.playerSeasonStats ?? [];

  return (
    <SpokeShell title="Season stats" description="Your record, season by season.">
      {seasons.length === 0 ? (
        <SpokeEmpty>No completed seasons yet.</SpokeEmpty>
      ) : (
        <div className="border-border bg-surface overflow-x-auto rounded-lg border">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-subtle-foreground border-divider border-b text-xs tracking-[0.08em] uppercase">
                <th className="px-4 py-3 text-left font-medium">Season</th>
                <NumHead>Events</NumHead>
                <NumHead>Wins</NumHead>
                <NumHead>Top 10s</NumHead>
                <NumHead>Cuts</NumHead>
                <NumHead>Best</NumHead>
                <NumHead>Earnings</NumHead>
              </tr>
            </thead>
            <tbody className="divide-divider divide-y">
              {[...seasons].reverse().map((s) => (
                <tr key={s.season}>
                  <td className="px-4 py-3 font-medium">Season {s.season}</td>
                  <NumCell>{s.events}</NumCell>
                  <NumCell>{s.wins}</NumCell>
                  <NumCell>{s.topTens}</NumCell>
                  <NumCell>{s.cuts}</NumCell>
                  <NumCell>{s.events > 0 ? ordinalPosition(s.bestFinish) : "—"}</NumCell>
                  <NumCell>{formatMoney(s.earnings)}</NumCell>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </SpokeShell>
  );
}

function NumHead({ children }: { children: React.ReactNode }) {
  return <th className="px-4 py-3 text-right font-medium">{children}</th>;
}

function NumCell({ children }: { children: React.ReactNode }) {
  return <td className="text-foreground px-4 py-3 text-right font-mono tabular-nums">{children}</td>;
}
