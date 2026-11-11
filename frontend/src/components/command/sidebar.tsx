"use client";

import { useEffect, useRef, useState, type MouseEvent } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  Activity,
  Award,
  BarChart3,
  CalendarDays,
  Dumbbell,
  Flag,
  Globe,
  Handshake,
  Inbox,
  LayoutDashboard,
  Medal,
  Menu,
  Newspaper,
  ScrollText,
  Trophy,
  User,
  Users,
  Wallet,
  Wrench,
  X,
  type LucideIcon,
} from "lucide-react";

import { cn } from "@/lib/utils";
import { useCareerInbox } from "@/lib/api/queries";

type NavItem = {
  label: string;
  icon: LucideIcon;
  /** Relative to /career/[id]; "" is the hub itself. */
  path: string;
  /** exact = active only when the path matches exactly (the hub). */
  exact?: boolean;
};

type NavGroup = { label?: string; items: NavItem[] };

// Every item routes to a real spoke page. (Leaderboard is intentionally absent — it is
// live only inside an event, and the /play surface owns it.)
const GROUPS: NavGroup[] = [
  {
    items: [
      { label: "Hub", icon: LayoutDashboard, path: "", exact: true },
      { label: "Inbox", icon: Inbox, path: "/inbox" },
    ],
  },
  {
    label: "Compete",
    items: [
      { label: "Schedule", icon: CalendarDays, path: "/calendar" },
      { label: "Play Event", icon: Flag, path: "/play" },
      { label: "Practice Facility", icon: Activity, path: "/practice" },
    ],
  },
  {
    label: "Manage",
    items: [
      { label: "Development", icon: Dumbbell, path: "/manage" },
      { label: "Fitness", icon: Activity, path: "/fitness" },
      { label: "Equipment", icon: Wrench, path: "/equipment" },
      { label: "Staff", icon: Handshake, path: "/staff" },
      { label: "Finances", icon: Wallet, path: "/finances" },
    ],
  },
  {
    label: "Career",
    items: [
      { label: "Profile", icon: User, path: "/profile" },
      { label: "Season Stats", icon: BarChart3, path: "/seasons" },
      { label: "Career Records", icon: ScrollText, path: "/career-records" },
      { label: "Achievements", icon: Medal, path: "/achievements" },
      { label: "Hall of Fame", icon: Trophy, path: "/hall-of-fame" },
    ],
  },
  {
    label: "World",
    items: [
      { label: "News", icon: Newspaper, path: "/news" },
      { label: "Rankings", icon: Globe, path: "/rankings" },
      { label: "Rivals", icon: Users, path: "/rivals" },
      { label: "Records", icon: Award, path: "/records" },
    ],
  },
];

export function Sidebar({ id }: { id: string }) {
  return (
    <aside className="border-border hidden w-[13.5rem] shrink-0 flex-col border-r bg-[var(--background)] p-3 lg:sticky lg:top-[4.5rem] lg:flex lg:h-[calc(100dvh-4.5rem)] xl:w-56 2xl:w-[14.5rem]">
      <CareerNavigation id={id} />
    </aside>
  );
}

export function MobileCareerNavigation({ id }: { id: string }) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const closeRef = useRef<HTMLButtonElement>(null);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (!open) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const desktop = window.matchMedia("(min-width: 64rem)");
    const closeAtDesktop = () => {
      if (desktop.matches) dialogRef.current?.close();
    };
    desktop.addEventListener("change", closeAtDesktop);
    closeAtDesktop();
    return () => {
      document.body.style.overflow = previousOverflow;
      desktop.removeEventListener("change", closeAtDesktop);
    };
  }, [open]);

  function openDrawer() {
    dialogRef.current?.showModal();
    setOpen(true);
    closeRef.current?.focus();
  }

  function closeDrawer() {
    dialogRef.current?.close();
  }

  function onBackdropClick(event: MouseEvent<HTMLDialogElement>) {
    if (event.target !== event.currentTarget) return;
    const bounds = event.currentTarget.getBoundingClientRect();
    if (
      event.clientX < bounds.left ||
      event.clientX > bounds.right ||
      event.clientY < bounds.top ||
      event.clientY > bounds.bottom
    )
      closeDrawer();
  }

  return (
    <>
      <button
        ref={triggerRef}
        type="button"
        aria-label="Open career navigation"
        aria-haspopup="dialog"
        aria-expanded={open}
        aria-controls="career-navigation-drawer"
        onClick={openDrawer}
        className="border-border text-foreground hover:bg-surface-elevated focus-visible:outline-primary inline-flex size-10 shrink-0 items-center justify-center rounded-lg border focus-visible:outline-2 focus-visible:outline-offset-2 lg:hidden"
      >
        <Menu className="size-5" aria-hidden="true" />
      </button>
      <dialog
        ref={dialogRef}
        id="career-navigation-drawer"
        aria-labelledby="career-navigation-title"
        aria-modal="true"
        onClick={onBackdropClick}
        onKeyDown={(event) => {
          if (event.key === "Escape") {
            event.preventDefault();
            closeDrawer();
          }
        }}
        onClose={() => {
          setOpen(false);
          triggerRef.current?.focus();
        }}
        className="border-border bg-background text-foreground backdrop:bg-background/75 fixed inset-y-0 left-0 m-0 h-dvh max-h-dvh w-[min(20rem,calc(100vw-3rem))] max-w-none border-r p-4 shadow-[var(--shadow-lg)] lg:hidden"
      >
        <div className="flex h-full min-h-0 flex-col gap-6">
          <div className="flex items-start justify-between gap-2">
            <div id="career-navigation-title">
              <BrandLink base={`/career/${id}`} onNavigate={closeDrawer} />
            </div>
            <button
              ref={closeRef}
              type="button"
              aria-label="Close career navigation"
              onClick={closeDrawer}
              className="border-border text-muted-foreground hover:bg-surface hover:text-foreground focus-visible:outline-primary inline-flex size-9 shrink-0 items-center justify-center rounded-lg border focus-visible:outline-2 focus-visible:outline-offset-2"
            >
              <X className="size-4" aria-hidden="true" />
            </button>
          </div>
          <CareerNavigation id={id} onNavigate={closeDrawer} />
        </div>
      </dialog>
    </>
  );
}

