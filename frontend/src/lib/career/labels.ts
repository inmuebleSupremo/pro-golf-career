/*
 * Display labels for the engine enums the career hub renders, plus goal
 * progress formatting. Mirrors the backend enums (the backend stays the source
 * of truth); an unknown name falls back to the raw value.
 */

const GOAL_TYPE_LABELS: Record<string, string> = {
  REACH_TOP_TOUR: "Reach the top tour",
  WIN_A_MAJOR: "Win a major",
  WORLD_NUMBER_ONE: "World number one",
  CAREER_WINS: "Career wins",
  CAREER_EARNINGS: "Career earnings",
  HALL_OF_FAME: "Hall of Fame",
};

const TOUR_TIER_LABELS: Record<string, string> = {
  DEVELOPMENT: "Development",
  SECONDARY: "Secondary",
  PRIMARY: "Primary",
  ELITE: "Elite",
};

const EVENT_PRESTIGE_LABELS: Record<string, string> = {
  REGULAR: "Regular",
  SIGNATURE: "Signature",
  MAJOR: "Major",
};

/* Boolean-style goals are achieved when their condition holds (target is 1); the
 * rest track a numeric metric toward a target. */
const BOOLEAN_GOAL_TYPES = new Set(["REACH_TOP_TOUR", "WORLD_NUMBER_ONE", "HALL_OF_FAME"]);

export function goalLabel(type: string): string {
  return GOAL_TYPE_LABELS[type] ?? type;
}

export function tourTierLabel(tier: string): string {
  return TOUR_TIER_LABELS[tier] ?? tier;
}

export function eventPrestigeLabel(prestige: string): string {
  return EVENT_PRESTIGE_LABELS[prestige] ?? prestige;
}

export function isBooleanGoal(type: string): boolean {
  return BOOLEAN_GOAL_TYPES.has(type);
}

const moneyFormatter = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
  maximumFractionDigits: 0,
});

/** Progress text for a targeted goal, e.g. "2 / 5" or "$250,000 / $1,000,000". */
export function goalProgressText(type: string, current: string, target: string): string {
  if (type === "CAREER_EARNINGS") {
    return `${formatMoney(current)} / ${formatMoney(target)}`;
  }
  return `${current} / ${target}`;
}

/** Fractional progress in [0, 1] for a targeted goal (guards divide-by-zero). */
export function goalProgressRatio(current: string, target: string): number {
  const t = Number(target);
  const c = Number(current);
  if (!Number.isFinite(t) || t <= 0) return 0;
  return Math.min(1, Math.max(0, c / t));
}

function formatMoney(value: string): string {
  const n = Number(value);
  return Number.isFinite(n) ? moneyFormatter.format(n) : value;
}
