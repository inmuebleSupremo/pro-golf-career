"use client";

import { useEffect } from "react";
import { Check } from "lucide-react";

import { Button } from "@/components/ui/button";
import { isNotFound, isUnauthorized } from "@/lib/api/graphql-client";
import { useBuyEquipment, useEquipment, useSelectLoadoutItem } from "@/lib/api/manage";
import { usePlayerProfile } from "@/lib/api/queries";
import { formatMoney } from "@/lib/career/labels";
import { humanize } from "@/lib/play/options";

type Item = {
  name: string;
  category: string;
  quality: number;
  cost: number;
  forgiveness: number;
  power: number;
  workability: number;
  feel: number;
};

/**
 * Equipment: the current tournament loadout, the owned bag (equip an alternative),
 * and pending upgrade offers (buy if affordable). Purchases auto-equip on the backend.
 */
export function EquipmentSection({
  id,
  onUnauthorized,
}: {
  id: string;
  onUnauthorized: () => void;
}) {
  const { data, isPending, isError, error } = useEquipment(id);
  const funds = usePlayerProfile(id).data?.playerProfile?.availableFunds ?? null;

  useEffect(() => {
    if (isError && isUnauthorized(error)) onUnauthorized();
  }, [isError, error, onUnauthorized]);

  return (
    <section className="flex flex-col gap-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div className="flex flex-col gap-1">
          <h2 className="font-serif text-xl font-medium">Equipment</h2>
          <p className="text-muted-foreground text-sm">
            Set your loadout and buy upgrades between events.
          </p>
        </div>
        {funds !== null ? (
          <span className="text-subtle-foreground text-sm">
            Available funds{" "}
            <span className="text-foreground font-mono tabular-nums">{formatMoney(funds)}</span>
          </span>
        ) : null}
      </div>

      {isPending ? (
        <SectionSkeleton />
      ) : isError ? (
        <SectionNote>
          {isNotFound(error)
            ? "This session is no longer available. Reopen the career from your saves."
            : "Couldn't load your equipment. Try again."}
        </SectionNote>
      ) : (
        <EquipmentBody
          id={id}
          loadout={data.playerLoadout}
          owned={data.playerEquipment}
          offers={data.pendingEquipment}
          funds={funds}
        />
      )}
    </section>
  );
}

function EquipmentBody({
  id,
  loadout,
  owned,
  offers,
  funds,
}: {
  id: string;
  loadout: Item[];
  owned: Item[];
  offers: Item[];
  funds: number | null;
}) {
  const select = useSelectLoadoutItem(id);
  const buy = useBuyEquipment(id);

  const equipped = new Set(loadout.map((i) => `${i.category}:${i.name}`));
  const categories = [...new Set(owned.map((i) => i.category))].sort();

  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-col gap-4">
        <h3 className="text-subtle-foreground text-xs font-medium tracking-[0.08em] uppercase">
          Your bag
        </h3>
        {owned.length === 0 ? (
          <SectionNote>No equipment owned yet.</SectionNote>
        ) : (
          <div className="flex flex-col gap-5">
            {categories.map((category) => (
              <div key={category} className="flex flex-col gap-2">
                <span className="text-muted-foreground text-sm font-medium">
                  {humanize(category)}
                </span>
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                  {owned
                    .filter((i) => i.category === category)
                    .map((item) => {
                      const isEquipped = equipped.has(`${item.category}:${item.name}`);
                      return (
                        <ItemCard
                          key={item.name}
                          item={item}
                          highlighted={isEquipped}
                          action={
                            isEquipped ? (
                              <span className="text-success inline-flex items-center gap-1.5 text-sm font-medium">
                                <Check className="size-4" aria-hidden="true" />
                                Equipped
                              </span>
                            ) : (
                              <Button
                                variant="secondary"
                                size="sm"
                                onClick={() =>
                                  select.mutate({ category: item.category, name: item.name })
                                }
                                disabled={select.isPending}
                              >
                                Equip
                              </Button>
                            )
                          }
                        />
                      );
                    })}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="flex flex-col gap-4">
        <h3 className="text-subtle-foreground text-xs font-medium tracking-[0.08em] uppercase">
          Upgrade offers
        </h3>
        {offers.length === 0 ? (
          <SectionNote>No upgrade offers right now.</SectionNote>
        ) : (
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            {offers.map((item, index) => {
              const affordable = funds === null || item.cost <= funds;
              return (
                <ItemCard
                  key={`${item.category}:${item.name}`}
                  item={item}
                  meta={<span className="font-mono tabular-nums">{formatMoney(item.cost)}</span>}
                  action={
                    <Button
                      size="sm"
                      onClick={() => buy.mutate(index)}
                      disabled={!affordable || buy.isPending}
                      title={affordable ? undefined : "Not enough funds"}
                    >
                      {affordable ? "Buy" : "Can't afford"}
                    </Button>
                  }
                />
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

function ItemCard({
  item,
  action,
  meta,
  highlighted,
}: {
  item: Item;
  action: React.ReactNode;
  meta?: React.ReactNode;
  highlighted?: boolean;
}) {
  return (
    <div
      className={`flex flex-col gap-3 rounded-lg border px-4 py-3 ${
        highlighted ? "border-primary bg-primary/[0.06]" : "border-border bg-surface"
      }`}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 flex-col gap-0.5">
          <span className="truncate font-medium">{item.name}</span>
          <span className="text-subtle-foreground text-xs">
            Quality{" "}
            <span className="font-mono tabular-nums">{Math.round(item.quality)}</span>
            {meta ? <> · {meta}</> : null}
          </span>
        </div>
        <div className="shrink-0">{action}</div>
      </div>
      <dl className="grid grid-cols-4 gap-2">
        <Trait label="Forgive" value={item.forgiveness} />
        <Trait label="Power" value={item.power} />
        <Trait label="Work" value={item.workability} />
        <Trait label="Feel" value={item.feel} />
      </dl>
    </div>
  );
}

function Trait({ label, value }: { label: string; value: number }) {
  return (
    <div className="flex flex-col gap-0.5">
      <dt className="text-subtle-foreground text-[0.65rem] tracking-[0.06em] uppercase">{label}</dt>
      <dd className="text-foreground font-mono text-sm tabular-nums">{Math.round(value)}</dd>
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
  return <div aria-hidden="true" className="border-border bg-surface h-64 animate-pulse rounded-lg border" />;
}
