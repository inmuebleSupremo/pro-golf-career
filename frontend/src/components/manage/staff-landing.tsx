"use client";

import { Handshake, UserPlus } from "lucide-react";

import { Card, CardHeader } from "@/components/command/card";
import { SpokeShell } from "@/components/career/spoke";
import { usePendingStaff, usePlayerStaff } from "@/lib/api/manage";

/** Staff landing: two bento tiles into Manage (roster) and Hire (candidates). */
export function StaffLanding({ id }: { id: string }) {
  const roster = usePlayerStaff(id).data?.playerStaff?.length ?? null;
  const candidates = usePendingStaff(id).data?.pendingStaff?.length ?? null;

  return (
    <SpokeShell title="Staff" description="Build and run your support team.">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <StaffTile
          href={`/career/${id}/staff/manage`}
          title="Manage Staff"
          icon={<Handshake className="size-6" aria-hidden="true" />}
          blurb="Your team under contract"
          stat={roster}
          statLabel={roster === 1 ? "member signed" : "members signed"}
        />
        <StaffTile
          href={`/career/${id}/staff/hire`}
          title="Hire Staff"
          icon={<UserPlus className="size-6" aria-hidden="true" />}
          blurb="Sign coaches and support"
          stat={candidates}
          statLabel={candidates === 1 ? "candidate available" : "candidates available"}
        />
      </div>
    </SpokeShell>
  );
}

function StaffTile({
  href,
  title,
  icon,
  blurb,
  stat,
  statLabel,
}: {
  href: string;
  title: string;
  icon: React.ReactNode;
  blurb: string;
  stat: number | null;
  statLabel: string;
}) {
  return (
    <Card href={href} className="min-h-[10rem]">
      <CardHeader title={title} portal />
      <div className="mt-auto flex items-center gap-4">
        <span className="bg-primary/10 text-primary grid size-12 shrink-0 place-items-center rounded-xl">
          {icon}
        </span>
        <div className="min-w-0">
          <p className="text-muted-foreground text-sm">{blurb}</p>
          <p className="mt-0.5 text-2xl font-bold tabular-nums tracking-[-0.02em]">
            {stat ?? "—"}
            <span className="text-muted-foreground ml-1.5 text-sm font-normal">{statLabel}</span>
          </p>
        </div>
      </div>
    </Card>
  );
}
