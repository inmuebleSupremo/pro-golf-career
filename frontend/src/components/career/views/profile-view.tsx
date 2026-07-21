"use client";

import { AttributeRadar } from "@/components/career/attribute-radar";
import { GolferHeader } from "@/components/career/golfer-header";
import { SpokeShell, SpokeEmpty, useSpokeGate } from "@/components/career/spoke";
import { usePlayerProfile } from "@/lib/api/queries";
import { attributeShortLabel, tourTierLabel } from "@/lib/career/labels";

type Attribute = { attribute: string; value: number };

/** The player's golfer profile — identity, an attribute radar, and a career-record strip. */
export function ProfileView({ id }: { id: string }) {
  const query = usePlayerProfile(id);
  const gate = useSpokeGate(query);
  if (gate) return gate;

  const profile = query.data?.playerProfile ?? null;

  if (!profile) {
    return (
      <SpokeShell title="Profile">
        <SpokeEmpty>No golfer is assigned to this career.</SpokeEmpty>
      </SpokeShell>
    );
  }

  return (
    <SpokeShell title="Profile" description="Your golfer’s identity, form, and record.">
      <GolferHeader profile={profile} />
      <AttributesCard attributes={profile.attributes} />
      <ul className="divide-divider border-border bg-surface flex flex-col divide-y overflow-hidden rounded-lg border sm:flex-row sm:divide-x sm:divide-y-0">
        <RecordStat label="Tour" value={profile.tour ? tourTierLabel(profile.tour) : "—"} />
        <RecordStat label="Events" value={String(profile.events)} />
        <RecordStat label="Wins" value={String(profile.wins)} />
        <RecordStat label="Top 10s" value={String(profile.topTens)} />
        <RecordStat label="Age" value={String(profile.age)} />
      </ul>
    </SpokeShell>
  );
}

function AttributesCard({ attributes }: { attributes: Attribute[] }) {
  const overall =
    attributes.length > 0
      ? (attributes.reduce((sum, a) => sum + a.value, 0) / attributes.length).toFixed(1)
      : "—";

  return (
    <section className="border-border from-surface-elevated to-surface rounded-xl border bg-gradient-to-b p-6 shadow-[var(--shadow-md)]">
      <div className="mb-2 flex items-center justify-between gap-3">
        <h2 className="text-base font-bold tracking-[-0.01em]">Attributes</h2>
        <span className="text-subtle-foreground text-xs tabular-nums">
          Overall <span className="text-foreground font-semibold">{overall}</span>
        </span>
      </div>
      <div className="grid items-center gap-x-8 gap-y-6 md:grid-cols-[minmax(0,320px)_1fr]">
        <div className="flex justify-center px-2 py-2">
          <AttributeRadar attributes={attributes} />
        </div>
        <ul className="grid grid-cols-1 gap-x-8 sm:grid-cols-2">
          {attributes.map((a) => (
            <li
              key={a.attribute}
              className="border-divider flex items-center justify-between gap-3 border-b py-2 last:border-b-0 sm:[&:nth-last-child(2)]:border-b-0"
            >
              <span className="text-muted-foreground text-sm">{attributeShortLabel(a.attribute)}</span>
              <span className="font-mono text-sm font-semibold tabular-nums">{a.value}</span>
            </li>
          ))}
        </ul>
      </div>
    </section>
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
