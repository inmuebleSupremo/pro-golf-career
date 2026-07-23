"use client";

import { useEffect, useState } from "react";
import { ArrowUp, Check, Coins, Minus, Plus, Sparkles } from "lucide-react";

import { Button } from "@/components/ui/button";
import { isNotFound, isUnauthorized } from "@/lib/api/graphql-client";
import { useSpendDevelopmentPoints } from "@/lib/api/manage";
import { useDevelopmentReport, usePlayerDevelopment, usePlayerProfile } from "@/lib/api/queries";
import { humanize } from "@/lib/play/options";

type Attribute = { attribute: string; value: number; potential: number };
type CostCurve = { pointsPerRating: number; costGrowth: number; costReference: number };
type Delta = { attribute: string; delta: number };

const GROUPS: { label: string; attributes: string[] }[] = [
  { label: "Tee", attributes: ["DRIVING_ACCURACY", "DRIVING_DISTANCE"] },
  { label: "Approach", attributes: ["IRONS_ACCURACY", "IRONS_CONTROL"] },
  { label: "Short Game", attributes: ["WEDGES", "PUTTING_ACCURACY", "PUTTING_PROXIMITY"] },
  { label: "Mental", attributes: ["COMPOSURE", "COURSE_MANAGEMENT"] },
];

/** The Development-Point cost of the next point at a given rating. Mirrors the engine's cost curve. */
function stepCost(rating: number, c: CostCurve): number {
  return Math.round(c.pointsPerRating * (1 + Math.max(0, rating - c.costReference) * c.costGrowth));
}

/** Total cost of raising an attribute from `from` by `points` (clamped to `potential`). */
function raiseCost(from: number, points: number, potential: number, c: CostCurve): number {
  let total = 0;
  let rating = from;
  for (let i = 0; i < points && rating < potential; i++) {
    total += stepCost(rating, c);
    rating += 1;
  }
  return total;
}

/**
 * Development: you earn Development Points from your play and spend them here to raise your attributes.
 * Higher ratings cost more (the cost curve), and nothing rises past its potential.
 */
