"use client";

import { RankingList, type RankingRow } from "@/components/career/views/rankings-view";
import { SpokeShell, SpokeEmpty, useSpokeGate } from "@/components/career/spoke";
import { usePlayerProfile, useWorldRankings } from "@/lib/api/queries";

// How many golfers to show on either side of the player in the ranking.
const WINDOW = 5;

/** Rivals spoke: the golfers ranked immediately above and below you. */
export function RivalsView({ id }: { id: string }) {
  // Fetch a deep slice so the player's window is present even when they rank well down the list.
  const query = useWorldRankings(id, 500);
  const profile = usePlayerProfile(id).data?.playerProfile ?? null;
  const gate = useSpokeGate(query);
  if (gate) return gate;

  const rows: RankingRow[] = query.data?.worldRankings ?? [];
  const playerGolferId = profile?.golferId ?? null;
  const index = playerGolferId ? rows.findIndex((r) => r.golferId === playerGolferId) : -1;
  const window = index >= 0 ? rows.slice(Math.max(0, index - WINDOW), index + WINDOW + 1) : [];

  return (
    <SpokeShell title="Rivals" description="The players either side of you in the ranking.">
      {index < 0 ? (
        <SpokeEmpty>
          You’re not in the ranked field yet — play some events to climb the World Ranking and your
          rivals will appear here.
        </SpokeEmpty>
      ) : (
        <RankingList rows={window} playerGolferId={playerGolferId} />
      )}
    </SpokeShell>
  );
}
