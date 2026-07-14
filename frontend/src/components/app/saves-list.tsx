"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { format } from "date-fns";

import { Button } from "@/components/ui/button";
import { useSaves } from "@/lib/api/queries";
import { useLoadCareer } from "@/lib/api/mutations";
import { isUnauthorized } from "@/lib/api/graphql-client";

function formatSavedAt(savedAt: string): string {
  const date = new Date(savedAt);
  return Number.isNaN(date.getTime()) ? savedAt : format(date, "d MMM yyyy, HH:mm");
}

export function SavesList() {
  const router = useRouter();
  const { data, isPending, isError, error } = useSaves();

  // A 401 from the proxy means the session is gone — return to login.
  useEffect(() => {
    if (isError && isUnauthorized(error)) {
      router.push("/login");
      router.refresh();
    }
  }, [isError, error, router]);

  if (isPending) return <SavesSkeleton />;

  if (isError) {
    if (isUnauthorized(error)) return null;
    return (
      <p role="alert" className="text-destructive text-sm">
        Couldn&apos;t load your saves. Try again.
      </p>
    );
  }

  const saves = data.listSaves;
  if (saves.length === 0) return <SavesEmpty />;

  return (
    <ul className="divide-divider border-border bg-surface flex flex-col divide-y overflow-hidden rounded-lg border">
      {saves.map((save) => (
        <li key={save.saveId} className="flex items-center justify-between gap-4 px-5 py-4">
          <div className="flex flex-col gap-1">
            <span className="font-medium">
              Season {save.season} · Week {save.week}
            </span>
            <span className="text-muted-foreground text-sm">
              {save.playerGolferId ? "Career in progress" : "No player assigned"}
            </span>
          </div>
          <div className="flex items-center gap-5">
            <span className="text-subtle-foreground font-mono text-sm tabular-nums">
              {formatSavedAt(save.savedAt)}
            </span>
            <ResumeButton saveId={save.saveId} />
          </div>
        </li>
      ))}
    </ul>
  );
}

function ResumeButton({ saveId }: { saveId: string }) {
  const router = useRouter();
  const load = useLoadCareer();
  const [failed, setFailed] = useState(false);

  async function onResume() {
    setFailed(false);
    try {
      const sessionId = await load.mutateAsync(saveId);
      router.push(`/career/${sessionId}`);
    } catch (error) {
      if (isUnauthorized(error)) {
        router.push("/login");
        router.refresh();
        return;
      }
      setFailed(true);
    }
  }

  return (
    <div className="flex flex-col items-end gap-1">
      <Button variant="secondary" size="sm" onClick={onResume} disabled={load.isPending}>
        {load.isPending ? "Opening…" : "Resume"}
      </Button>
      {failed ? <span className="text-destructive text-xs">Couldn&apos;t open</span> : null}
    </div>
  );
}

function SavesEmpty() {
  return (
    <div className="border-border bg-surface flex flex-col items-center gap-5 rounded-lg border border-dashed px-6 py-16 text-center">
      <div className="flex flex-col gap-2">
        <p className="text-foreground font-serif text-xl">Start your first career</p>
        <p className="text-muted-foreground max-w-sm text-sm">
          Create a golfer and guide them from the Development tour toward a legacy.
        </p>
      </div>
      <Button asChild size="lg">
        <Link href="/new">Start a new career</Link>
      </Button>
    </div>
  );
}

function SavesSkeleton() {
  return (
    <div
      aria-hidden="true"
      className="divide-divider border-border bg-surface flex flex-col divide-y overflow-hidden rounded-lg border"
    >
      {Array.from({ length: 3 }).map((_, i) => (
        <div key={i} className="flex items-center justify-between px-5 py-4">
          <div className="flex flex-col gap-2">
            <div className="bg-divider h-4 w-40 animate-pulse rounded" />
            <div className="bg-divider h-3 w-28 animate-pulse rounded" />
          </div>
          <div className="bg-divider h-3 w-24 animate-pulse rounded" />
        </div>
      ))}
    </div>
  );
}
