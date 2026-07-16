/*
 * Onboarding option data, mirroring the engine enums. These lists are a UX
 * convenience; the backend is the real validator (a bad enum name → BAD_REQUEST
 * on createPlayer). A drifted list only under-offers options, never breaks creation.
 */

/*
 * Backend-enforced starting-age range. createPlayer stacks two checks — the golfer
 * factory (16–30) and the Career constructor (CareerConstants, 16–22) — so the
 * effective range is their intersection, 16–22. A value outside it is a BAD_REQUEST.
 */
export const MIN_START_AGE = 16;
export const MAX_START_AGE = 22;
export const DEFAULT_START_AGE = 21;

export interface NationalityOption {
  /** Engine `Nationality` enum name (submitted as-is). */
  value: string;
  label: string;
}

/** The 16 engine `Nationality` values with display labels. */
export const NATIONALITIES: readonly NationalityOption[] = [
  { value: "USA", label: "United States" },
  { value: "GBR", label: "United Kingdom" },
  { value: "IRL", label: "Ireland" },
  { value: "ESP", label: "Spain" },
  { value: "ZAF", label: "South Africa" },
  { value: "AUS", label: "Australia" },
  { value: "JPN", label: "Japan" },
  { value: "KOR", label: "South Korea" },
  { value: "SWE", label: "Sweden" },
  { value: "GER", label: "Germany" },
  { value: "FRA", label: "France" },
  { value: "CAN", label: "Canada" },
  { value: "ARG", label: "Argentina" },
  { value: "NZL", label: "New Zealand" },
  { value: "ITA", label: "Italy" },
  { value: "MEX", label: "Mexico" },
];

export interface ArchetypeOption {
  /** Engine `Archetype` enum name (submitted as-is). */
  value: string;
  label: string;
  /** One-line play-style summary derived from the archetype's strengths/weaknesses. */
  description: string;
}

/**
 * The five playing-style archetypes offered at create-your-golfer. The three
 * background archetypes (GRASS_ROOTS_TALENT, TOP_COLLEGE_GRADUATE, FUTURE_PRODIGY)
 * are AI-only and deliberately excluded.
 */
export const ARCHETYPES: readonly ArchetypeOption[] = [
  {
    value: "POWER_HITTER",
    label: "Power Hitter",
    description: "Length off the tee and attacking irons — at the cost of touch around the greens.",
  },
  {
    value: "PRECISION_PLAYER",
    label: "Precision Player",
    description: "Fairways and greens with pinpoint accuracy, giving up raw distance.",
  },
  {
    value: "SHORT_GAME_ARTIST",
    label: "Short-Game Artist",
    description: "A deadly wedge and putter to score from anywhere; not the longest off the tee.",
  },
  {
    value: "ALL_ROUNDER",
    label: "All-Rounder",
    description: "No glaring weakness and no standout strength — balanced across the bag.",
  },
  {
    value: "MENTAL_FORTRESS",
    label: "Mental Fortress",
    description: "Ice-cold composure and course management when it matters most.",
  },
];

const NATIONALITY_LABELS = new Map(NATIONALITIES.map((n) => [n.value, n.label]));
const ARCHETYPE_LABELS = new Map(ARCHETYPES.map((a) => [a.value, a.label]));

export function nationalityLabel(value: string): string {
  return NATIONALITY_LABELS.get(value) ?? value;
}

export function archetypeLabel(value: string): string {
  return ARCHETYPE_LABELS.get(value) ?? value;
}
