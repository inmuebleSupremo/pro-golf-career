"use client";

import { useEffect, useState } from "react";
import { Check } from "lucide-react";

import { Button } from "@/components/ui/button";
import { isNotFound, isUnauthorized } from "@/lib/api/graphql-client";
import { usePlayerProfile } from "@/lib/api/queries";
import { useSetDevelopmentFocus } from "@/lib/api/manage";
import { humanize } from "@/lib/play/options";

type Attribute = { attribute: string; value: number };

/**
 * Training: pick an ordered development focus. The backend stores focus write-only
 * (it shapes future training), so the chosen order is tracked locally and applied on
 * save. Attributes and their current values are read from the player profile.
 */
export function TrainingSection({ id, onUnauthorized }: { id: string; onUnauthorized: () => void }) {
  const { data, isPending, isError, error } = usePlayerProfile(id);

  useEffect(() => {
    if (isError && isUnauthorized(error)) onUnauthorized();
  }, [isError, error, onUnauthorized]);

  const profile = data?.playerProfile ?? null;

  return (
    <section className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <h2 className="font-serif text-xl font-medium">Training focus</h2>
        <p className="text-muted-foreground text-sm">
          Prioritise the attributes to develop. Tap in the order you want them improved.
        </p>
      </div>

      {isPending ? (
        <SectionSkeleton />
      ) : isError || !profile ? (
        <SectionNote>
          {isNotFound(error)
            ? "This session is no longer available. Reopen the career from your saves."
            : "Couldn't load your golfer. Try again."}
        </SectionNote>
      ) : (
        <FocusPicker id={id} attributes={profile.attributes} />
      )}
    </section>
  );
}

function FocusPicker({ id, attributes }: { id: string; attributes: Attribute[] }) {
  const [focus, setFocus] = useState<string[]>([]);
  const setDevelopmentFocus = useSetDevelopmentFocus(id);

  function toggle(name: string) {
    setDevelopmentFocus.reset(); // any edit clears the prior "saved" confirmation
    setFocus((prev) =>
      prev.includes(name) ? prev.filter((n) => n !== name) : [...prev, name],
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        {attributes.map((a) => {
          const order = focus.indexOf(a.attribute);
          const selected = order !== -1;
          return (
            <button
              key={a.attribute}
              type="button"
              onClick={() => toggle(a.attribute)}
              aria-pressed={selected}
              className={`group flex flex-col gap-2 rounded-lg border px-4 py-3 text-left transition-colors ${
                selected
                  ? "border-primary bg-primary/[0.06]"
                  : "border-border bg-surface hover:bg-background"
              }`}
            >
              <div className="flex items-center justify-between gap-2">
                <span className="flex items-center gap-2 font-medium">
                  {selected ? (
                    <span className="bg-primary text-primary-foreground inline-flex size-5 items-center justify-center rounded-full font-mono text-xs tabular-nums">
                      {order + 1}
                    </span>
                  ) : null}
                  {humanize(a.attribute)}
                </span>
                <span className="text-subtle-foreground font-mono text-xs tabular-nums">
                  {a.value}
                </span>
              </div>
              <div className="bg-divider h-1.5 w-full overflow-hidden rounded-full">
                <div className="bg-primary h-full rounded-full" style={{ width: `${a.value}%` }} />
              </div>
            </button>
          );
        })}
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <Button
          onClick={() => setDevelopmentFocus.mutate(focus)}
          disabled={focus.length === 0 || setDevelopmentFocus.isPending}
        >
          {setDevelopmentFocus.isPending ? "Saving…" : "Save focus"}
        </Button>
        {focus.length > 0 ? (
          <Button
            variant="ghost"
            onClick={() => {
              setFocus([]);
              setDevelopmentFocus.reset();
            }}
            disabled={setDevelopmentFocus.isPending}
          >
            Clear
          </Button>
        ) : null}
        {setDevelopmentFocus.isSuccess ? (
          <span className="text-success inline-flex items-center gap-1.5 text-sm font-medium">
            <Check className="size-4" aria-hidden="true" />
            Focus saved
          </span>
        ) : null}
        {setDevelopmentFocus.isError ? (
          <span className="text-destructive text-sm">Couldn&apos;t save — try again.</span>
        ) : null}
      </div>
    </div>
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
  return <div aria-hidden="true" className="border-border bg-surface h-48 animate-pulse rounded-lg border" />;
}
