"use client";

import { SpokeShell, SpokeEmpty, useSpokeGate } from "@/components/career/spoke";
import { usePlayerProfile, useWorldRankings } from "@/lib/api/queries";

export type RankingRow = {
  position: number;
  golferId: string;
  name: string;
  rankingValue: number;
};

const TOP_COUNT = 15;

/** Rankings spoke: the top of the World Ranking, with the player's own row anchored below if outside it. */
export function RankingsView({ id }: { id: string }) {
  // Fetch a deep slice so the player's real row (with points) is present even when ranked well down.
  const query = useWorldRankings(id, 100);
  const profile = usePlayerProfile(id).data?.playerProfile ?? null;
  const gate = useSpokeGate(query);
  if (gate) return gate;

  const rows: RankingRow[] = query.data?.worldRankings ?? [];
  const playerGolferId = profile?.golferId ?? null;
  const top = rows.slice(0, TOP_COUNT);
  const inTop = playerGolferId != null && top.some((r) => r.golferId === playerGolferId);

  // When the player sits outside the top, anchor their row below a divider (leaderboard style).
  let trailing: RankingRow | null = null;
  if (!inTop && playerGolferId != null) {
    const found = rows.find((r) => r.golferId === playerGolferId);
    if (found) {
      trailing = found;
    } else if (profile?.worldRanking != null) {
      // Ranked beyond the fetched slice — show position + name from the profile (points unknown here).
      trailing = {
        position: profile.worldRanking,
        golferId: playerGolferId,
        name: `${profile.firstName} ${profile.lastName}`,
        rankingValue: Number.NaN,
      };
    }
  }

  return (
    <SpokeShell title="World Ranking" description="The top of the professional game.">
      {rows.length === 0 ? (
        <SpokeEmpty>The ranking is empty — no events have been ranked yet.</SpokeEmpty>
      ) : (
        <RankingList rows={top} playerGolferId={playerGolferId} trailing={trailing} />
      )}
    </SpokeShell>
  );
}

/**
 * The divided ranking list with a caption header. Shared by the rankings + rivals spokes.
 * `trailing` appends the player's row below a divider (used when they're outside the shown rows).
 */
export function RankingList({
  rows,
  playerGolferId,
  trailing = null,
}: {
  rows: RankingRow[];
  playerGolferId: string | null;
  trailing?: RankingRow | null;
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
        {trailing ? (
          <>
            <li className="text-subtle-foreground px-5 py-1 text-center text-xs" aria-hidden="true">
              ···
            </li>
            <RankingRowItem row={trailing} isPlayer />
          </>
        ) : null}
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
        {Number.isFinite(row.rankingValue) ? Math.round(row.rankingValue) : "—"}
      </span>
    </li>
  );
}
