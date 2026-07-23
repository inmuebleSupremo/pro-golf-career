"use client";

import { useEffect, useRef, useState } from "react";
import { ArrowUp, Check, Sparkles } from "lucide-react";

import { Button } from "@/components/ui/button";
import { isNotFound, isUnauthorized } from "@/lib/api/graphql-client";
import { useSetDevelopmentFocus } from "@/lib/api/manage";
import { useDevelopmentReport, usePlayerDevelopmentFocus, usePlayerProfile } from "@/lib/api/queries";
import { humanize } from "@/lib/play/options";

type Attribute = { attribute: string; value: number; potential: number };
type Delta = { attribute: string; delta: number; season: number };

/** The four skill areas the nine attributes group into, for a low-cognitive-load training focus. */
const GROUPS: { label: string; attributes: string[] }[] = [
  { label: "Tee", attributes: ["DRIVING_ACCURACY", "DRIVING_DISTANCE"] },
  { label: "Approach", attributes: ["IRONS_ACCURACY", "IRONS_CONTROL"] },
  { label: "Short Game", attributes: ["WEDGES", "PUTTING_ACCURACY", "PUTTING_PROXIMITY"] },
  { label: "Mental", attributes: ["COMPOSURE", "COURSE_MANAGEMENT"] },
];

/**
 * Training focus: pick the areas to develop this season (no ranking — a simple set). At season end the
 * engine pours that season's Development Points into the focused attributes, toward each one's potential.
 * The Development Report shows what last season's play actually developed.
 */
export function TrainingSection({ id, onUnauthorized }: { id: string; onUnauthorized: () => void }) {
  const { data, isPending, isError, error } = usePlayerProfile(id);

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
    <div className="flex flex-col gap-8">
      <DevelopmentReport id={id} />
      <FocusPicker id={id} attributes={profile.attributes} />
    </div>
  );
}

function FocusPicker({ id, attributes }: { id: string; attributes: Attribute[] }) {
  const byName = new Map(attributes.map((a) => [a.attribute, a]));
  const currentFocus = usePlayerDevelopmentFocus(id).data?.playerDevelopmentFocus;
  const save = useSetDevelopmentFocus(id);

  const [selected, setSelected] = useState<Set<string>>(new Set());
  const initialised = useRef(false);
  useEffect(() => {
    if (!initialised.current && currentFocus) {
      setSelected(new Set(currentFocus));
      initialised.current = true;
    }
  }, [currentFocus]);

  function toggle(name: string) {
    save.reset();
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(name)) next.delete(name);
      else next.add(name);
      return next;
    });
  }

  function onSave() {
    // No ranking: send the selected set ordered weakest-first, so the engine develops the neediest
    // focused attribute first as its points spill toward each ceiling.
    const ordered = [...selected].sort((a, b) => (byName.get(a)?.value ?? 0) - (byName.get(b)?.value ?? 0));
    save.mutate(ordered);
  }

  return (
    <section className="flex flex-col gap-4">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h2 className="text-base font-bold tracking-[-0.01em]">Training focus</h2>
        <p className="text-muted-foreground text-sm">
          {selected.size === 0
            ? "No focus set — development spreads across all areas."
            : `Focusing ${selected.size} ${selected.size === 1 ? "attribute" : "attributes"} — a narrower focus develops faster.`}
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        {GROUPS.map((group) => (
          <div key={group.label} className="border-border bg-surface flex flex-col gap-3 rounded-xl border p-4">
            <h3 className="text-subtle-foreground text-xs font-bold tracking-[0.12em] uppercase">
              {group.label}
            </h3>
            <div className="flex flex-col gap-2">
              {group.attributes.map((name) => {
                const attr = byName.get(name);
                if (!attr) return null;
                return (
                  <AttributeRow
                    key={name}
                    attribute={attr}
                    selected={selected.has(name)}
                    onToggle={() => toggle(name)}
                  />
                );
              })}
            </div>
          </div>
        ))}
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <Button onClick={onSave} disabled={save.isPending}>
          {save.isPending ? "Saving…" : "Save focus"}
        </Button>
        {selected.size > 0 ? (
          <Button variant="ghost" onClick={() => setSelected(new Set())} disabled={save.isPending}>
            Clear
          </Button>
        ) : null}
        {save.isSuccess ? (
          <span className="text-success inline-flex items-center gap-1.5 text-sm font-medium">
            <Check className="size-4" aria-hidden="true" />
            Focus saved
          </span>
        ) : null}
        {save.isError ? <span className="text-destructive text-sm">Couldn’t save — try again.</span> : null}
      </div>
    </section>
  );
}

function AttributeRow({
  attribute,
  selected,
  onToggle,
}: {
  attribute: Attribute;
  selected: boolean;
  onToggle: () => void;
}) {
  const headroom = attribute.potential - attribute.value;
  return (
    <button
      type="button"
      onClick={onToggle}
      aria-pressed={selected}
      className={`flex flex-col gap-1.5 rounded-lg border px-3 py-2.5 text-left transition-colors ${
        selected ? "border-primary bg-primary/[0.06]" : "border-border bg-background hover:bg-surface"
      }`}
    >
      <div className="flex items-center justify-between gap-2">
        <span className="flex items-center gap-1.5 text-sm font-medium">
          <span
            className={`grid size-4 shrink-0 place-items-center rounded border ${
              selected ? "border-primary bg-primary text-primary-foreground" : "border-border"
            }`}
            aria-hidden="true"
          >
            {selected ? <Check className="size-3" /> : null}
          </span>
          {humanize(attribute.attribute)}
        </span>
        <span className="text-subtle-foreground font-mono text-xs tabular-nums">
          <span className="text-foreground">{attribute.value}</span> / {attribute.potential}
        </span>
      </div>
      <PotentialBar value={attribute.value} potential={attribute.potential} />
      {headroom > 0 ? (
        <span className="text-subtle-foreground text-[0.6875rem]">{headroom} to develop</span>
      ) : (
        <span className="text-subtle-foreground text-[0.6875rem]">At potential</span>
      )}
    </button>
  );
}

/** A track showing the current value (solid) within the attribute's potential ceiling (faint). */
function PotentialBar({ value, potential }: { value: number; potential: number }) {
  return (
    <div className="bg-divider relative h-1.5 w-full overflow-hidden rounded-full">
      <div className="bg-primary/25 absolute inset-y-0 left-0 rounded-full" style={{ width: `${potential}%` }} />
      <div className="bg-primary absolute inset-y-0 left-0 rounded-full" style={{ width: `${value}%` }} />
    </div>
  );
}

function DevelopmentReport({ id }: { id: string }) {
  const report = (useDevelopmentReport(id).data?.developmentReport ?? []) as Delta[];
  if (report.length === 0) return null;

  const season = report[0]?.season;
  const gains = [...report].sort((a, b) => b.delta - a.delta);

  return (
    <section className="border-primary/30 bg-primary/[0.05] flex flex-col gap-3 rounded-xl border p-5">
      <div className="flex items-center gap-2">
        <Sparkles className="text-primary size-4" aria-hidden="true" />
        <h2 className="text-base font-bold tracking-[-0.01em]">
          Development report{season != null ? ` · Season ${season}` : ""}
        </h2>
      </div>
      <p className="text-muted-foreground text-sm">Your play, coach, and focus developed these last season:</p>
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
