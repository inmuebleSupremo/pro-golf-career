import Link from "next/link";
import { ArrowUpRight } from "lucide-react";

import { cn } from "@/lib/utils";

/*
 * The command-centre bento card. A surface tile with a restrained entrance.
 * Pass `href` to make it a portal into a full page — it gains a hover lift and a
 * corner arrow that the CardHeader reveals. Card size (grid span) is the hierarchy,
 * set by the caller via `className` (e.g. "col-span-2 row-span-2").
 */
export function Card({
  href,
  className,
  children,
}: {
  href?: string;
  className?: string;
  children: React.ReactNode;
}) {
  const base = cn(
    "group border-border from-surface-elevated to-surface relative flex flex-col rounded-xl border bg-gradient-to-b p-5 shadow-[var(--shadow-md)]",
    "[animation:cc-rise_0.5s_var(--ease-out)_backwards] transition-[transform,border-color] duration-[var(--duration-base)]",
    href && "hover:border-border-strong hover:-translate-y-0.5",
    className,
  );
  if (href) {
    return (
      <Link href={href} className={base}>
        {children}
      </Link>
    );
  }
  return <article className={base}>{children}</article>;
}

/*
 * The card's label row: an uppercase title, optional right-side content, and — inside
 * a portal card — a corner arrow that leans on hover.
 */
export function CardHeader({
  title,
  right,
  portal = false,
}: {
  title: string;
  right?: React.ReactNode;
  portal?: boolean;
}) {
  return (
    <div className="mb-4 flex items-center gap-2">
      <span className="text-muted-foreground text-[0.6875rem] font-bold tracking-[0.12em] uppercase">
        {title}
      </span>
      {right}
      {portal && (
        <ArrowUpRight
          className="text-subtle-foreground group-hover:text-primary ml-auto size-[0.9375rem] transition-[color,transform] duration-[var(--duration-base)] group-hover:translate-x-0.5"
          aria-hidden="true"
        />
      )}
    </div>
  );
}
