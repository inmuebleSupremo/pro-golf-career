"use client";

import { useCallback } from "react";
import { useRouter } from "next/navigation";

import { SpokeShell } from "@/components/career/spoke";
import { EquipmentSection } from "@/components/manage/equipment-section";
import { FundsPill } from "@/components/manage/funds-pill";

/** Equipment page: the tournament loadout, the owned bag, and upgrade offers. */
export function EquipmentView({ id }: { id: string }) {
  const router = useRouter();
  const onUnauthorized = useCallback(() => {
    router.push("/login");
    router.refresh();
  }, [router]);

  return (
    <SpokeShell
      title="Equipment"
      description="Set your loadout and buy upgrades between events."
      action={<FundsPill id={id} />}
    >
      <EquipmentSection id={id} onUnauthorized={onUnauthorized} />
    </SpokeShell>
  );
}
