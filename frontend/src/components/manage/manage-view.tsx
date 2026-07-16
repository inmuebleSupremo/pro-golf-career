"use client";

import { useCallback } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";

import { EquipmentSection } from "@/components/manage/equipment-section";
import { ScheduleSection } from "@/components/manage/schedule-section";
import { SponsorshipSection } from "@/components/manage/sponsorship-section";
import { StaffSection } from "@/components/manage/staff-section";
import { TrainingSection } from "@/components/manage/training-section";

/**
 * Management surface for a loaded career. Composes independent sections, each owning
 * its own data and mutations. Unauthorized sessions bubble up here and redirect once.
 */
export function ManageView({ id }: { id: string }) {
  const router = useRouter();

  const onUnauthorized = useCallback(() => {
    router.push("/login");
    router.refresh();
  }, [router]);

  return (
    <div className="flex flex-col gap-10">
      <div className="flex flex-col gap-3">
        <Link
          href={`/career/${id}`}
          className="text-muted-foreground hover:text-foreground inline-flex w-fit items-center gap-1.5 text-sm transition-colors"
        >
          <ArrowLeft className="size-4" aria-hidden="true" />
          Career
        </Link>
        <h1 className="font-serif text-3xl font-medium">Manage career</h1>
      </div>

      <ScheduleSection id={id} onUnauthorized={onUnauthorized} />
      <TrainingSection id={id} onUnauthorized={onUnauthorized} />
      <EquipmentSection id={id} onUnauthorized={onUnauthorized} />
      <StaffSection id={id} onUnauthorized={onUnauthorized} />
      <SponsorshipSection id={id} onUnauthorized={onUnauthorized} />
    </div>
  );
}
