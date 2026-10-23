"use client";

import * as RadioGroup from "@radix-ui/react-radio-group";
import { Check } from "lucide-react";

import { cn } from "../../lib/utils";
import { ARCHETYPES, STARTING_BACKGROUNDS } from "../../lib/onboarding/options";

/*
 * Archetype selection — a Radix RadioGroup rendered as selectable cards, so the
 * player weighs each play-style's trade-offs (the build choice). Radix gives real
 * radio semantics: one selection, arrow-key navigation, and the global focus ring.
 */
interface ArchetypeSelectProps {
  value?: string;
  onValueChange: (value: string) => void;
  onBlur?: () => void;
  invalid?: boolean;
  describedBy?: string;
}

export function ArchetypeSelect({
  value,
  onValueChange,
  onBlur,
  invalid,
  describedBy,
}: ArchetypeSelectProps) {
  return (
    <ChoiceCards
      value={value}
      onValueChange={onValueChange}
      onBlur={onBlur}
      invalid={invalid}
      describedBy={describedBy}
      label="Playing-style archetype"
      options={ARCHETYPES}
    />
  );
}

export function StartingBackgroundSelect(props: ArchetypeSelectProps) {
  return <ChoiceCards {...props} label="Starting Background" options={STARTING_BACKGROUNDS} />;
}

function ChoiceCards({
  value,
  onValueChange,
  onBlur,
  invalid,
  describedBy,
  label,
  options,
}: ArchetypeSelectProps & {
  label: string;
  options: readonly { value: string; label: string; description: string }[];
}) {
  return (
    <RadioGroup.Root
      value={value}
      onValueChange={onValueChange}
      onBlur={onBlur}
      aria-label={label}
      aria-invalid={invalid || undefined}
      aria-describedby={describedBy}
      className="grid gap-3 sm:grid-cols-2"
    >
      {options.map((option) => (
        <RadioGroup.Item
          key={option.value}
          value={option.value}
          className={cn(
            "group border-border bg-surface flex flex-col items-start gap-1.5 rounded-lg border p-4 text-left",
            "transition-colors duration-[var(--duration-base)] ease-[var(--ease-out)]",
            "hover:border-primary/50 data-[state=checked]:border-primary data-[state=checked]:bg-primary/[0.06]",
          )}
        >
          <span className="flex w-full items-center justify-between gap-2">
            <span className="text-foreground font-medium">{option.label}</span>
            <span
              aria-hidden="true"
              className="border-border group-data-[state=checked]:border-primary group-data-[state=checked]:bg-primary flex size-[18px] shrink-0 items-center justify-center rounded-full border transition-colors"
            >
              <Check className="text-primary-foreground size-3 opacity-0 transition-opacity group-data-[state=checked]:opacity-100" />
            </span>
          </span>
          <span className="text-muted-foreground text-sm">{option.description}</span>
        </RadioGroup.Item>
      ))}
    </RadioGroup.Root>
  );
}
