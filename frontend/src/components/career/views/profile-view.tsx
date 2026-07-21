"use client";

import { GolferHeader } from "@/components/career/golfer-header";
import { SpokeShell, SpokeEmpty, useSpokeGate } from "@/components/career/spoke";
import { usePlayerProfile } from "@/lib/api/queries";
import { tourTierLabel } from "@/lib/career/labels";

/** The player's golfer profile — reuses the hub's GolferHeader plus a career-record strip. */
export function ProfileView({ id }: { id: string }) {
  const query = usePlayerProfile(id);
  const gate = useSpokeGate(query);
  if (gate) return gate;

  const profile = query.data?.playerProfile ?? null;

  return (
    <SpokeShell title="Profile" description="Your golfer’s identity, form, and record.">
      {profile ? (
        <>
          <GolferHeader profile={profile} />
          <ul className="divide-divider border-border bg-surface flex flex-col divide-y overflow-hidden rounded-lg border sm:flex-row sm:divide-x sm:divide-y-0">
            <RecordStat label="Tour" value={profile.tour ? tourTierLabel(profile.tour) : "—"} />
            <RecordStat label="Events" value={String(profile.events)} />
            <RecordStat label="Wins" value={String(profile.wins)} />
            <RecordStat label="Top 10s" value={String(profile.topTens)} />
            <RecordStat label="Age" value={String(profile.age)} />
          </ul>
        </>
      ) : (
        <SpokeEmpty>No golfer is assigned to this career.</SpokeEmpty>
      )}
    </SpokeShell>
  );
}

function RecordStat({ label, value }: { label: string; value: string }) {
  return (
    <li className="flex flex-1 flex-col gap-1 px-5 py-4">
      <span className="text-subtle-foreground text-xs tracking-[0.08em] uppercase">{label}</span>
      <span className="text-foreground font-mono text-lg tabular-nums">{value}</span>
    </li>
  );
}
