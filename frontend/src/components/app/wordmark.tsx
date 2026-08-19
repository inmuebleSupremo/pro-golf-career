import Link from "next/link";
import { Flag } from "lucide-react";

import { cn } from "@/lib/utils";

/**
 * The Pro Golf Career brand mark — the flag tile plus the wordmark — matching the command-centre
 * sidebar so the pre-game screens (landing, auth, home) read as the same product. Renders as a link
 * when `href` is given, otherwise a plain block. `subtitle` shows the small caption beneath the name.
 */
export function Wordmark({
  href,
  subtitle,
  className,
}: {
  href?: string;
  subtitle?: string;
  className?: string;
}) {
  const inner = (
    <>
      <span className="from-primary grid size-9 place-items-center rounded-[10px] bg-gradient-to-br to-[color-mix(in_oklch,var(--primary),black_28%)] text-[var(--primary-foreground)] shadow-[0_6px_16px_-6px_color-mix(in_oklch,var(--primary),transparent_45%)]">
        <Flag className="size-[1.15rem]" aria-hidden="true" />
      </span>
      <span className="leading-tight">
        <span className="block text-[0.9375rem] font-bold tracking-[-0.02em]">Pro Golf Career</span>
        {subtitle ? (
          <span className="text-subtle-foreground block text-[0.6875rem] font-medium">{subtitle}</span>
        ) : null}
      </span>
    </>
  );

  const classes = cn("inline-flex items-center gap-3", className);
  return href ? (
    <Link href={href} className={classes}>
      {inner}
    </Link>
  ) : (
    <span className={classes}>{inner}</span>
  );
}
