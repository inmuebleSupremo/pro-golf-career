/*
 * Shot router — the bridge between the game's shot inputs (club + target
 * distance + strategy) and the 2.5D visual physics engine.
 *
 * The engine animates a ball through a three-phase loop (air flight, ground
 * check, roll) driven entirely by a small physics profile. This module owns
 * the mapping from a player's decision to that profile. Every number here is
 * lifted verbatim from the `shotProfiles` table in the physics prototype — the
 * router selects a profile, it never invents one.
 *
 * The renderer reads five values off the returned profile:
 *   vz    — launch height (initial vertical velocity; 0 means a pure roll)
 *   curve — mid-air lateral shift (draw is +, fade is -)
 *   rest  — bounce restitution (how lively the first bounce is)
 *   bite  — spin decay applied to horizontal speed on the first bounce
 *   fric  — rolling deceleration once the ball is on the ground
 *
 * Target distance never appears in the returned profile; it drives the
 * engine's velocity calc (distance / air frames), and the router only consults
 * it to tell a full wedge from a greenside chip.
 */

export type ClubCategory = "Driver" | "Woods" | "Irons" | "Wedges" | "Putter";

export type PlayerStrategy = "Normal" | "Aggressive" | "Conservative";

/** The physics contract the animation renderer consumes — exactly these keys. */
export interface ShotPhysicsProfile {
  /** Launch height: initial upward velocity. 0 renders a pure ground roll. */
  readonly vz: number;
  /** Mid-air lateral shift. Positive draws right-to-left, negative fades. */
  readonly curve: number;
  /** Bounce restitution: fraction of vertical speed kept on each bounce. */
  readonly rest: number;
  /** Spin bite: fraction of horizontal speed shed on the first bounce. */
  readonly bite: number;
  /** Rolling friction: per-frame horizontal deceleration on the ground. */
  readonly fric: number;
}

/*
 * Named profiles, transcribed from the prototype's `shotProfiles` array.
 * Only the five physics fields survive here — the prototype's sx/sy/tx/ty are
 * staging coordinates the router replaces with the live target distance.
 */
const DRIVE_STRAIGHT: ShotPhysicsProfile = { vz: 8.5, curve: 0, rest: 0.35, bite: 0.3, fric: 0.035 };
const DRIVE_DRAW: ShotPhysicsProfile = { vz: 8.0, curve: 25, rest: 0.35, bite: 0.2, fric: 0.03 };
const DRIVE_FADE: ShotPhysicsProfile = { vz: 9.0, curve: -25, rest: 0.3, bite: 0.4, fric: 0.04 };
const STANDARD_IRON: ShotPhysicsProfile = { vz: 7.5, curve: 0, rest: 0.2, bite: 0.4, fric: 0.04 };
const STINGER: ShotPhysicsProfile = { vz: 3.5, curve: 0, rest: 0.4, bite: 0.1, fric: 0.035 };
const FULL_WEDGE: ShotPhysicsProfile = { vz: 9.5, curve: 0, rest: 0.15, bite: 0.6, fric: 0.05 };
const FLOP_SHOT: ShotPhysicsProfile = { vz: 11.5, curve: 0, rest: 0.05, bite: 0.8, fric: 0.1 };
const CHIP_AND_CHECK: ShotPhysicsProfile = { vz: 4.5, curve: 0, rest: 0.35, bite: 0.7, fric: 0.04 };
const BUMP_AND_RUN: ShotPhysicsProfile = { vz: 2.0, curve: 0, rest: 0.4, bite: 0.05, fric: 0.03 };
const STANDARD_PUTT: ShotPhysicsProfile = { vz: 0, curve: 0, rest: 0, bite: 0, fric: 0.015 };

/**
 * Below this carry (yards) a normal-strategy wedge plays as a greenside
 * chip-and-check rather than a full swing — the one place target distance
 * changes which profile the router hands back.
 */
export const GREENSIDE_MAX_YARDS = 30;

/**
 * Resolve a player's shot decision into a physics profile for the renderer.
 *
 * Coverage notes for combinations the prototype never authored:
 *  - Woods have no dedicated profile; they carry and run out like a driver, so
 *    they borrow the Driver family by strategy.
 *  - There is no aggressive iron in the prototype, so an aggressive iron falls
 *    back to the stock Standard Iron.
 *  - A putt is a putt: the Putter ignores strategy entirely.
 */
