"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { format } from "date-fns";
import { Play, Plus } from "lucide-react";

import { Button } from "@/components/ui/button";
import { useSaves } from "@/lib/api/queries";
import { useDeleteSave, useLoadCareer } from "@/lib/api/mutations";
import { isUnauthorized } from "@/lib/api/graphql-client";

type Save = {
  saveId: string;
  savedAt: string;
  season: number;
  week: number;
  playerGolferId: string | null;
};

function formatSavedAt(savedAt: string): string {
  const date = new Date(savedAt);
  return Number.isNaN(date.getTime()) ? savedAt : format(date, "d MMM yyyy, HH:mm");
}

/**
 * The post-auth home menu (spec: entry-flow): a prominent "Continue" that resumes the most recent
 * career, a clear "New career" path, and — separately — the rest of the saved games to browse and
 * play. Replaces the flat saves list so the common case (jump back in) is one click and the library
 * is a distinct, secondary option.
 */
export function HomeMenu() {
  const router = useRouter();
  const { data, isPending, isError, error } = useSaves();

  useEffect(() => {
    if (isError && isUnauthorized(error)) {
      router.push("/login");
      router.refresh();
    }
  }, [isError, error, router]);

  if (isPending) return <HomeSkeleton />;

  if (isError) {
    if (isUnauthorized(error)) return null;
    return (
      <p role="alert" className="text-destructive text-sm">
        Couldn&apos;t load your saves. Try again.
      </p>
    );
  }

  const saves: Save[] = [...data.listSaves].sort(
    (a, b) => new Date(b.savedAt).getTime() - new Date(a.savedAt).getTime(),
  );

  if (saves.length === 0) return <FirstCareer />;

  const [mostRecent, ...others] = saves;

  return (
    <div className="flex flex-col gap-8">
      <header className="flex flex-col gap-2">
        <h1 className="text-3xl font-bold tracking-[-0.025em]">Welcome back</h1>
        <p className="text-muted-foreground">Jump back into your career, or start something new.</p>
      </header>

      <ContinueHero save={mostRecent} />

      {others.length > 0 ? (
        <section className="flex flex-col gap-3">
          <h2 className="text-subtle-foreground text-[0.6875rem] font-bold tracking-[0.14em] uppercase">
            Other saves
          </h2>
          <ul className="divide-divider border-border bg-surface flex flex-col divide-y overflow-hidden rounded-lg border">
            {others.map((save) => (
              <li key={save.saveId} className="flex items-center justify-between gap-4 px-5 py-4">
                <div className="flex min-w-0 flex-col gap-1">
                  <span className="font-medium">
                    Season {save.season} · Week {save.week}
                  </span>
                  <span className="text-muted-foreground text-sm">
                    {save.playerGolferId ? "Career in progress" : "No player assigned"}
                  </span>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-subtle-foreground mr-1 hidden font-mono text-sm tabular-nums sm:inline">
                    {formatSavedAt(save.savedAt)}
                  </span>
                  <DeleteControl saveId={save.saveId} />
                  <ResumeButton saveId={save.saveId} label="Resume" variant="secondary" />
                </div>
              </li>
            ))}
          </ul>
        </section>
      ) : null}
    </div>
  );
}

/** The hero: continue the most recent career, or branch to a new one. */
function ContinueHero({ save }: { save: Save }) {
  return (
    <section className="border-border bg-surface rounded-xl border p-6 shadow-[var(--shadow-md)] sm:p-7">
      <p className="text-subtle-foreground font-mono text-[0.7rem] tracking-[0.16em] uppercase">
        Continue
      </p>
      <div className="mt-2.5 flex flex-wrap items-end justify-between gap-x-6 gap-y-5">
        <div className="flex min-w-0 flex-col gap-1.5">
          <h2 className="text-2xl font-bold tracking-[-0.02em]">
            Season {save.season} · Week {save.week}
          </h2>
          <p className="text-muted-foreground text-sm">
            {save.playerGolferId ? "Career in progress" : "No player assigned"} · saved{" "}
            <span className="tabular-nums">{formatSavedAt(save.savedAt)}</span>
          </p>
        </div>
        <div className="flex items-center gap-3">
          <ResumeButton saveId={save.saveId} label="Continue" size="lg" icon />
          <Button asChild variant="secondary" size="lg">
            <Link href="/new">
              <Plus className="size-4" aria-hidden="true" />
              New career
            </Link>
          </Button>
        </div>
      </div>
      <div className="border-divider mt-5 border-t pt-4">
        <DeleteControl saveId={save.saveId} />
      </div>
    </section>
  );
}

