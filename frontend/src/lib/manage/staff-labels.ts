/*
 * Player-facing descriptions of what each staff role does. True to the underlying mechanics (a coach speeds
 * development, a fitness coach builds conditioning, etc.) but phrased for the player rather than exposing the
 * exact formulae — a touch of information, not a spec.
 */

const STAFF_ROLE_EFFECT: Record<string, string> = {
  COACH: "Speeds up how quickly your attributes develop each season.",
  FITNESS_COACH: "Builds your long-term conditioning and eases fatigue.",
  PHYSIOTHERAPIST: "Speeds your recovery between events.",
  SPORTS_PSYCHOLOGIST: "Keeps you composed when the pressure is on.",
  CADDIE: "Sharper reads and fewer mistakes off the tee and around the green.",
};

/** What this staff role contributes, in player-facing terms. */
export function staffRoleEffect(role: string): string {
  return STAFF_ROLE_EFFECT[role] ?? "";
}

/** A short standing label for a quality tier (1–3), for flavour alongside the stars. */
export function staffTierLabel(tier: number): string {
  if (tier >= 3) return "Elite";
  if (tier === 2) return "Established";
  return "Up-and-coming";
}