function BrandLink({ base, onNavigate }: { base: string; onNavigate?: () => void }) {
  return (
    <Link href={base} onClick={onNavigate} className="flex items-center gap-3 rounded-lg px-2 py-1">
      <span className="from-primary grid size-9 place-items-center rounded-[10px] bg-gradient-to-br to-[color-mix(in_oklch,var(--primary),black_28%)] text-[var(--primary-foreground)] shadow-[0_6px_16px_-6px_color-mix(in_oklch,var(--primary),transparent_45%)]">
        <Flag className="size-[1.15rem]" aria-hidden="true" />
      </span>
      <span className="leading-tight">
        <span className="block text-[0.9375rem] font-bold tracking-[-0.02em]">Pro Golf Career</span>
        <span className="text-subtle-foreground block text-[0.6875rem] font-medium">
          Command Centre
        </span>
      </span>
    </Link>
  );
}

function CareerNavigation({ id, onNavigate }: { id: string; onNavigate?: () => void }) {
  const pathname = usePathname();
  const base = `/career/${id}`;
  const inbox = useCareerInbox(id);
  const inboxCount = inbox.data?.careerInbox.items.length ?? 0;

  return (
    <nav aria-label="Career navigation" className="flex min-h-0 flex-col gap-1 overflow-y-auto">
      {GROUPS.map((group, gi) => (
        <div key={group.label ?? gi} className={cn(gi > 0 && "mt-4 lg:mt-3")}>
          {group.label && (
            <p className="text-subtle-foreground px-3 pt-1.5 pb-1.5 text-[0.625rem] font-bold tracking-[0.14em] uppercase">
              {group.label}
            </p>
          )}
          {group.items.map((item) => (
            <NavRow
              key={item.label}
              item={item}
              base={base}
              pathname={pathname}
              badge={item.path === "/inbox" ? inboxCount : undefined}
              onNavigate={onNavigate}
            />
          ))}
        </div>
      ))}
    </nav>
  );
}

function NavRow({
  item,
  base,
  pathname,
  badge,
  onNavigate,
}: {
  item: NavItem;
  base: string;
  pathname: string;
  badge?: number;
  onNavigate?: () => void;
}) {
  const Icon = item.icon;
  const rowClass =
    "group relative flex items-center gap-2.5 rounded-[10px] px-2.5 py-2 text-[0.84375rem] font-medium transition-colors duration-[var(--duration-base)]";

  const href = `${base}${item.path}`;
  const active = item.exact ? pathname === href : pathname.startsWith(href);

  return (
    <Link
      href={href}
      onClick={onNavigate}
      className={cn(
        rowClass,
        active
          ? "bg-primary/20 text-foreground"
          : "text-muted-foreground hover:bg-primary/10 hover:text-foreground",
      )}
      aria-current={active ? "page" : undefined}
    >
      {active && (
        <span
          className="bg-primary absolute top-2 bottom-2 -left-3 w-[3px] rounded-r"
          aria-hidden="true"
        />
      )}
      <Icon className="size-[1.0625rem] shrink-0" aria-hidden="true" />
      <span>{item.label}</span>
      {badge ? (
        <span className="bg-info/14 text-info ml-auto min-w-5 rounded-full px-1.5 py-0.5 text-center font-mono text-[0.6875rem] font-bold tabular-nums">
          {badge}
        </span>
      ) : null}
    </Link>
  );
}
