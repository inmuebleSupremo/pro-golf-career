"use client";

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
  LayoutDashboard,
  Medal,
  Newspaper,
  ScrollText,
  Trophy,
  User,
  Users,
  Wallet,
  Wrench,
  type LucideIcon,
} from "lucide-react";

import { cn } from "@/lib/utils";

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
  { items: [{ label: "Hub", icon: LayoutDashboard, path: "", exact: true }] },
  {
    label: "Compete",
    items: [
      { label: "Schedule", icon: CalendarDays, path: "/calendar" },
      { label: "Play Event", icon: Flag, path: "/play" },
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
    label: "Manage",
    items: [
      { label: "Development", icon: Dumbbell, path: "/manage" },
      { label: "Equipment", icon: Wrench, path: "/equipment" },
      { label: "Staff", icon: Handshake, path: "/staff" },
      { label: "Finances", icon: Wallet, path: "/finances" },
      { label: "Fitness", icon: Activity, path: "/fitness" },
    ],
  },
];

export function Sidebar({ id }: { id: string }) {
  const pathname = usePathname();
  const base = `/career/${id}`;

  return (
    <aside className="border-border hidden w-64 shrink-0 flex-col gap-6 border-r bg-[var(--background)] p-4 lg:sticky lg:top-0 lg:flex lg:h-dvh">
      <Link href={base} className="flex items-center gap-3 rounded-lg px-2 py-1">
        <span className="from-primary grid size-9 place-items-center rounded-[10px] bg-gradient-to-br to-[color-mix(in_oklch,var(--primary),black_28%)] text-[var(--primary-foreground)] shadow-[0_6px_16px_-6px_color-mix(in_oklch,var(--primary),transparent_45%)]">
          <Flag className="size-[1.15rem]" aria-hidden="true" />
        </span>
        <span className="leading-tight">
          <span className="block text-[0.9375rem] font-bold tracking-[-0.02em]">Pro Golf Career</span>
          <span className="text-subtle-foreground block text-[0.6875rem] font-medium">Command Centre</span>
        </span>
      </Link>

      <nav className="flex flex-col gap-1 overflow-y-auto">
        {GROUPS.map((group, gi) => (
          <div key={group.label ?? gi} className={cn(gi > 0 && "mt-4")}>
            {group.label && (
              <p className="text-subtle-foreground px-3 pt-1.5 pb-1.5 text-[0.625rem] font-bold tracking-[0.14em] uppercase">
                {group.label}
              </p>
            )}
            {group.items.map((item) => (
              <NavRow key={item.label} item={item} base={base} pathname={pathname} />
            ))}
          </div>
        ))}
      </nav>
    </aside>
  );
}

function NavRow({ item, base, pathname }: { item: NavItem; base: string; pathname: string }) {
  const Icon = item.icon;
  const rowClass =
    "group relative flex items-center gap-2.5 rounded-[10px] px-2.5 py-2 text-[0.84375rem] font-medium transition-colors duration-[var(--duration-base)]";

  const href = `${base}${item.path}`;
  const active = item.exact ? pathname === href : pathname.startsWith(href);

  return (
    <Link
      href={href}
      className={cn(
        rowClass,
        active
          ? "bg-surface-elevated text-foreground"
          : "text-muted-foreground hover:bg-surface hover:text-foreground",
      )}
      aria-current={active ? "page" : undefined}
    >
      {active && (
        <span className="bg-primary absolute top-2 bottom-2 -left-4 w-[3px] rounded-r" aria-hidden="true" />
      )}
      <Icon className="size-[1.0625rem] shrink-0" aria-hidden="true" />
      <span>{item.label}</span>
    </Link>
  );
}
