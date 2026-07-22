"use client";

import { useCallback } from "react";
import { useRouter } from "next/navigation";

import { SpokeShell } from "@/components/career/spoke";
import { FundsPill } from "@/components/manage/funds-pill";
import { StaffSection } from "@/components/manage/staff-section";

/** Staff page: hire coaches and support staff from the candidate pool. */
export function StaffView({ id }: { id: string }) {
  const router = useRouter();
  const onUnauthorized = useCallback(() => {
    router.push("/login");
    router.refresh();
  }, [router]);

  return (
    <SpokeShell
      title="Staff"
      description="Hire coaches and support staff to develop your golfer."
      action={<FundsPill id={id} />}
    >
      <StaffSection id={id} onUnauthorized={onUnauthorized} />
    </SpokeShell>
  );
}
