"use client";

import { useState } from "react";
import { Check } from "lucide-react";

import { Button } from "@/components/ui/button";
import { GoalsEditor } from "@/components/career/goals-editor";
import { SpokeShell, SpokeEmpty, useSpokeGate } from "@/components/career/spoke";
import { useCareerOverview } from "@/lib/api/queries";
import {
  goalLabel,
  goalProgressRatio,
  goalProgressText,
  isBooleanGoal,
} from "@/lib/career/labels";

type Goal = { type: string; target: string; current: string; achieved: boolean };

/** Career-goals spoke: view progress and edit the self-chosen ambitions (reuses GoalsEditor). */
export function GoalsView({ id }: { id: string }) {
  const query = useCareerOverview(id);
  const [editing, setEditing] = useState(false);
  const gate = useSpokeGate(query);
  if (gate) return gate;

  const goals: Goal[] = query.data?.careerGoals ?? [];

  return (
    <SpokeShell
      title="Career goals"
      description="What this career is chasing."
      action={
        !editing ? (
          <Button variant="ghost" size="sm" onClick={() => setEditing(true)}>
            {goals.length === 0 ? "Set goals" : "Edit"}
          </Button>
        ) : null
      }
    >
      {editing ? (
        <GoalsEditor id={id} goals={goals} onDone={() => setEditing(false)} />
      ) : goals.length === 0 ? (
        <SpokeEmpty>No goals set yet — choose what this career is chasing.</SpokeEmpty>
      ) : (
        <ul className="divide-divider border-border bg-surface flex flex-col divide-y overflow-hidden rounded-lg border">
          {goals.map((goal, i) => (
            <GoalRow key={`${goal.type}-${i}`} goal={goal} />
          ))}
        </ul>
      )}
    </SpokeShell>
  );
}

function GoalRow({ goal }: { goal: Goal }) {
  const targeted = !isBooleanGoal(goal.type);

  return (
    <li className="flex items-center justify-between gap-4 px-5 py-4">
      <div className="flex min-w-0 flex-1 flex-col gap-2">
        <span className="font-medium">{goalLabel(goal.type)}</span>
        {targeted ? (
          <div className="flex max-w-xs flex-col gap-1.5">
            <div className="bg-divider h-1.5 w-full overflow-hidden rounded-full">
              <div
                className="bg-primary h-full rounded-full"
                style={{ width: `${goalProgressRatio(goal.current, goal.target) * 100}%` }}
              />
            </div>
            <span className="text-subtle-foreground font-mono text-xs tabular-nums">
              {goalProgressText(goal.type, goal.current, goal.target)}
            </span>
          </div>
        ) : goal.achieved ? null : (
          <span className="text-muted-foreground text-sm">In progress</span>
        )}
      </div>
      {goal.achieved ? (
        <span className="bg-success/[0.12] text-success inline-flex shrink-0 items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium">
          <Check className="size-3.5" aria-hidden="true" />
          Achieved
        </span>
      ) : null}
    </li>
  );
}
