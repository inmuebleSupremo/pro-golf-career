"use client";

import { useEffect } from "react";

import { Button } from "@/components/ui/button";
import { isNotFound, isUnauthorized } from "@/lib/api/graphql-client";
import { useHireStaff, usePendingStaff } from "@/lib/api/manage";
import { usePlayerProfile } from "@/lib/api/queries";
import { formatMoney } from "@/lib/career/labels";
import { humanize } from "@/lib/play/options";

type Candidate = {
  role: string;
  name: string;
  quality: number;
  hiringCost: number;
  seasonalSalary: number;
};

/**
 * Staff: hire from the pending candidate pool. The backend exposes no current-roster
 * query, so this surfaces hiring only; releasing staff isn't wireable until a roster
 * read exists.
 */
export function StaffSection({ id, onUnauthorized }: { id: string; onUnauthorized: () => void }) {
  const { data, isPending, isError, error } = usePendingStaff(id);
  const funds = usePlayerProfile(id).data?.playerProfile?.availableFunds ?? null;
  const hire = useHireStaff(id);

  useEffect(() => {
    if (isError && isUnauthorized(error)) onUnauthorized();
  }, [isError, error, onUnauthorized]);

  return (
    <section className="flex flex-col gap-4">
      {isPending ? (
        <SectionSkeleton />
      ) : isError ? (
        <SectionNote>
          {isNotFound(error)
            ? "This session is no longer available. Reopen the career from your saves."
            : "Couldn't load staff candidates. Try again."}
        </SectionNote>
      ) : data.pendingStaff.length === 0 ? (
        <SectionNote>No staff candidates available right now.</SectionNote>
      ) : (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          {data.pendingStaff.map((candidate, index) => {
            const affordable = funds === null || candidate.hiringCost <= funds;
            return (
              <CandidateCard
                key={`${candidate.role}:${candidate.name}`}
                candidate={candidate}
                action={
                  <Button
                    size="sm"
                    onClick={() => hire.mutate(index)}
                    disabled={!affordable || hire.isPending}
                    title={affordable ? undefined : "Not enough funds"}
                  >
                    {affordable ? "Hire" : "Can't afford"}
                  </Button>
                }
              />
            );
          })}
        </div>
      )}
    </section>
  );
}

function CandidateCard({
  candidate,
  action,
}: {
  candidate: Candidate;
  action: React.ReactNode;
}) {
  return (
    <div className="border-border bg-surface flex flex-col gap-3 rounded-lg border px-4 py-3">
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 flex-col gap-0.5">
          <span className="truncate font-medium">{candidate.name}</span>
          <span className="text-muted-foreground text-sm">{humanize(candidate.role)}</span>
        </div>
        <div className="shrink-0">{action}</div>
      </div>
      <dl className="grid grid-cols-3 gap-2">
        <Stat label="Quality" value={String(Math.round(candidate.quality))} />
        <Stat label="Hiring" value={formatMoney(candidate.hiringCost)} />
        <Stat label="Salary / yr" value={formatMoney(candidate.seasonalSalary)} />
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
