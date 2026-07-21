"use client";

import { useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";

import { Button } from "@/components/ui/button";
import { isNotFound, isUnauthorized } from "@/lib/api/graphql-client";

/*
 * Shared scaffolding for the hub's spoke pages. The command-centre shell already
 * supplies the sidebar + identity strip, so a spoke is just a titled panel: this
 * gives every spoke the same heading idiom (bold sans, matching the hub) and the
 * same query-state handling (skeleton, unauthorized redirect, dead-session notice)
 * so each view stays focused on its own content.
 */

export function SpokeShell({
  title,
  description,
  action,
  children,
}: {
  title: string;
  description?: string;
  action?: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
        <h1 className="text-[1.45rem] font-bold tracking-[-0.025em]">{title}</h1>
        {description ? (
          <span className="text-muted-foreground text-sm">{description}</span>
        ) : null}
        {action ? <div className="ml-auto">{action}</div> : null}
      </div>
      {children}
    </div>
  );
}

type QueryState = { isPending: boolean; isError: boolean; error: unknown };

/**
 * Handles the shared query lifecycle for a spoke. Returns a node to render (skeleton
 * or message) while the data is unavailable, or null once the caller can render its
 * content. Redirects to login on an auth failure, mirroring the hub.
 */
export function useSpokeGate({ isPending, isError, error }: QueryState): React.ReactNode | null {
  const router = useRouter();

  useEffect(() => {
    if (isError && isUnauthorized(error)) {
      router.push("/login");
      router.refresh();
    }
  }, [isError, error, router]);

  if (isPending) return <SpokeSkeleton />;

  if (isError) {
    if (isUnauthorized(error)) return null;
    // An unknown/expired session (e.g. after a restart) comes back NOT_FOUND — the
    // ephemeral session is gone; guide the player back to the durable save (design D1).
    if (isNotFound(error)) {
      return (
        <SpokeMessage
          title="This session is no longer available"
          body="Loaded careers are held in memory and don't survive a restart. Reopen it from your saves."
        />
      );
    }
    return (
      <SpokeMessage
        title="Couldn't load this page"
        body="Something went wrong. Head back to your hub and try again."
      />
    );
  }

  return null;
}

export function SpokeMessage({ title, body }: { title: string; body: string }) {
  return (
    <div className="flex flex-col items-center gap-4 py-16 text-center">
      <div className="flex flex-col gap-2">
        <p className="text-foreground text-xl font-bold tracking-[-0.02em]">{title}</p>
        <p className="text-muted-foreground max-w-sm text-sm">{body}</p>
      </div>
      <Button asChild>
        <Link href="/saves">Back to saves</Link>
      </Button>
    </div>
  );
}

/** An empty-state note in the divided-list idiom. */
export function SpokeEmpty({ children }: { children: React.ReactNode }) {
  return (
    <p className="border-border bg-surface text-muted-foreground rounded-lg border border-dashed px-5 py-8 text-center text-sm">
      {children}
    </p>
  );
}

function SpokeSkeleton() {
  return (
    <div aria-hidden="true" className="flex flex-col gap-6">
      <div className="bg-divider h-8 w-40 animate-pulse rounded" />
      <div className="border-border bg-surface h-40 animate-pulse rounded-lg border" />
      <div className="border-border bg-surface h-40 animate-pulse rounded-lg border" />
    </div>
  );
}
