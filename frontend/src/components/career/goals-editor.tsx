"use client";

import { useState } from "react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useSetCareerGoals } from "@/lib/api/goals";
import type { CareerGoalInput } from "@/lib/graphql/generated/graphql";
import { GOAL_OPTIONS, type GoalOption, goalLabel } from "@/lib/career/labels";

type Goal = { type: string; target: string };
type Draft = Record<string, { selected: boolean; target: number }>;

function initialDraft(goals: Goal[]): Draft {
  const byType = new Map(goals.map((g) => [g.type, g]));
  const draft: Draft = {};
  for (const option of GOAL_OPTIONS) {
    const existing = byType.get(option.type);
    draft[option.type] = {
      selected: existing != null,
      target: existing ? Number(existing.target) : (option.defaultTarget ?? 1),
    };
  }
  return draft;
}

/** Choose the player's career goals: toggle each ambition, set a target for the numeric ones, then save. */
export function GoalsEditor({
  id,
  goals,
  onDone,
}: {
  id: string;
  goals: Goal[];
  onDone: () => void;
}) {
  const [draft, setDraft] = useState<Draft>(() => initialDraft(goals));
  const save = useSetCareerGoals(id);

  function toggle(type: string) {
    setDraft((prev) => ({ ...prev, [type]: { ...prev[type], selected: !prev[type].selected } }));
  }

  function setTarget(type: string, value: number) {
    setDraft((prev) => ({ ...prev, [type]: { ...prev[type], target: value } }));
  }

  async function onSave() {
    const chosen: CareerGoalInput[] = GOAL_OPTIONS.filter((o) => draft[o.type].selected).map((o) =>
      // Boolean goals omit the target (the engine defaults it to 1); targeted goals send it as a Long string.
      o.targeted
        ? { type: o.type, target: String(Math.max(1, Math.round(draft[o.type].target))) }
        : { type: o.type },
    );
    await save.mutateAsync(chosen);
    onDone();
  }

  return (
    <div className="border-border bg-surface flex flex-col gap-4 rounded-lg border p-5">
      <ul className="flex flex-col gap-2">
        {GOAL_OPTIONS.map((option) => (
          <GoalRow
            key={option.type}
            option={option}
            state={draft[option.type]}
            onToggle={() => toggle(option.type)}
            onTarget={(v) => setTarget(option.type, v)}
          />
        ))}
      </ul>

      <div className="flex items-center gap-3">
        <Button onClick={onSave} disabled={save.isPending}>
          {save.isPending ? "Saving…" : "Save goals"}
        </Button>
        <Button variant="ghost" onClick={onDone} disabled={save.isPending}>
          Cancel
        </Button>
        {save.isError ? (
          <span className="text-destructive text-sm">Couldn&apos;t save — try again.</span>
        ) : null}
      </div>
    </div>
  );
}

function GoalRow({
  option,
  state,
  onToggle,
  onTarget,
}: {
  option: GoalOption;
  state: { selected: boolean; target: number };
  onToggle: () => void;
  onTarget: (value: number) => void;
}) {
  return (
    <li className="flex flex-wrap items-center justify-between gap-3">
      <button
        type="button"
        onClick={onToggle}
        aria-pressed={state.selected}
        className={`flex items-center gap-3 rounded-md px-1 py-1.5 text-left text-sm transition-colors ${
          state.selected ? "text-foreground font-medium" : "text-muted-foreground hover:text-foreground"
        }`}
      >
        <span
          className={`flex size-4 items-center justify-center rounded border transition-colors ${
            state.selected ? "border-primary bg-primary text-primary-foreground" : "border-border"
          }`}
          aria-hidden="true"
        >
          {state.selected ? <CheckMark /> : null}
        </span>
        {goalLabel(option.type)}
      </button>

      {option.targeted && state.selected ? (
        <label className="text-muted-foreground flex items-center gap-2 text-sm">
          <span>{option.targetKind === "money" ? "Amount ($)" : "Target"}</span>
          <Input
            type="number"
            min={1}
            step={option.targetKind === "money" ? 100000 : 1}
            value={state.target}
            onChange={(e) => onTarget(Number(e.target.value))}
            className="w-32"
          />
        </label>
      ) : null}
    </li>
  );
}

function CheckMark() {
  return (
    <svg viewBox="0 0 12 12" className="size-3" fill="none" stroke="currentColor" strokeWidth={2}>
      <path d="M2.5 6.5l2.5 2.5 4.5-5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
