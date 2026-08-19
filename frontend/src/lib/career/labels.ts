/*
 * Display labels for the engine enums the career hub renders. Mirrors the backend
 * enums (the backend stays the source of truth); an unknown name falls back to the
 * raw value.
 */

const TOUR_TIER_LABELS: Record<string, string> = {
  DEVELOPMENT: "Development",
  PRO: "Pro",
};

const EVENT_PRESTIGE_LABELS: Record<string, string> = {
  REGULAR: "Regular",
  SIGNATURE: "Signature",
  TOUR_CHAMPIONSHIP: "Tour Championship",
  MAJOR: "Major",
};

export function tourTierLabel(tier: string): string {
  return TOUR_TIER_LABELS[tier] ?? tier;
}

export function eventPrestigeLabel(prestige: string): string {
  return EVENT_PRESTIGE_LABELS[prestige] ?? prestige;
}

const moneyFormatter = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
  maximumFractionDigits: 0,
});

/** Format a currency amount (accepts a number or a numeric string). */
export function formatMoney(value: number | string): string {
  const n = Number(value);
  return Number.isFinite(n) ? moneyFormatter.format(n) : String(value);
}

/** A stroke total relative to par: "E", "+3", or "-8". */
export function formatScore(score: number): string {
  if (score === 0) return "E";
  return score > 0 ? `+${score}` : `${score}`;
}

const RECORD_TYPE_LABELS: Record<string, string> = {
  MOST_CAREER_WINS: "Most career wins",
  MOST_MAJOR_WINS: "Most major wins",
  LOWEST_TOURNAMENT_SCORE: "Lowest tournament score",
  MOST_CONSECUTIVE_CUTS: "Most consecutive cuts",
  LONGEST_CAREER: "Longest career",
};

/** The record type as a display label (falls back to the raw name). */
export function recordTypeLabel(type: string): string {
  return RECORD_TYPE_LABELS[type] ?? type;
}

/**
 * A record's value formatted for its type: a score-to-par for the lowest-score record, otherwise a counted
 * quantity with its unit ("14 wins", "9 seasons"). The engine stores every value as a number.
 */
export function recordValueText(type: string, value: number): string {
  if (type === "LOWEST_TOURNAMENT_SCORE") return formatScore(Math.round(value));
  const n = Math.round(value);
  const unit: Record<string, [string, string]> = {
    MOST_CAREER_WINS: ["win", "wins"],
    MOST_MAJOR_WINS: ["major", "majors"],
    MOST_CONSECUTIVE_CUTS: ["cut", "cuts"],
    LONGEST_CAREER: ["season", "seasons"],
  };
  const pair = unit[type];
  return pair ? `${n} ${n === 1 ? pair[0] : pair[1]}` : String(n);
}

const ATTRIBUTE_SHORT_LABELS: Record<string, string> = {
  DRIVING_ACCURACY: "Driving Acc.",
  DRIVING_DISTANCE: "Driving Dist.",
  IRONS_ACCURACY: "Irons Acc.",
  IRONS_CONTROL: "Irons Ctrl.",
  WEDGES: "Wedges",
  PUTTING_ACCURACY: "Putting Acc.",
  PUTTING_PROXIMITY: "Putting Prox.",
  COMPOSURE: "Composure",
  COURSE_MANAGEMENT: "Course Mgmt.",
};

/** A compact attribute label for tight spots (radar axes, hub bars). Falls back to the raw name. */
export function attributeShortLabel(attribute: string): string {
  return ATTRIBUTE_SHORT_LABELS[attribute] ?? attribute;
}

/** A finishing position as an ordinal: 1 → "1st", 12 → "12th". */
export function ordinalPosition(position: number): string {
  const suffix =
    position % 100 >= 11 && position % 100 <= 13
      ? "th"
      : (["th", "st", "nd", "rd"][position % 10] ?? "th");
  return `${position}${suffix}`;
}
