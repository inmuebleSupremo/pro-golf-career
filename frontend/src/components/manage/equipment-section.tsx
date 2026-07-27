"use client";

import { useEffect, useState } from "react";
import { Check, ChevronDown, Handshake, Lock, Sparkles } from "lucide-react";

import { Button } from "@/components/ui/button";
import { isNotFound, isUnauthorized } from "@/lib/api/graphql-client";
import {
  useAcceptEquipmentDeal,
  useBuyEquipment,
  useEquipment,
  useSelectLoadoutItem,
} from "@/lib/api/manage";
import { usePlayerProfile } from "@/lib/api/queries";
import { formatMoney } from "@/lib/career/labels";
import { humanize } from "@/lib/play/options";

type Item = {
  name: string;
  category: string;
  brand: string;
  quality: number;
  cost: number;
  forgiveness: number;
  power: number;
  workability: number;
  feel: number;
  fit: number;
};

type Deal = {
  brand: string;
  signingBonus: number;
  perSeasonRetainer: number;
  durationSeasons: number;
  gearTier: number;
  gearFit: number;
  seasonsRemaining: number;
};

const CATEGORY_ORDER = [
  "DRIVER",
  "FAIRWAY_WOODS",
  "HYBRIDS",
  "IRONS",
  "WEDGES",
  "PUTTER",
  "GOLF_BALL",
];

/**
 * Equipment: your bag and the season's upgrade offers. Every item trades off four characteristics
 * (forgiveness, power, workability, feel) shaped by its brand, and carries a fit score against your build —
 * so choosing gear is a real decision (complement your weaknesses), not a quality ladder. Purchases
 * auto-equip; you can also switch to any owned item.
 */
export function EquipmentSection({ id, onUnauthorized }: { id: string; onUnauthorized: () => void }) {
  const { data, isPending, isError, error } = useEquipment(id);
  const funds = usePlayerProfile(id).data?.playerProfile?.availableFunds ?? null;

  useEffect(() => {
    if (isError && isUnauthorized(error)) onUnauthorized();
  }, [isError, error, onUnauthorized]);

  if (isPending) return <SectionSkeleton />;
  if (isError) {
    return (
      <SectionNote>
        {isNotFound(error)
          ? "This session is no longer available. Reopen the career from your saves."
          : "Couldn't load your equipment. Try again."}
      </SectionNote>
    );
  }

  return (
    <EquipmentBody
      id={id}
      loadout={data.playerLoadout as Item[]}
      owned={data.playerEquipment as Item[]}
      offers={data.pendingEquipment as Item[]}
      activeDeal={(data.activeEquipmentDeal as Deal | null) ?? null}
      pendingDeals={(data.pendingEquipmentDeals as Deal[]) ?? []}
      funds={funds}
    />
  );
}

function byCategory(order: string): number {
  const i = CATEGORY_ORDER.indexOf(order);
  return i === -1 ? CATEGORY_ORDER.length : i;
}

