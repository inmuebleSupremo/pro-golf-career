"use client";

import { useEffect } from "react";

import { Button } from "@/components/ui/button";
import { isNotFound, isUnauthorized } from "@/lib/api/graphql-client";
import { useAcceptSponsorship, usePendingSponsorships } from "@/lib/api/manage";
import { formatMoney } from "@/lib/career/labels";

type Offer = {
  sponsor: string;
  perSeasonPayment: number;
  signingBonus: number;
  durationSeasons: number;
  grossValue: number;
};

/** Sponsorships: review and accept pending endorsement offers. Accepting pays the signing bonus. */
export function SponsorshipSection({
  id,
  onUnauthorized,
}: {
  id: string;
  onUnauthorized: () => void;
}) {
  const { data, isPending, isError, error } = usePendingSponsorships(id);
  const accept = useAcceptSponsorship(id);

  useEffect(() => {
    if (isError && isUnauthorized(error)) onUnauthorized();
  }, [isError, error, onUnauthorized]);

  return (
    <section className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <h2 className="font-serif text-xl font-medium">Sponsorships</h2>
        <p className="text-muted-foreground text-sm">
          Sign endorsement deals for a signing bonus and per-season income.
        </p>
      </div>

      {isPending ? (
        <SectionSkeleton />
      ) : isError ? (
        <SectionNote>
          {isNotFound(error)
            ? "This session is no longer available. Reopen the career from your saves."
            : "Couldn't load sponsorship offers. Try again."}
        </SectionNote>
      ) : data.pendingSponsorships.length === 0 ? (
        <SectionNote>No sponsorship offers on the table right now.</SectionNote>
      ) : (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          {data.pendingSponsorships.map((offer, index) => (
            <OfferCard
              key={`${offer.sponsor}:${index}`}
              offer={offer}
              action={
                <Button
                  size="sm"
                  onClick={() => accept.mutate(index)}
                  disabled={accept.isPending}
                >
                  Accept
                </Button>
              }
            />
          ))}
        </div>
      )}
    </section>
  );
}

function OfferCard({ offer, action }: { offer: Offer; action: React.ReactNode }) {
  return (
    <div className="border-border bg-surface flex flex-col gap-3 rounded-lg border px-4 py-3">
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 flex-col gap-0.5">
          <span className="truncate font-medium">{offer.sponsor}</span>
          <span className="text-muted-foreground text-sm">
            {offer.durationSeasons} {offer.durationSeasons === 1 ? "season" : "seasons"} ·{" "}
            <span className="text-foreground font-mono tabular-nums">
              {formatMoney(offer.grossValue)}
            </span>{" "}
            total
          </span>
        </div>
        <div className="shrink-0">{action}</div>
      </div>
      <dl className="grid grid-cols-2 gap-2">
        <Stat label="Signing bonus" value={formatMoney(offer.signingBonus)} />
        <Stat label="Per season" value={formatMoney(offer.perSeasonPayment)} />
      </dl>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-0.5">
      <dt className="text-subtle-foreground text-xs tracking-[0.08em] uppercase">{label}</dt>
      <dd className="text-foreground font-mono text-sm tabular-nums">{value}</dd>
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
  return <div aria-hidden="true" className="border-border bg-surface h-40 animate-pulse rounded-lg border" />;
}
