package com.progolf.sim.shot;

/**
 * The single configuration surface for every realism constant used by shot resolution (task 7.5).
 *
 * <p>These values define the CURRENT calibration and are expected to be tuned in a dedicated pass
 * against target scoring distributions. They live here — and only here — so tuning never requires
 * touching resolution logic. All dispersion values are in yards.
 */
public final class SimConstants {

    private SimConstants() {
    }

    // --- Base dispersion (scales with intended shot length) ---
    // Dispersion = FRACTION * shotDistance + FLOOR, before attribute/condition modifiers. This governs the
    // full ball-flight shots (tee to green); putts bypass it entirely (see the putting model below). The
    // fractions are calibrated so greens-in-regulation, fairways, and scoring land on realistic targets.
    /** Lateral dispersion as a fraction of intended shot distance. */
    public static final double LATERAL_DISPERSION_FRACTION = 0.094;
    /** Minimum lateral dispersion (yards) regardless of shot length — keeps short pitches/chips accurate. */
    public static final double LATERAL_DISPERSION_FLOOR = 0.10;
    /** Longitudinal dispersion as a fraction of intended shot distance. */
    public static final double DISTANCE_DISPERSION_FRACTION = 0.058;
    /** Minimum longitudinal dispersion (yards) regardless of shot length — keeps short pitches/chips accurate. */
    public static final double DISTANCE_DISPERSION_FLOOR = 0.20;

    // --- Attribute influence ---
    /** Attribute normalised value maps to a factor in [MIN_FACTOR, MIN_FACTOR + FACTOR_SPAN]. */
    public static final double MIN_ATTRIBUTE_FACTOR = 0.5;
    public static final double ATTRIBUTE_FACTOR_SPAN = 1.0;
    /** Fraction of club base distance reachable at attribute 0 vs. the span added by distance skill. */
    public static final double REACH_FLOOR = 0.80;
    public static final double REACH_SPAN = 0.40;
    /** Wind resistance from distance skill: floor and span (higher distance skill resists wind more). */
    public static final double WIND_RESIST_FLOOR = 0.30;
    public static final double WIND_RESIST_SPAN = 0.50;

    // --- Condition penalties (sigma multipliers / mean adjustments) ---
    public static final double HEADWIND_MEAN_WEIGHT = 0.70;
    public static final double TAILWIND_MEAN_WEIGHT = 0.50;
    public static final double CROSSWIND_SIGMA_WEIGHT = 0.15;
    public static final double PRESSURE_SIGMA_WEIGHT = 0.50;
    public static final double FATIGUE_SIGMA_WEIGHT = 0.60;
    public static final double FATIGUE_MEAN_WEIGHT = 0.10;
    public static final double LIE_SIGMA_WEIGHT = 0.80;

    // --- Rare extremes (mixture tail) ---
    /** Base probability of a mishit before Course Management reduces it. */
    public static final double BASE_MISHIT_PROBABILITY = 0.030;
    /** Fraction of mishit probability removed at maximum Course Management. */
    public static final double MISHIT_MANAGEMENT_RELIEF = 0.50;
    /** Probability of an exceptional recovery / hero shot. */
    public static final double HERO_PROBABILITY = 0.020;
    /** Error inflation applied on a mishit. */
    public static final double MISHIT_ERROR_MULTIPLIER = 4.0;
    /** Fraction of intended carry lost on a mishit. */
    public static final double MISHIT_SHORTFALL_FRACTION = 0.15;
    /** Error reduction applied on a hero shot. */
    public static final double HERO_ERROR_MULTIPLIER = 0.30;

    // --- Safety net ---
    /** Lateral magnitude beyond which outcomes are compressed. */
    public static final double SAFETY_LATERAL_CAP = 45.0;
    /** Distance-error magnitude beyond which outcomes are compressed. */
    public static final double SAFETY_DISTANCE_CAP = 60.0;
    /** Compression applied to the portion of an error beyond the cap (keeps poor shots, bounds catastrophe). */
    public static final double SAFETY_COMPRESSION = 0.15;
    /** Hard ceiling as a multiple of the cap. */
    public static final double SAFETY_HARD_MULTIPLE = 2.0;