function EquipmentBody({
  id,
  loadout,
  owned,
  offers,
  activeDeal,
  pendingDeals,
  funds,
}: {
  id: string;
  loadout: Item[];
  owned: Item[];
  offers: Item[];
  activeDeal: Deal | null;
  pendingDeals: Deal[];
  funds: number | null;
}) {
  const select = useSelectLoadoutItem(id);
  const buy = useBuyEquipment(id);
  const acceptDeal = useAcceptEquipmentDeal(id);

  const equippedByCategory = new Map(loadout.map((i) => [i.category, i]));
  const categories = [...new Set(owned.map((i) => i.category))].sort((a, b) => byCategory(a) - byCategory(b));
  const offerCategories = [...new Set(offers.map((i) => i.category))].sort((a, b) => byCategory(a) - byCategory(b));

  return (
    <div className="flex flex-col gap-10">
      <DealsSection
        activeDeal={activeDeal}
        pendingDeals={pendingDeals}
        funds={funds}
        onSign={(index) => acceptDeal.mutate(index)}
        signing={acceptDeal.isPending}
      />

      <section className="flex flex-col gap-4">
        <SectionHeading
          title="Your bag"
          hint="Your club in each slot — expand a slot to switch to an alternative you own."
        />
        {owned.length === 0 ? (
          <SectionNote>No equipment owned yet.</SectionNote>
        ) : (
          <div className="flex flex-col gap-4">
            {categories.map((category) => (
              <CategoryBag
                key={category}
                category={category}
                equipped={equippedByCategory.get(category) ?? null}
                items={owned.filter((i) => i.category === category)}
                onEquip={(item) => select.mutate({ category: item.category, name: item.name })}
                equipping={select.isPending}
              />
            ))}
          </div>
        )}
      </section>

      <section className="flex flex-col gap-4">
        <SectionHeading
          title="Upgrade offers"
          hint="Each slot offers two brands — a trade-off. Buy what suits your build; purchases auto-equip."
        />
        {offers.length === 0 ? (
          <SectionNote>
            {activeDeal
              ? `You're kitted by ${activeDeal.brand}. À-la-carte upgrades resume when the deal ends.`
              : "No upgrade offers right now. New gear appears each off-season."}
          </SectionNote>
        ) : (
          <div className="flex flex-col gap-6">
            {offerCategories.map((category) => {
              const pairs = offers
                .map((item, index) => ({ item, index }))
                .filter((o) => o.item.category === category);
              const equipped = equippedByCategory.get(category) ?? null;
              return (
                <div key={category} className="flex flex-col gap-2.5">
                  <span className="text-muted-foreground text-sm font-semibold">{humanize(category)}</span>
                  <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
                    {pairs.map(({ item, index }) => {
                      const affordable = funds === null || item.cost <= funds;
                      const betterFit = equipped != null && item.fit > equipped.fit + 0.02;
                      const higherTier = equipped != null && item.quality > equipped.quality + 0.01;
                      return (
                        <ItemCard
                          key={`${item.category}:${item.name}`}
                          item={item}
                          badge={
                            betterFit || higherTier ? (
                              <span className="text-success text-[0.7rem] font-semibold">
                                {betterFit && higherTier
                                  ? "Better fit & tier"
                                  : betterFit
                                    ? "Better fit"
                                    : "Higher tier"}
                              </span>
                            ) : null
                          }
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
                </div>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}

/* ----------------------------------------------------------- Brand deals */

function DealsSection({
  activeDeal,
  pendingDeals,
  funds,
  onSign,
  signing,
}: {
  activeDeal: Deal | null;
  pendingDeals: Deal[];
  funds: number | null;
  onSign: (index: number) => void;
  signing: boolean;
}) {
  if (activeDeal) {
    return (
      <section className="flex flex-col gap-4">
        <SectionHeading title="Brand deal" hint="You're under contract — kitted and paid by your brand." />
        <ActiveDealBanner deal={activeDeal} />
      </section>
    );
  }
  if (pendingDeals.length === 0) return null;

  return (
    <section className="flex flex-col gap-4">
      <SectionHeading
        title="Brand deals"
        hint="Sign a brand: they pay you and kit your whole bag — but you play their gear and lock in for the term."
      />
      <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
        {pendingDeals.map((deal, index) => (
          <DealCard
            key={`${deal.brand}:${index}`}
            deal={deal}
            funds={funds}
            onSign={() => onSign(index)}
            signing={signing}
          />
        ))}
      </div>
    </section>
  );
}

function ActiveDealBanner({ deal }: { deal: Deal }) {
  return (
    <div className="border-primary bg-primary/[0.06] flex flex-col gap-3 rounded-xl border px-5 py-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex items-center gap-2.5">
          <span className="bg-primary/12 text-primary grid size-10 shrink-0 place-items-center rounded-lg">
            <Handshake className="size-5" aria-hidden="true" />
          </span>
          <div>
            <p className="font-semibold tracking-[-0.01em]">{deal.brand}</p>
            <p className="text-muted-foreground text-sm">
              {deal.seasonsRemaining} {deal.seasonsRemaining === 1 ? "season" : "seasons"} remaining
            </p>
          </div>
        </div>
        <span className="text-success font-mono text-sm font-semibold tabular-nums">
          {formatMoney(deal.perSeasonRetainer)}/yr
        </span>
      </div>
      <FitBar fit={deal.gearFit} />
      <p className="text-subtle-foreground text-xs">
        À-la-carte upgrades are paused until the deal ends.
      </p>
    </div>
  );
}

function DealCard({
  deal,
  funds,
  onSign,
  signing,
}: {
  deal: Deal;
  funds: number | null;
  onSign: () => void;
  signing: boolean;
}) {
  return (
    <div className="border-border bg-surface flex flex-col gap-3.5 rounded-xl border px-4 py-3.5">
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 flex-col gap-0.5">
          <span className="truncate font-semibold tracking-[-0.01em]">{deal.brand}</span>
          <span className="text-subtle-foreground text-xs">
            Gear tier <span className="text-foreground font-mono tabular-nums">{pct(deal.gearTier)}</span> ·{" "}
            {deal.durationSeasons} {deal.durationSeasons === 1 ? "season" : "seasons"}
          </span>
        </div>
        <Button size="sm" onClick={onSign} disabled={signing}>
          Sign
        </Button>
      </div>

      <FitBar fit={deal.gearFit} />

      <dl className="grid grid-cols-2 gap-x-4 gap-y-1">
        <Money label="Retainer / yr" value={deal.perSeasonRetainer} accent />
        <Money label="Signing bonus" value={deal.signingBonus} />
      </dl>
      {funds != null ? (
        <p className="text-subtle-foreground inline-flex items-center gap-1 text-[0.7rem]">
          <Lock className="size-3" aria-hidden="true" /> Locks you into {deal.brand}&apos;s gear for the term.
        </p>
      ) : null}
    </div>
  );
}

function Money({ label, value, accent }: { label: string; value: number; accent?: boolean }) {
  return (
    <div className="flex flex-col gap-0.5">
      <dt className="text-subtle-foreground text-[0.7rem] tracking-[0.04em] uppercase">{label}</dt>
      <dd className={`font-mono text-sm tabular-nums ${accent ? "text-success" : "text-foreground"}`}>
        {formatMoney(value)}
      </dd>
    </div>
  );
}

/* --------------------------------------------------------- Your bag slot */

/**
 * One equipment slot: the currently-equipped club, with the other owned clubs collapsed behind a toggle so
 * the bag stays compact no matter how long the career runs. Alternatives are deduped to the best (highest
 * tier) of each brand, so a shelf of near-identical hand-me-downs never piles up — only the genuinely
 * different trade-offs (one per brand) are offered as a switch.
 */
function CategoryBag({
  category,
  equipped,
  items,
  onEquip,
  equipping,
}: {
  category: string;
  equipped: Item | null;
  items: Item[];
  onEquip: (item: Item) => void;
  equipping: boolean;
}) {
  const [open, setOpen] = useState(false);
  // Dedupe over the whole slot (best of each brand), then drop the equipped one — so a strictly-worse
  // same-brand club never shows as an "alternative", only genuinely different trade-offs do.
  const alternatives = dedupeByBrand(items)
    .filter((i) => i.name !== equipped?.name)
    .sort((a, b) => b.fit - a.fit);
  const bestFitName = alternatives.length > 0 ? alternatives[0].name : null;

  return (
    <div className="flex flex-col gap-2.5">
      <div className="flex items-center justify-between gap-3">
        <span className="text-muted-foreground text-sm font-semibold">{humanize(category)}</span>
        {alternatives.length > 0 ? (
          <button
            type="button"
            onClick={() => setOpen((v) => !v)}
            aria-expanded={open}
            className="text-subtle-foreground hover:text-foreground inline-flex items-center gap-1 text-xs font-medium transition-colors"
          >
            {open
              ? "Hide"
              : `${alternatives.length} ${alternatives.length === 1 ? "alternative" : "alternatives"}`}
            <ChevronDown className={`size-3.5 transition-transform ${open ? "rotate-180" : ""}`} aria-hidden="true" />
          </button>
        ) : null}
      </div>

      {equipped ? (
        <ItemCard
          item={equipped}
          highlighted
          action={
            <span className="text-success inline-flex items-center gap-1.5 text-sm font-medium">
              <Check className="size-4" aria-hidden="true" /> Equipped
            </span>
          }
        />
      ) : null}

      {open ? (
        <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
          {alternatives.map((item) => (
            <ItemCard
              key={item.name}
              item={item}
              badge={
                item.name === bestFitName ? (
                  <span className="text-info inline-flex items-center gap-1 text-[0.7rem] font-semibold">
                    <Sparkles className="size-3" aria-hidden="true" /> Best fit
                  </span>
                ) : null
              }
              action={
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => onEquip(item)}
                  disabled={equipping}
                >
                  Equip
                </Button>
              }
            />
          ))}
        </div>
      ) : null}
    </div>
  );
}

/** Keeps only the best (highest-tier) item of each brand — same-brand clubs share a shape, so the lesser ones dominate nothing. */
function dedupeByBrand(items: Item[]): Item[] {
  const best = new Map<string, Item>();
  for (const item of items) {
    const current = best.get(item.brand);
    if (!current || item.quality > current.quality) best.set(item.brand, item);
  }
  return [...best.values()];
}

/* -------------------------------------------------------------- Item card */

const TRAITS: { key: keyof Item; label: string }[] = [
  { key: "forgiveness", label: "Forgiveness" },
  { key: "power", label: "Power" },
  { key: "workability", label: "Workability" },
  { key: "feel", label: "Feel" },
];

function ItemCard({
  item,
  action,
  meta,
  badge,
  highlighted,
}: {
  item: Item;
  action: React.ReactNode;
  meta?: React.ReactNode;
  badge?: React.ReactNode;
  highlighted?: boolean;
}) {
  return (
    <div
      className={`flex flex-col gap-3.5 rounded-xl border px-4 py-3.5 ${
        highlighted ? "border-primary bg-primary/[0.06]" : "border-border bg-surface"
      }`}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 flex-col gap-0.5">
          <span className="flex items-center gap-2">
            <span className="truncate font-semibold tracking-[-0.01em]">{item.brand}</span>
            {badge}
          </span>
          <span className="text-subtle-foreground text-xs">
            Tier <span className="text-foreground font-mono tabular-nums">{pct(item.quality)}</span>
            {meta ? <> · {meta}</> : null}
          </span>
        </div>
        <div className="shrink-0">{action}</div>
      </div>

      <FitBar fit={item.fit} />

      <dl className="grid grid-cols-2 gap-x-4 gap-y-1.5">
        {TRAITS.map((t) => (
          <TraitBar key={t.key} label={t.label} value={item[t.key] as number} />
        ))}
      </dl>
    </div>
  );
}

/** Fit relative to the player's build: 0.5 is neutral; tone/label escalate above and below. */
function FitBar({ fit }: { fit: number }) {
  const { label, tone, track } =
    fit >= 0.6
      ? { label: "Great fit", tone: "text-success", track: "bg-success" }
      : fit >= 0.54
        ? { label: "Good fit", tone: "text-success", track: "bg-success/70" }
        : fit > 0.46
          ? { label: "Neutral fit", tone: "text-muted-foreground", track: "bg-muted-foreground/50" }
          : fit > 0.4
            ? { label: "Weak fit", tone: "text-warning", track: "bg-warning" }
            : { label: "Poor fit", tone: "text-destructive", track: "bg-destructive" };
  return (
    <div className="flex items-center gap-2.5">
      <span className={`w-20 shrink-0 text-xs font-semibold ${tone}`}>{label}</span>
      <div className="bg-surface-3 relative h-1.5 flex-1 overflow-hidden rounded-full">
        {/* Centred meter: the 50% mark is neutral, the bar fills from there toward the fit. */}
        <div className="bg-border absolute inset-y-0 left-1/2 w-px" aria-hidden="true" />
        <div
          className={`absolute inset-y-0 rounded-full ${track}`}
          style={
            fit >= 0.5
              ? { left: "50%", width: `${Math.min(50, (fit - 0.5) * 100)}%` }
              : { right: "50%", width: `${Math.min(50, (0.5 - fit) * 100)}%` }
          }
        />
      </div>
    </div>
  );
}

function TraitBar({ label, value }: { label: string; value: number }) {
  return (
    <div className="flex flex-col gap-1">
      <div className="flex items-baseline justify-between gap-2">
        <span className="text-subtle-foreground text-[0.7rem] tracking-[0.04em] uppercase">{label}</span>
        <span className="text-foreground font-mono text-xs tabular-nums">{pct(value)}</span>
      </div>
      <div className="bg-surface-3 h-1.5 overflow-hidden rounded-full">
        <div className="bg-info/70 h-full rounded-full" style={{ width: `${pct(value)}%` }} />
      </div>
    </div>
  );
}

function pct(value: number): number {
  return Math.round(value * 100);
}

function SectionHeading({ title, hint }: { title: string; hint: string }) {
  return (
    <div className="flex flex-col gap-0.5">
      <h3 className="text-base font-bold tracking-[-0.01em]">{title}</h3>
      <p className="text-muted-foreground text-sm">{hint}</p>
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
