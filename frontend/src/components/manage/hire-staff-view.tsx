"use client";

import { UserPlus } from "lucide-react";

import { Button } from "@/components/ui/button";
import { SpokeShell, SpokeEmpty, useSpokeGate } from "@/components/career/spoke";
import { FundsPill } from "@/components/manage/funds-pill";
import { StarRating } from "@/components/manage/star-rating";
import { useHireStaff, usePendingStaff } from "@/lib/api/manage";
import { usePlayerProfile } from "@/lib/api/queries";
import { formatMoney } from "@/lib/career/labels";
import { nationalityLabel } from "@/lib/onboarding/options";
import { humanize } from "@/lib/play/options";

type Candidate = {
  role: string;
  name: string;
  age: number;
  nationality: string;
  personality: string;
  quality: number;
  hiringCost: number;
  seasonalSalary: number;
};

/** Hire Staff: this season's candidates from the staffing pool, as profile cards. */
export function HireStaffView({ id }: { id: string }) {
  const query = usePendingStaff(id);
  const funds = usePlayerProfile(id).data?.playerProfile?.availableFunds ?? null;
  const hire = useHireStaff(id);
  const gate = useSpokeGate(query);
  if (gate) return gate;

  const candidates: Candidate[] = query.data?.pendingStaff ?? [];

  return (
    <SpokeShell
      title="Hire Staff"
      description="This season’s candidates for your support team."
      action={<FundsPill id={id} />}
    >
      {candidates.length === 0 ? (
        <SpokeEmpty>
          No candidates this season. Advance a season for a fresh set, or release a role you’ve
          already filled to open it up.
        </SpokeEmpty>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {candidates.map((candidate, index) => (
            <CandidateCard
              key={`${candidate.role}:${candidate.name}:${index}`}
              candidate={candidate}
              affordable={funds === null || candidate.hiringCost <= funds}
              pending={hire.isPending}
              onHire={() => hire.mutate(index)}
            />
          ))}
        </div>
      )}
    </SpokeShell>
  );
}

function CandidateCard({
  candidate,
  affordable,
  pending,
  onHire,
}: {
  candidate: Candidate;
  affordable: boolean;
  pending: boolean;
  onHire: () => void;
}) {
  return (
    <div className="border-border from-surface-elevated to-surface flex flex-col gap-4 rounded-xl border bg-gradient-to-b p-5 shadow-[var(--shadow-md)]">
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 flex-col gap-0.5">
          <h3 className="truncate text-base font-bold tracking-[-0.01em]">{candidate.name}</h3>
          <p className="text-muted-foreground text-sm">{humanize(candidate.role)}</p>
        </div>
        <StarRating quality={candidate.quality} />
      </div>

      <p className="text-subtle-foreground text-sm">
        Age {candidate.age} · {nationalityLabel(candidate.nationality)} ·{" "}
        {humanize(candidate.personality)}
      </p>

      <dl className="mt-auto grid grid-cols-2 gap-2">
        <Stat label="Hiring cost" value={formatMoney(candidate.hiringCost)} />
        <Stat label="Salary / yr" value={formatMoney(candidate.seasonalSalary)} />
      </dl>

      <Button
        onClick={onHire}
        disabled={!affordable || pending}
        title={affordable ? undefined : "Not enough funds"}
      >
        <UserPlus className="size-4" aria-hidden="true" />
        {affordable ? "Hire" : "Can’t afford"}
      </Button>
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
