"use client";

import { SpokeShell, SpokeEmpty, useSpokeGate } from "@/components/career/spoke";
import { useCareerOverview, usePlayerProfile } from "@/lib/api/queries";

type NewsItem = {
  season: number;
  type: string;
  headline: string;
  prominence: number;
  subjectGolferId: string | null;
};

/** Tour-news spoke: the world's recent headlines, the player's own items highlighted. */
export function NewsView({ id }: { id: string }) {
  const query = useCareerOverview(id);
  const profile = usePlayerProfile(id).data?.playerProfile ?? null;
  const gate = useSpokeGate(query);
  if (gate) return gate;

  const news: NewsItem[] = query.data?.newsFeed ?? [];
  const playerGolferId = profile?.golferId ?? null;

  return (
    <SpokeShell title="Tour news" description="What’s happening across the tour.">
      {news.length === 0 ? (
        <SpokeEmpty>No headlines yet — the season is young.</SpokeEmpty>
      ) : (
        <ul className="divide-divider border-border bg-surface flex flex-col divide-y overflow-hidden rounded-lg border">
          {news.map((item, i) => {
            const isPlayer = playerGolferId != null && item.subjectGolferId === playerGolferId;
            return (
              <li
                key={`${item.season}-${i}`}
                className={`flex items-center justify-between gap-4 px-5 py-3 ${isPlayer ? "bg-primary/[0.06]" : ""}`}
              >
                <span className="flex min-w-0 items-center gap-2 text-sm">
                  {isPlayer ? (
                    <span className="bg-primary size-1.5 shrink-0 rounded-full" aria-hidden="true" />
                  ) : null}
                  <span className={`truncate ${isPlayer ? "text-foreground font-medium" : "text-foreground"}`}>
                    {item.headline}
                  </span>
                </span>
                <span className="text-subtle-foreground shrink-0 font-mono text-xs tabular-nums">
                  S{item.season}
                </span>
              </li>
            );
          })}
        </ul>
      )}
    </SpokeShell>
  );
}
