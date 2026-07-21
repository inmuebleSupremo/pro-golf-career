import { archetypeLabel, nationalityLabel } from "@/lib/onboarding/options";
import { formatMoney, tourTierLabel } from "@/lib/career/labels";

interface Profile {
  firstName: string;
  lastName: string;
  nationality: string;
  age: number;
  archetype: string;
  worldRanking: number | null;
  careerEarnings: number;
  tour: string | null;
  wins: number;
}

/** The player's golfer identity: name, key facts, and headline stats. Attributes render separately. */
export function GolferHeader({ profile }: { profile: Profile }) {
  return (
    <section className="border-border bg-surface flex flex-wrap items-start justify-between gap-4 rounded-lg border p-6">
      <div className="flex flex-col gap-1.5">
        <h1 className="font-serif text-3xl font-medium tracking-[-0.02em]">
          {profile.firstName} {profile.lastName}
        </h1>
        <p className="text-muted-foreground text-sm">
          {nationalityLabel(profile.nationality)} · {archetypeLabel(profile.archetype)} · age{" "}
          {profile.age}
          {profile.tour ? ` · ${tourTierLabel(profile.tour)} tour` : ""}
        </p>
      </div>
      <div className="flex gap-6">
        <HeaderStat label="World rank" value={profile.worldRanking ? `#${profile.worldRanking}` : "—"} />
        <HeaderStat label="Earnings" value={formatMoney(profile.careerEarnings)} />
        <HeaderStat label="Wins" value={String(profile.wins)} />
      </div>
    </section>
  );
}

function HeaderStat({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-1">
      <span className="text-subtle-foreground text-xs tracking-[0.08em] uppercase">{label}</span>
      <span className="text-foreground font-mono text-lg tabular-nums">{value}</span>
    </div>
  );
}
