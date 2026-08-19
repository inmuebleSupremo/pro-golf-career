"use client";

import { Lock, Trophy } from "lucide-react";

import { SpokeShell, SpokeEmpty, useSpokeGate } from "@/components/career/spoke";
import { useAchievements } from "@/lib/api/queries";

type Achievement = {
  id: string;
  category: string;
  categoryLabel: string;
  title: string;
  description: string | null;
  secret: boolean;
  unlocked: boolean;
  seasonUnlocked: number | null;
};

type Group = { label: string; items: Achievement[] };

/** Groups the flat catalogue into its categories, preserving the backend's display order. */
function groupByCategory(items: Achievement[]): Group[] {
  const groups: Group[] = [];
  for (const item of items) {
    let group = groups.find((g) => g.label === item.categoryLabel);
    if (!group) {
      group = { label: item.categoryLabel, items: [] };
      groups.push(group);
    }
    group.items.push(item);
  }
  return groups;
}

/** Achievements spoke: the full catalogue, grouped by theme, with each feat's unlock state. */
export function AchievementsView({ id }: { id: string }) {
  const query = useAchievements(id);
  const gate = useSpokeGate(query);
  if (gate) return gate;

  const achievements: Achievement[] = query.data?.achievements ?? [];
  const unlockedCount = achievements.filter((a) => a.unlocked).length;
  const groups = groupByCategory(achievements);

  return (
    <SpokeShell
      title="Achievements"
      description="The feats and milestones your career has unlocked."
      action={
        achievements.length > 0 ? (
          <span className="text-subtle-foreground font-mono text-sm tabular-nums">
            <span className="text-gold font-semibold">{unlockedCount}</span> / {achievements.length}
          </span>
        ) : null
      }
    >
      {achievements.length === 0 ? (
        <SpokeEmpty>Achievements appear once your golfer is created — then go earn them.</SpokeEmpty>
      ) : (
        <div className="flex flex-col gap-9">
          {groups.map((group) => (
            <section key={group.label} className="flex flex-col gap-3">
              <h2 className="text-subtle-foreground text-[0.6875rem] font-bold tracking-[0.14em] uppercase">
                {group.label}
              </h2>
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                {group.items.map((achievement) => (
                  <AchievementCard key={achievement.id} achievement={achievement} />
                ))}
              </div>
            </section>
          ))}
        </div>
      )}
    </SpokeShell>
  );
}

function AchievementCard({ achievement }: { achievement: Achievement }) {
  const { unlocked, secret, seasonUnlocked } = achievement;
  const hidden = secret && !unlocked;
  const title = hidden ? "Secret achievement" : achievement.title;
  const description = hidden ? "Keep playing to discover this one." : achievement.description;

  return (
    <div
      className={[
        "flex items-start gap-3 rounded-lg border p-4 transition-colors",
        unlocked
          ? "border-gold/25 bg-gold/[0.06]"
          : "border-border bg-surface",
      ].join(" ")}
    >
      <span
        className={[
          "mt-0.5 grid size-8 shrink-0 place-items-center rounded-full",
          unlocked ? "bg-gold/15 text-gold" : "bg-surface-3 text-subtle-foreground",
        ].join(" ")}
        aria-hidden="true"
      >
        {unlocked ? <Trophy className="size-4" /> : <Lock className="size-4" />}
      </span>
      <div className="flex min-w-0 flex-col gap-1">
        <span className={`font-medium ${unlocked ? "text-foreground" : "text-muted-foreground"}`}>
          {title}
        </span>
        {description ? (
          <span className="text-muted-foreground text-sm">{description}</span>
        ) : null}
        {unlocked && seasonUnlocked != null ? (
          <span className="text-gold/80 mt-0.5 font-mono text-[0.6875rem] tracking-wide tabular-nums uppercase">
            Unlocked · Season {seasonUnlocked}
          </span>
        ) : null}
      </div>
    </div>
  );
}
