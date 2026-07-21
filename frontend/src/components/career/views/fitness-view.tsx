"use client";

import { Activity, HeartPulse, ShieldAlert } from "lucide-react";

import { SpokeShell, SpokeEmpty, useSpokeGate } from "@/components/career/spoke";
import { usePlayerFitness } from "@/lib/api/queries";
import { humanize } from "@/lib/play/options";

type Injury = { type: string; severity: string; rehabWeeksRemaining: number } | null;
type Fitness = {
  availability: string;
  fitness: number;
  fatigue: number;
  canCompete: boolean;
  canPlayThroughInjury: boolean;
  injury: Injury;
};

const AVAILABILITY: Record<string, { label: string; note: string; tone: string }> = {
  AVAILABLE: {
    label: "Available",
    note: "Fit to enter events unimpaired.",
    tone: "bg-success/[0.14] text-success",
  },
  RESTING: {
    label: "Resting",
    note: "Too fatigued to compete — a forced rest until recovered.",
    tone: "bg-info/[0.14] text-info",
  },
  RECOVERING: {
    label: "Recovering",
    note: "Late-stage rehab — can play through at a shot impairment.",
    tone: "gold-metal text-[#3c2f12]",
  },
  INJURED: {
    label: "Injured",
    note: "Unable to compete until rehabilitation progresses.",
    tone: "bg-destructive/[0.16] text-destructive",
  },
};

/** Fitness spoke: availability, condition, fatigue, and any active injury. */
export function FitnessView({ id }: { id: string }) {
  const query = usePlayerFitness(id);
  const gate = useSpokeGate(query);
  if (gate) return gate;

  const fitness = (query.data?.playerFitness ?? null) as Fitness | null;

  if (!fitness) {
    return (
      <SpokeShell title="Fitness">
        <SpokeEmpty>No fitness data — this career has no active golfer.</SpokeEmpty>
      </SpokeShell>
    );
  }

  const availability = AVAILABILITY[fitness.availability] ?? {
    label: humanize(fitness.availability),
    note: "",
    tone: "bg-surface-3 text-muted-foreground",
  };

  return (
    <SpokeShell title="Fitness" description="Your golfer’s condition and readiness to compete.">
      <div className="border-border from-surface-elevated to-surface flex flex-col gap-4 rounded-xl border bg-gradient-to-b p-5 shadow-[var(--shadow-md)]">
        <div className="flex flex-wrap items-center gap-3">
          <span
            className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-bold tracking-wide uppercase ${availability.tone}`}
          >
            <Activity className="size-3.5" aria-hidden="true" />
            {availability.label}
          </span>
          {availability.note ? (
            <span className="text-muted-foreground text-sm">{availability.note}</span>
          ) : null}
        </div>
        <div className="grid grid-cols-1 gap-x-8 gap-y-4 sm:grid-cols-2">
          <Meter label="Condition" value={fitness.fitness} icon={HeartPulse} />
          <Meter label="Fatigue" value={fitness.fatigue} icon={Activity} inverse />
        </div>
      </div>

      {fitness.injury ? (
        <InjuryCard injury={fitness.injury} canPlayThrough={fitness.canPlayThroughInjury} />
      ) : (
        <p className="border-border bg-surface text-muted-foreground flex items-center justify-center gap-2 rounded-lg border border-dashed px-5 py-6 text-sm">
          <HeartPulse className="text-success size-4" aria-hidden="true" />
          No active injury — fully healthy.
        </p>
      )}
    </SpokeShell>
  );
}

/** A 0–1 meter. `inverse` tints toward the info colour (high = worse) rather than success. */
function Meter({
  label,
  value,
  icon: Icon,
  inverse,
}: {
  label: string;
  value: number;
  icon: typeof Activity;
  inverse?: boolean;
}) {
  const pct = Math.round(Math.min(1, Math.max(0, value)) * 100);
  return (
    <div className="flex flex-col gap-1.5">
      <div className="flex items-center justify-between gap-2">
        <span className="text-muted-foreground inline-flex items-center gap-1.5 text-sm">
          <Icon className="size-3.5" aria-hidden="true" />
          {label}
        </span>
        <span className="font-mono text-sm font-semibold tabular-nums">{pct}%</span>
      </div>
      <div className="bg-surface-3 h-2 w-full overflow-hidden rounded-full">
        <div
          className={`h-full rounded-full ${inverse ? "bg-info" : "bg-success"}`}
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  );
}

function InjuryCard({
  injury,
  canPlayThrough,
}: {
  injury: { type: string; severity: string; rehabWeeksRemaining: number };
  canPlayThrough: boolean;
}) {
  return (
    <section className="border-destructive/30 bg-destructive/[0.06] flex flex-col gap-4 rounded-lg border p-5">
      <div className="flex items-center gap-2">
        <ShieldAlert className="text-destructive size-4" aria-hidden="true" />
        <h2 className="font-medium">Active injury</h2>
      </div>
      <ul className="divide-divider border-border bg-surface flex flex-col divide-y overflow-hidden rounded-lg border sm:flex-row sm:divide-x sm:divide-y-0">
        <InjuryStat label="Area" value={humanize(injury.type)} />
        <InjuryStat label="Severity" value={humanize(injury.severity)} />
        <InjuryStat
          label="Rehab left"
          value={`${injury.rehabWeeksRemaining} ${injury.rehabWeeksRemaining === 1 ? "week" : "weeks"}`}
        />
      </ul>
      <p className="text-muted-foreground text-sm">
        {canPlayThrough
          ? "You can choose to play through this injury, but shots are impaired until it heals."
          : "Rehabilitation must progress further before you can play through this injury."}
      </p>
    </section>
  );
}

function InjuryStat({ label, value }: { label: string; value: string }) {
  return (
    <li className="flex flex-1 flex-col gap-1 px-5 py-4">
      <span className="text-subtle-foreground text-xs tracking-[0.08em] uppercase">{label}</span>
      <span className="text-foreground font-mono text-lg tabular-nums">{value}</span>
    </li>
  );
}
