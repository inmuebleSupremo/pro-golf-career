"use client";

import { SpokeShell, SpokeEmpty, useSpokeGate } from "@/components/career/spoke";
import { usePlayerProfile, useWorldRankings } from "@/lib/api/queries";

export type RankingRow = {
  position: number;
  golferId: string;
  name: string;
  rankingValue: number;
};

/** Rankings spoke: the current World Ranking board, the player's own row highlighted. */
export function RankingsView({ id }: { id: string }) {
  const query = useWorldRankings(id, 100);
  const playerGolferId = usePlayerProfile(id).data?.playerProfile?.golferId ?? null;
  const gate = useSpokeGate(query);
  if (gate) return gate;

  const rows: RankingRow[] = query.data?.worldRankings ?? [];

  return (
    <SpokeShell title="World Ranking" description="The top of the professional game.">
      {rows.length === 0 ? (
        <SpokeEmpty>The ranking is empty — no events have been ranked yet.</SpokeEmpty>
      ) : (
        <RankingList rows={rows} playerGolferId={playerGolferId} />
      )}
    </SpokeShell>
  );
}

/** The divided ranking list with a caption header. Shared by the rankings + rivals spokes. */
export function RankingList({
  rows,
  playerGolferId,
}: {
  rows: RankingRow[];
  playerGolferId: string | null;
}) {
  return (
    <div className="flex flex-col gap-2">
      <div className="text-subtle-foreground flex items-center gap-4 px-5 text-xs tracking-[0.08em] uppercase">
        <span className="w-8 shrink-0">#</span>
        <span className="flex-1">Player</span>
        <span className="shrink-0">Points</span>
      </div>
      <ul className="divide-divider border-border bg-surface flex flex-col divide-y overflow-hidden rounded-lg border">
        {rows.map((row) => (
          <RankingRowItem
            key={row.golferId}
            row={row}
            isPlayer={playerGolferId != null && row.golferId === playerGolferId}
          />
        ))}
      </ul>
    </div>
  );
}

function RankingRowItem({ row, isPlayer }: { row: RankingRow; isPlayer: boolean }) {
  return (
    <li className={`flex items-center gap-4 px-5 py-3.5 ${isPlayer ? "bg-primary/[0.06]" : ""}`}>
      <span className="text-subtle-foreground w-8 shrink-0 font-mono text-sm tabular-nums">
        {row.position}
      </span>
      <span className="flex min-w-0 flex-1 items-center gap-2">
        <span className={`truncate font-medium ${isPlayer ? "text-primary" : ""}`}>{row.name}</span>
        {isPlayer ? <span className="text-subtle-foreground text-xs">(you)</span> : null}
      </span>
      <span className="text-subtle-foreground shrink-0 font-mono text-sm tabular-nums">
        {Math.round(row.rankingValue)}
      </span>
    </li>
  );
}
