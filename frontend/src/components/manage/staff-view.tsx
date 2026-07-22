"use client";

import { Button } from "@/components/ui/button";
import { SpokeShell, SpokeEmpty, useSpokeGate } from "@/components/career/spoke";
import { StarRating } from "@/components/manage/star-rating";
import { usePlayerStaff, useReleaseStaff } from "@/lib/api/manage";
import { formatMoney } from "@/lib/career/labels";
import { nationalityLabel } from "@/lib/onboarding/options";
import { humanize } from "@/lib/play/options";

type Member = {
  role: string;
  name: string;
  age: number;
  nationality: string;
  personality: string;
  quality: number;
  seasonalSalary: number;
};

/** Manage Staff: the support team currently under contract, with the option to release. */
export function StaffView({ id }: { id: string }) {
  const query = usePlayerStaff(id);
  const release = useReleaseStaff(id);
  const gate = useSpokeGate(query);
  if (gate) return gate;

  const roster: Member[] = query.data?.playerStaff ?? [];

  return (
    <SpokeShell title="Manage Staff" description="Your support team under contract.">
      {roster.length === 0 ? (
        <SpokeEmpty>
          No staff under contract yet. Head to Hire Staff to sign coaches and support.
        </SpokeEmpty>
      ) : (
        <ul className="divide-divider border-border bg-surface flex flex-col divide-y overflow-hidden rounded-lg border">
          {roster.map((member) => (
            <li key={member.role} className="flex items-center justify-between gap-4 px-5 py-4">
              <div className="flex min-w-0 flex-col gap-1">
                <span className="flex items-center gap-2">
                  <span className="truncate font-medium">{member.name}</span>
                  <StarRating quality={member.quality} />
                </span>
                <span className="text-muted-foreground text-sm">
                  {humanize(member.role)} · Age {member.age} · {nationalityLabel(member.nationality)} ·{" "}
                  <span className="font-mono tabular-nums">{formatMoney(member.seasonalSalary)}</span>/yr
                </span>
              </div>
              <Button
                variant="secondary"
                size="sm"
                onClick={() => release.mutate(member.role)}
                disabled={release.isPending}
                aria-label={`Release ${member.name}`}
              >
                Release
              </Button>
            </li>
          ))}
        </ul>
      )}
    </SpokeShell>
  );
}