    // --- Situational strategy (spec: shot-resolution pin-attacking) ---
    // On a scoring approach a golfer aims a fraction of the way to a tucked pin, scaled by the shot's
    // confidence: a short wedge can hunt the flag, a long iron plays the safe green centre. The fraction
    // fades linearly from full at NEAR to zero at FAR (yards of remaining distance).
    /** At or under this remaining distance, an approach is confident enough to fully commit to a pin attack. */
    public static final double PIN_ATTACK_FADE_NEAR = 120.0;
    /** At or beyond this remaining distance, no pin is attacked — the golfer plays the green centre. */
    public static final double PIN_ATTACK_FADE_FAR = 190.0;
    /** Recovery-difficulty of the lie at or above which a golfer plays conservatively regardless of disposition. */
    public static final double RECOVERY_CAUTION_THRESHOLD = 0.4;

    // Lay-up vs go-for-it: on a long approach (not a tee shot) the golfer decides whether to attack a
    // reachable green or lay up to a full-wedge distance. An aggressive disposition or a comfortably
    // reachable green goes for it (birdie/eagle chance, hazard risk); otherwise it lays up (safe).
    /** Remaining distance (yards) at or beyond which a long approach becomes a lay-up-or-go decision. */
    public static final double LAYUP_MIN_DISTANCE = 215.0;
    /** The distance (yards) a lay-up leaves for the following shot — a comfortable full wedge. */
    public static final double LAYUP_LEAVE_DISTANCE = 95.0;
    /** Margin (yards) by which reach must exceed the distance for the green to count as comfortably reachable. */
    public static final double LAYUP_COMFORTABLE_MARGIN = 10.0;

    // --- Round resolution ---
    /** Distance (yards) at or under which the ball is considered holed — roughly a one-foot tap-in. */
    public static final double HOLED_THRESHOLD = 0.35;
    /** Maximum shots resolved for a single hole (guards against pathological loops). */
    public static final int MAX_SHOTS_PER_HOLE = 12;

    // --- Putting model (spec: shot-resolution putting) ---
    // A putt (a shot played from the green) is resolved by an explicit make-probability model rather than
    // the full ball-flight geometry: the ball rolls along the green, sheltered from wind, and either drops
    // or finishes a short, proximity-controlled distance away. This is what lets short putts hole out
    // near-certainly and keeps putts-per-round realistic (~29-32). A full-flight model with a fixed
    // dispersion floor could never reliably hole out from tap-in range.
    /** Yards-to-feet conversion (make probability is expressed in feet, the natural putting unit). */
    public static final double YARDS_TO_FEET = 3.0;
    /** Distance (feet) at which a neutral (0-skill) putter makes 50% — raised by putting accuracy. */
    public static final double PUTT_MAKE_F50_BASE = 5.0;
    /** Additional 50%-make distance (feet) contributed at maximum putting accuracy. */
    public static final double PUTT_MAKE_F50_SPAN = 6.0;
    /** Steepness of the make-probability fall-off with distance (higher = sharper cliff past f50). */
    public static final double PUTT_MAKE_SHARPNESS = 2.6;
    /** Ceiling on make probability so even a tap-in can (very rarely) miss. */
    public static final double PUTT_MAKE_CAP = 0.999;
    /** Fraction of make probability removed at maximum fatigue. */
    public static final double PUTT_FATIGUE_PENALTY = 0.15;
    /** Fraction of make probability removed at maximum uncomposed pressure. */
    public static final double PUTT_PRESSURE_PENALTY = 0.20;
    /** Minimum leave (yards) after a missed putt — above {@link #HOLED_THRESHOLD} so a miss is never
     * mistaken for a hole-out and always leaves a distinct (near-certain) tap-in. */
    public static final double PUTT_LEAVE_FLOOR = 0.15;
    /** Leave as a fraction of the putt distance, before proximity relief. */
    public static final double PUTT_LEAVE_FRACTION = 0.06;
    /** Fraction of the distance-scaled leave removed at maximum putting proximity. */
    public static final double PUTT_LEAVE_PROX_RELIEF = 0.5;
    /** Relative spread of the (gaussian) leave around its mean. */
    public static final double PUTT_LEAVE_SIGMA = 0.5;
    /** Hard minimum leave (yards) so a missed putt always leaves a real tap-in (> HOLED_THRESHOLD). */
    public static final double PUTT_LEAVE_MIN = 0.40;
    /** Leave is capped at this fraction of the putt distance so a missed putt always converges nearer. */
    public static final double PUTT_LEAVE_CONVERGE = 0.55;
}
