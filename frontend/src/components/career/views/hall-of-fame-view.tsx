"use client";

import { SpokeShell, SpokeEmpty, useSpokeGate } from "@/components/career/spoke";
import { useHallOfFame, usePlayerProfile } from "@/lib/api/queries";

type Induction = {
  golferId: string;
  name: string;
  season: number;
  score: number;
  careerWins: number;
};

/** Hall-of-Fame spoke: the world's inductions, the player's own highlighted. */
export function HallOfFameView({ id }: { id: string }) {
  const query = useHallOfFame(id);
  const profile = usePlayerProfile(id).data?.playerProfile ?? null;
  const gate = useSpokeGate(query);
  if (gate) return gate;

  const inductions: Induction[] = query.data?.hallOfFame ?? [];
  const playerGolferId = profile?.golferId ?? null;

  return (
    <SpokeShell title="Hall of Fame" description="The game’s immortals.">
      {inductions.length === 0 ? (
        <SpokeEmpty>No inductees yet — legends are still being made.</SpokeEmpty>
      ) : (
        <ul className="divide-divider border-border bg-surface flex flex-col divide-y overflow-hidden rounded-lg border">
          {inductions.map((induction) => {
            const isPlayer = playerGolferId != null && induction.golferId === playerGolferId;
            return (
              <li
                key={induction.golferId}
                className={`flex items-center justify-between gap-4 px-5 py-4 ${isPlayer ? "bg-primary/[0.06]" : ""}`}
              >
                <div className="flex min-w-0 flex-col gap-1">
                  <span className="flex items-center gap-2">
                    <span className={`truncate font-medium ${isPlayer ? "text-primary" : ""}`}>
                      {induction.name}
                    </span>
                    {isPlayer ? (
                      <span className="text-subtle-foreground text-xs">(you)</span>
                    ) : null}
                  </span>
                  <span className="text-muted-foreground text-sm">
                    Inducted Season {induction.season} · {induction.careerWins}{" "}
                    {induction.careerWins === 1 ? "win" : "wins"}
                  </span>
                </div>
                <span className="text-subtle-foreground shrink-0 font-mono text-sm tabular-nums">
                  {Math.round(induction.score)}
                </span>
              </li>
            );
          })}
        </ul>
      )}
    </SpokeShell>
  );
}
