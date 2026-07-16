import { archetypeLabel, nationalityLabel } from "@/lib/onboarding/options";
import { formatMoney, tourTierLabel } from "@/lib/career/labels";
import { humanize } from "@/lib/play/options";

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
  attributes: Array<{ attribute: string; value: number }>;
}

/** The player's golfer identity: name, key facts, headline stats, and attribute spread. */
export function GolferHeader({ profile }: { profile: Profile }) {
  return (
    <section className="border-border bg-surface flex flex-col gap-6 rounded-lg border p-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
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
          <HeaderStat
            label="World rank"
            value={profile.worldRanking ? `#${profile.worldRanking}` : "—"}
          />
          <HeaderStat label="Earnings" value={formatMoney(profile.careerEarnings)} />
          <HeaderStat label="Wins" value={String(profile.wins)} />
        </div>
      </div>
      <div className="grid grid-cols-2 gap-x-6 gap-y-3 sm:grid-cols-3">
        {profile.attributes.map((a) => (
          <AttributeBar key={a.attribute} name={humanize(a.attribute)} value={a.value} />
        ))}
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

function AttributeBar({ name, value }: { name: string; value: number }) {
  return (
    <div className="flex flex-col gap-1.5">
      <div className="flex items-baseline justify-between gap-2">
        <span className="text-muted-foreground truncate text-sm">{name}</span>
        <span className="text-subtle-foreground font-mono text-xs tabular-nums">{value}</span>
      </div>
      <div className="bg-divider h-1.5 w-full overflow-hidden rounded-full">
        <div className="bg-primary h-full rounded-full" style={{ width: `${value}%` }} />
      </div>
    </div>
  );
}
