import Link from "next/link";

import { AudioControls } from "@/components/play/audio-controls";
import { cn } from "@/lib/utils";

export function PracticeFacilityNav({ id, active }: { id: string; active: "range" | "putting" }) {
  return <nav aria-label="Practice Facility" className="flex shrink-0 gap-1">
    <Link href={`/career/${id}/practice`} aria-current={active === "range" ? "page" : undefined}
      className={cn("rounded-md px-2 py-1 text-xs font-semibold", active === "range"
        ? "bg-primary text-primary-foreground" : "border-border text-muted-foreground hover:text-foreground border")}>Driving range</Link>
    <Link href={`/career/${id}/practice/putting`} aria-current={active === "putting" ? "page" : undefined}
      className={cn("rounded-md px-2 py-1 text-xs font-semibold", active === "putting"
        ? "bg-primary text-primary-foreground" : "border-border text-muted-foreground hover:text-foreground border")}>Putting green</Link>
  </nav>;
}

export function PracticeFacilityHeader({ id, active, title, subtitle }: {
  id: string;
  active: "range" | "putting";
  title: string;
  subtitle: string;
}) {
  return <header className="practice-facility-header border-border bg-surface flex shrink-0 flex-wrap items-center gap-x-3 gap-y-1 border-b px-3 py-1.5 sm:px-4">
    <div className="min-w-40 flex-1">
      <p className="text-subtle-foreground text-[9px] font-bold tracking-widest uppercase">Practice Facility · Free practice</p>
      <h1 className="text-foreground text-base leading-tight font-bold">{title}</h1>
      <p className="text-muted-foreground text-[10px]">{subtitle}</p>
    </div>
    <div className="ml-auto flex w-full shrink-0 flex-wrap items-center justify-end gap-2 sm:w-auto">
      <PracticeFacilityNav id={id} active={active} />
      <AudioControls />
      <Link href={`/career/${id}`}
        className="border-border-strong bg-surface text-foreground rounded-md border px-3 py-1.5 text-xs font-semibold hover:border-primary focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring">Leave practice</Link>
    </div>
  </header>;
}