export function shotRouter(
  clubCategory: ClubCategory,
  targetDistance: number,
  playerStrategy: PlayerStrategy,
): ShotPhysicsProfile {
  // A putt is a putt — strategy makes no difference on the green.
  if (clubCategory === "Putter") {
    return STANDARD_PUTT;
  }

  // Woods have no profile of their own; treat them as drivers.
  const category: ClubCategory = clubCategory === "Woods" ? "Driver" : clubCategory;

  switch (category) {
    case "Driver":
      if (playerStrategy === "Aggressive") return DRIVE_DRAW;
      if (playerStrategy === "Conservative") return DRIVE_FADE;
      return DRIVE_STRAIGHT;

    case "Irons":
      if (playerStrategy === "Conservative") return STINGER;
      // No aggressive iron authored — the stock iron stands in.
      return STANDARD_IRON;

    case "Wedges":
      if (playerStrategy === "Aggressive") return FLOP_SHOT;
      if (playerStrategy === "Conservative") return BUMP_AND_RUN;
      // A normal wedge: greenside distances check up, full swings fly out.
      return targetDistance <= GREENSIDE_MAX_YARDS ? CHIP_AND_CHECK : FULL_WEDGE;

    default:
      // Unreachable (Putter/Woods are handled above); keep the stock iron as a
      // safe last resort rather than throwing inside the render path.
      return STANDARD_IRON;
  }
}

/*
 * ── Bridge from the live game's enums ──────────────────────────────────────
 * The play UI submits the engine's own enum names (see src/lib/play/options.ts),
 * which use a finer club set and BALANCED where the router says Normal. These
 * adapters fold those inputs onto the router's five categories / three
 * strategies so the game can call the router without knowing its vocabulary.
 */

const GAME_CLUB_TO_CATEGORY: Readonly<Record<string, ClubCategory>> = {
  DRIVER: "Driver",
  FAIRWAY_WOOD: "Woods",
  HYBRID: "Woods",
  IRON: "Irons",
  WEDGE: "Wedges",
  PUTTER: "Putter",
};

const GAME_STRATEGY_TO_PLAYER: Readonly<Record<string, PlayerStrategy>> = {
  BALANCED: "Normal",
  AGGRESSIVE: "Aggressive",
  CONSERVATIVE: "Conservative",
};

/** Map a game club enum (e.g. FAIRWAY_WOOD) onto a router category. */
export function toClubCategory(gameClub: string): ClubCategory {
  return GAME_CLUB_TO_CATEGORY[gameClub] ?? "Irons";
}

/** Map a game strategy enum (e.g. BALANCED) onto a router strategy. */
export function toPlayerStrategy(gameStrategy: string): PlayerStrategy {
  return GAME_STRATEGY_TO_PLAYER[gameStrategy] ?? "Normal";
}

/**
 * Resolve a physics profile straight from the game's raw shot inputs — the
 * enum names the play UI already submits alongside a target distance.
 */
export function shotRouterFromGameInputs(
  gameClub: string,
  targetDistance: number,
  gameStrategy: string,
): ShotPhysicsProfile {
  return shotRouter(toClubCategory(gameClub), targetDistance, toPlayerStrategy(gameStrategy));
}

/**
 * Guess the club a carry distance (yards) implies. Used when a shot was simmed
 * rather than played, so the router still has a club to work from — the player
 * never chose one, so we read it back from how far the ball travelled.
 */
export function inferClubFromCarry(carryYards: number): ClubCategory {
  if (carryYards >= 230) return "Driver";
  if (carryYards >= 190) return "Woods";
  if (carryYards >= 55) return "Irons";
  if (carryYards >= 15) return "Wedges";
  return "Putter";
}

/**
 * Resolve a physics profile for a shot with no chosen club (a simmed shot),
 * inferring the club from the carry and playing it as a normal-strategy swing.
 */
export function shotRouterFromCarry(carryYards: number): ShotPhysicsProfile {
  return shotRouter(inferClubFromCarry(carryYards), carryYards, "Normal");
}
