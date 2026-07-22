"use client";

import { usePlayerProfile } from "@/lib/api/queries";
import { formatMoney } from "@/lib/career/labels";

/** A compact available-funds readout for management page headers (affordability at a glance). */
export function FundsPill({ id }: { id: string }) {
  const funds = usePlayerProfile(id).data?.playerProfile?.availableFunds ?? null;
  if (funds === null) return null;
  return (
    <span className="border-border bg-surface text-muted-foreground rounded-full border px-3 py-1.5 text-xs font-semibold tabular-nums">
      Funds <span className="text-foreground">{formatMoney(funds)}</span>
    </span>
  );
}