function ResumeButton({
  saveId,
  label,
  variant,
  size = "sm",
  icon,
}: {
  saveId: string;
  label: string;
  variant?: "secondary";
  size?: "sm" | "lg";
  icon?: boolean;
}) {
  const router = useRouter();
  const load = useLoadCareer();
  const [failed, setFailed] = useState(false);

  async function onResume() {
    setFailed(false);
    try {
      const sessionId = await load.mutateAsync(saveId);
      router.push(`/career/${sessionId}`);
    } catch (err) {
      if (isUnauthorized(err)) {
        router.push("/login");
        router.refresh();
        return;
      }
      setFailed(true);
    }
  }

  return (
    <div className="flex flex-col items-end gap-1">
      <Button variant={variant} size={size} onClick={onResume} disabled={load.isPending}>
        {icon && !load.isPending ? <Play className="size-4" aria-hidden="true" /> : null}
        {load.isPending ? "Opening…" : label}
      </Button>
      {failed ? <span className="text-destructive text-xs">Couldn&apos;t open</span> : null}
    </div>
  );
}

/**
 * Deleting a save is permanent — the career cannot be recovered — so the action asks for an
 * explicit confirmation in place before it fires.
 */
function DeleteControl({ saveId }: { saveId: string }) {
  const router = useRouter();
  const remove = useDeleteSave();
  const [confirming, setConfirming] = useState(false);

  async function onDelete() {
    try {
      await remove.mutateAsync(saveId); // the list refreshes via query invalidation
    } catch (err) {
      if (isUnauthorized(err)) {
        router.push("/login");
        router.refresh();
      }
    }
  }

  if (!confirming) {
    return (
      <Button variant="ghost" size="sm" onClick={() => setConfirming(true)}>
        Delete
      </Button>
    );
  }

  return (
    <span className="flex items-center gap-2">
      <span className="text-muted-foreground text-sm">Delete permanently?</span>
      <Button variant="destructive" size="sm" onClick={onDelete} disabled={remove.isPending}>
        {remove.isPending ? "Deleting…" : "Delete"}
      </Button>
      <Button variant="ghost" size="sm" onClick={() => setConfirming(false)} disabled={remove.isPending}>
        Cancel
      </Button>
      {remove.isError ? <span className="text-destructive text-xs">Couldn&apos;t delete</span> : null}
    </span>
  );
}

/** No saves yet — lead straight into creating the first golfer. */
function FirstCareer() {
  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center">
      <div className="border-border bg-surface flex max-w-md flex-col items-center gap-5 rounded-xl border px-6 py-14 text-center shadow-[var(--shadow-md)]">
        <div className="flex flex-col gap-2">
          <h1 className="text-2xl font-bold tracking-[-0.02em]">Start your first career</h1>
          <p className="text-muted-foreground text-sm">
            Create a golfer and guide them from the Development tour toward a legacy.
          </p>
        </div>
        <Button asChild size="lg">
          <Link href="/new">
            <Plus className="size-4" aria-hidden="true" />
            Create your golfer
          </Link>
        </Button>
      </div>
    </div>
  );
}

function HomeSkeleton() {
  return (
    <div aria-hidden="true" className="flex flex-col gap-8">
      <div className="flex flex-col gap-2">
        <div className="bg-divider h-8 w-48 animate-pulse rounded" />
        <div className="bg-divider h-4 w-72 animate-pulse rounded" />
      </div>
      <div className="border-border bg-surface h-40 animate-pulse rounded-xl border" />
      <div className="border-border bg-surface h-24 animate-pulse rounded-lg border" />
    </div>
  );
}
