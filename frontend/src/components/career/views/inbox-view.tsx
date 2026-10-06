"use client";

import Link from "next/link";
import {
  CalendarDays,
  ChevronRight,
  Dumbbell,
  Handshake,
  Wallet,
  Wrench,
  type LucideIcon,
} from "lucide-react";

import { SpokeEmpty, SpokeShell, useSpokeGate } from "@/components/career/spoke";
import { useCareerInbox } from "@/lib/api/queries";

type InboxItem = { kind: string; count: number };

const ITEM_DETAILS: Record<
  string,
  { title: string; detail: (count: number) => string; path: string; icon: LucideIcon }
> = {
  SCHEDULE: {
    title: "Plan your season",
    detail: (count) =>
      `${count} eligible ${count === 1 ? "event is" : "events are"} ready to review.`,
    path: "/calendar",
    icon: CalendarDays,
  },
  DEVELOPMENT: {
    title: "Development points available",
    detail: (count) => `${count} point${count === 1 ? "" : "s"} can be allocated to your golfer.`,
    path: "/manage",
    icon: Dumbbell,
  },
  SPONSORSHIP: {
    title: "Sponsorship offers to review",
    detail: (count) =>
      `${count} offer${count === 1 ? "" : "s"} can be considered while your book has room.`,
    path: "/finances",
    icon: Wallet,
  },
  EQUIPMENT: {
    title: "Equipment options available",
    detail: (count) => `${count} gear ${count === 1 ? "option is" : "options are"} available now.`,
    path: "/equipment",
    icon: Wrench,
  },
  STAFF: {
    title: "Staff candidates to review",
    detail: (count) => `${count} candidate${count === 1 ? "" : "s"} can be hired now.`,
    path: "/staff",
    icon: Handshake,
  },
};

/** A compact action index; each linked spoke remains the authority for its decision. */
export function InboxView({ id }: { id: string }) {
  const query = useCareerInbox(id);
  const gate = useSpokeGate(query);
  if (gate) return gate;

  const items = (query.data?.careerInbox.items ?? []) as InboxItem[];
  return (
    <SpokeShell
      title="Inbox"
      description="Career decisions and opportunities that need your attention."
    >
      {items.length === 0 ? (
        <SpokeEmpty>
          You&apos;re all caught up. New opportunities will appear here when there&apos;s something
          useful to review.
        </SpokeEmpty>
      ) : (
        <div className="divide-divider border-border bg-surface flex flex-col divide-y overflow-hidden rounded-xl border">
          {items.map((item) => {
            const detail = ITEM_DETAILS[item.kind];
            if (!detail) return null;
            const Icon = detail.icon;
            return (
              <Link
                key={item.kind}
                href={`/career/${id}${detail.path}`}
                className="hover:bg-surface-elevated flex items-center gap-4 px-5 py-4 transition-colors"
              >
                <span className="bg-info/10 text-info grid size-10 shrink-0 place-items-center rounded-lg">
                  <Icon className="size-4" aria-hidden="true" />
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block font-medium">{detail.title}</span>
                  <span className="text-muted-foreground mt-0.5 block text-sm">
                    {detail.detail(item.count)}
                  </span>
                </span>
                <ChevronRight
                  className="text-subtle-foreground size-4 shrink-0"
                  aria-hidden="true"
                />
              </Link>
            );
          })}
        </div>
      )}
    </SpokeShell>
  );
}