export function TrainingSection({ id, onUnauthorized }: { id: string; onUnauthorized: () => void }) {
  const { data, isPending, isError, error } = usePlayerProfile(id);
  const dev = usePlayerDevelopment(id).data?.playerDevelopment ?? null;

  useEffect(() => {
    if (isError && isUnauthorized(error)) onUnauthorized();
  }, [isError, error, onUnauthorized]);

  const profile = data?.playerProfile ?? null;

  if (isPending) return <SectionSkeleton />;
  if (isError || !profile) {
    return (
      <SectionNote>
        {isNotFound(error)
          ? "This session is no longer available. Reopen the career from your saves."
          : "Couldn't load your golfer. Try again."}
      </SectionNote>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <DevelopmentReport id={id} />
      <SpendScreen
        id={id}
        attributes={profile.attributes}
        balance={dev?.points ?? 0}
        curve={dev}
      />
    </div>
  );
}

function SpendScreen({
  id,
  attributes,
  balance,
  curve,
}: {
  id: string;
  attributes: Attribute[];
  balance: number;
  curve: CostCurve | null;
}) {
  const byName = new Map(attributes.map((a) => [a.attribute, a]));
  const spend = useSpendDevelopmentPoints(id);
  const [pending, setPending] = useState<Record<string, number>>({});

  const spent = curve
    ? attributes.reduce((sum, a) => sum + raiseCost(a.value, pending[a.attribute] ?? 0, a.potential, curve), 0)
    : 0;
  const remaining = balance - spent;
  const totalPoints = Object.values(pending).reduce((s, n) => s + n, 0);

  function increment(attr: Attribute) {
    if (!curve) return;
    const added = pending[attr.attribute] ?? 0;
    const displayed = attr.value + added;
    if (displayed >= attr.potential) return;
    if (remaining < stepCost(displayed, curve)) return;
    spend.reset();
    setPending((p) => ({ ...p, [attr.attribute]: added + 1 }));
  }

  function decrement(name: string) {
    spend.reset();
    setPending((p) => {
      const next = { ...p };
      if ((next[name] ?? 0) <= 1) delete next[name];
      else next[name] = next[name] - 1;
      return next;
    });
  }

  function apply() {
    const raises = Object.entries(pending)
      .filter(([, points]) => points > 0)
      .map(([attribute, points]) => ({ attribute, points }));
    if (raises.length === 0) return;
    spend.mutate(raises, { onSuccess: () => setPending({}) });
  }

  return (
    <section className="flex flex-col gap-5">
      <div className="border-border from-surface-elevated to-surface flex flex-wrap items-center justify-between gap-4 rounded-xl border bg-gradient-to-b p-5 shadow-[var(--shadow-md)]">
        <div className="flex items-center gap-3">
          <span className="bg-gold/10 text-gold grid size-11 shrink-0 place-items-center rounded-xl">
            <Coins className="size-6" aria-hidden="true" />
          </span>
          <div>
            <p className="text-2xl font-bold tabular-nums tracking-[-0.02em]">
              {remaining}
              <span className="text-muted-foreground ml-1.5 text-sm font-normal">
                Development Points
              </span>
            </p>
            <p className="text-muted-foreground text-sm">
              Earned from your play — spend them to raise your stats.
            </p>
          </div>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          {spend.isSuccess ? (
            <span className="text-success inline-flex items-center gap-1.5 text-sm font-medium">
              <Check className="size-4" aria-hidden="true" />
              Applied
            </span>
          ) : null}
          {spend.isError ? <span className="text-destructive text-sm">Couldn’t apply — try again.</span> : null}
          {totalPoints > 0 ? (
            <Button variant="ghost" onClick={() => setPending({})} disabled={spend.isPending}>
              Reset
            </Button>
          ) : null}
          <Button onClick={apply} disabled={totalPoints === 0 || spend.isPending}>
            {spend.isPending ? "Applying…" : totalPoints > 0 ? `Apply +${totalPoints}` : "Apply"}
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        {GROUPS.map((group) => (
          <div key={group.label} className="border-border bg-surface flex flex-col gap-3 rounded-xl border p-4">
            <h3 className="text-subtle-foreground text-xs font-bold tracking-[0.12em] uppercase">
              {group.label}
            </h3>
            <div className="flex flex-col gap-3">
              {group.attributes.map((name) => {
                const attr = byName.get(name);
                if (!attr) return null;
                const added = pending[name] ?? 0;
                const displayed = attr.value + added;
                const atCeiling = displayed >= attr.potential;
                const nextCost = curve && !atCeiling ? stepCost(displayed, curve) : null;
                const canBuy = curve != null && !atCeiling && remaining >= (nextCost ?? Infinity);
                return (
                  <AttributeRow
                    key={name}
                    label={humanize(name)}
                    value={attr.value}
                    added={added}
                    potential={attr.potential}
                    nextCost={nextCost}
                    canBuy={canBuy}
                    onInc={() => increment(attr)}
                    onDec={() => decrement(name)}
                  />
                );
              })}
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}

function AttributeRow({
  label,
  value,
  added,
  potential,
  nextCost,
  canBuy,
  onInc,
  onDec,
}: {
  label: string;
  value: number;
  added: number;
  potential: number;
  nextCost: number | null;
  canBuy: boolean;
  onInc: () => void;
  onDec: () => void;
}) {
  const displayed = value + added;
  return (
    <div className="flex flex-col gap-1.5">
      <div className="flex items-center justify-between gap-2">
        <span className="text-sm font-medium">{label}</span>
        <div className="flex items-center gap-2">
          <span className="font-mono text-sm tabular-nums">
            <span className="text-foreground">{displayed}</span>
            {added > 0 ? <span className="text-success"> (+{added})</span> : null}
            <span className="text-subtle-foreground"> / {potential}</span>
          </span>
          <div className="flex items-center gap-1">
            <IconBtn label={`Lower ${label}`} onClick={onDec} disabled={added === 0}>
              <Minus className="size-3.5" aria-hidden="true" />
            </IconBtn>
            <IconBtn label={`Raise ${label}`} onClick={onInc} disabled={!canBuy}>
              <Plus className="size-3.5" aria-hidden="true" />
            </IconBtn>
          </div>
        </div>
      </div>
      <div className="bg-divider relative h-2 w-full overflow-hidden rounded-full">
        <div className="bg-primary/20 absolute inset-y-0 left-0 rounded-full" style={{ width: `${potential}%` }} />
        <div className="bg-success/70 absolute inset-y-0 left-0 rounded-full" style={{ width: `${displayed}%` }} />
        <div className="bg-primary absolute inset-y-0 left-0 rounded-full" style={{ width: `${value}%` }} />
      </div>
      <span className="text-subtle-foreground text-[0.6875rem] tabular-nums">
        {nextCost != null ? `Next point: ${nextCost} DP` : "At potential"}
      </span>
    </div>
  );
}

function IconBtn({
  label,
  onClick,
  disabled,
  children,
}: {
  label: string;
  onClick: () => void;
  disabled: boolean;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-label={label}
      className="border-border text-foreground hover:bg-surface-elevated grid size-7 place-items-center rounded-md border transition-colors disabled:cursor-not-allowed disabled:opacity-40"
    >
      {children}
    </button>
  );
}

function DevelopmentReport({ id }: { id: string }) {
  const report = (useDevelopmentReport(id).data?.developmentReport ?? []) as Delta[];
  if (report.length === 0) return null;
  const gains = [...report].sort((a, b) => b.delta - a.delta);

  return (
    <section className="border-primary/30 bg-primary/[0.05] flex flex-col gap-3 rounded-xl border p-5">
      <div className="flex items-center gap-2">
        <Sparkles className="text-primary size-4" aria-hidden="true" />
        <h2 className="text-base font-bold tracking-[-0.01em]">Recently developed</h2>
      </div>
      <ul className="grid grid-cols-1 gap-x-6 gap-y-2 sm:grid-cols-2">
        {gains.map((gain) => (
          <li key={gain.attribute} className="flex items-center justify-between gap-3">
            <span className="text-sm">{humanize(gain.attribute)}</span>
            <span className="text-success inline-flex items-center gap-0.5 font-mono text-sm font-semibold tabular-nums">
              <ArrowUp className="size-3.5" aria-hidden="true" />
              {gain.delta}
            </span>
          </li>
        ))}
      </ul>
    </section>
  );
}

function SectionNote({ children }: { children: React.ReactNode }) {
  return (
    <p className="border-border bg-surface text-muted-foreground rounded-lg border border-dashed px-5 py-8 text-center text-sm">
      {children}
    </p>
  );
}

function SectionSkeleton() {
  return <div aria-hidden="true" className="border-border bg-surface h-64 animate-pulse rounded-lg border" />;
}
