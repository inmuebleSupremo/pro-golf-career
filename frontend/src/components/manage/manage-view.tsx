"use client";

import { useCallback } from "react";
import { useRouter } from "next/navigation";

import { SpokeShell } from "@/components/career/spoke";
import { TrainingSection } from "@/components/manage/training-section";

/**
 * Development page: set the golfer's training focus. Equipment and Staff have their own pages;
 * sponsorships + finances live on Finance; schedule + availability on Schedule.
 */
export function ManageView({ id }: { id: string }) {
  const router = useRouter();
  const onUnauthorized = useCallback(() => {
    router.push("/login");
    router.refresh();
  }, [router]);

  return (
    <SpokeShell title="Development" description="Focus this season’s training and review last season’s gains.">
      <TrainingSection id={id} onUnauthorized={onUnauthorized} />
    </SpokeShell>
  );
}
