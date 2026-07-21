"use client";

import { useCallback } from "react";
import { useRouter } from "next/navigation";

import { EquipmentSection } from "@/components/manage/equipment-section";
import { StaffSection } from "@/components/manage/staff-section";
import { TrainingSection } from "@/components/manage/training-section";
import { usePlayerProfile } from "@/lib/api/queries";
import { formatMoney } from "@/lib/career/labels";

/**
 * Development surface for a loaded career: train, equip, and staff the golfer, presented as
 * bento boxes. Schedule + availability live on Schedule; sponsorships + finances on Finance.
 * Available funds is shown here purely so upgrades and hires can be judged for affordability.
 */
export function ManageView({ id }: { id: string }) {
  const router = useRouter();
  const funds = usePlayerProfile(id).data?.playerProfile?.availableFunds ?? null;

  const onUnauthorized = useCallback(() => {
    router.push("/login");
    router.refresh();
  }, [router]);

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
        <h1 className="text-[1.45rem] font-bold tracking-[-0.025em]">Development</h1>
        <span className="text-muted-foreground text-sm">Train, equip, and staff your golfer.</span>
        {funds !== null ? (
          <span className="border-border bg-surface text-muted-foreground ml-auto rounded-full border px-3 py-1.5 text-xs font-semibold tabular-nums">
            Funds <span className="text-foreground">{formatMoney(funds)}</span>
          </span>
        ) : null}
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <BentoBox className="lg:col-span-2">
          <TrainingSection id={id} onUnauthorized={onUnauthorized} />
        </BentoBox>
        <BentoBox>
          <EquipmentSection id={id} onUnauthorized={onUnauthorized} />
        </BentoBox>
        <BentoBox>
          <StaffSection id={id} onUnauthorized={onUnauthorized} />
        </BentoBox>
      </div>
    </div>
  );
}

/** A bento panel: an elevated surface so the section's inner surface tiles read against it. */
function BentoBox({ className, children }: { className?: string; children: React.ReactNode }) {
  return (
    <div
      className={`border-border from-surface-elevated to-surface rounded-xl border bg-gradient-to-b p-5 shadow-[var(--shadow-md)] ${className ?? ""}`}
    >
      {children}
    </div>
  );
}
