/*
 * Shot-decision option data, mirroring the engine Club and Strategy enums.
 * The backend validates the submitted enum names; these are the offered choices.
 */

export interface Option {
  value: string;
  label: string;
}

export const CLUBS: readonly Option[] = [
  { value: "DRIVER", label: "Driver" },
  { value: "FAIRWAY_WOOD", label: "Fairway wood" },
  { value: "HYBRID", label: "Hybrid" },
  { value: "IRON", label: "Iron" },
  { value: "WEDGE", label: "Wedge" },
  { value: "PUTTER", label: "Putter" },
];

export const STRATEGIES: readonly Option[] = [
  { value: "CONSERVATIVE", label: "Conservative" },
  { value: "BALANCED", label: "Balanced" },
  { value: "AGGRESSIVE", label: "Aggressive" },
];

/** Turn an engine enum name (e.g. FAIRWAY_WOOD, DEEP_ROUGH) into readable text ("Fairway wood"). */
export function humanize(name: string): string {
  const lower = name.replace(/_/g, " ").toLowerCase();
  return lower.charAt(0).toUpperCase() + lower.slice(1);
}
