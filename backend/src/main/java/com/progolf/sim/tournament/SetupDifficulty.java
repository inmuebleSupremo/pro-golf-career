package com.progolf.sim.tournament;

import com.progolf.sim.course.CourseSetup;

/**
 * Maps an event's tour tier and prestige to a {@link CourseSetup} (spec: course-setup / event-prestige).
 * The tier component normalizes for field strength — a weaker-field tour is set up easier (wide, calm,
 * centre pins) and a stronger-field tour harder — so every tier's Regular events play near even par; the
 * prestige component adds difficulty on top (Signature harder than Regular, Majors hardest).
 *
 * <p>A single {@code difficulty} in [0,1] per event class (tier base + prestige bump) is mapped linearly to
 * each of the three setup factors between an easy end (difficulty 0) and a hard end (difficulty 1).
 * Magnitudes are calibrated (throwaway per-tier/prestige diagnostic over the real tour fractions and
 * field sizes) to the scoring targets: each tier's Regular events ~even par, Signature ~+1, Majors ~+3.
 * Measured calm field means at this calibration (weather adds on top): Regular ~−0.4..+0.3 across all tiers
 * (tightly normalized), Signature ~+0.7..+1.2, Major ~+2.1..+2.7 (an Elite-strength major field ~+2.1 calm,
 * lifting into the +2.5..+4 target under typical weather). Most of the difficulty lives in the
 * weather-independent width/pin levers so these hold regardless of wind.
 */
public final class SetupDifficulty {

    private SetupDifficulty() {
    }

    // Tier base difficulty (ascends with field strength; a stronger field needs a harder setup to score ~E).
    private static final double TIER_DEVELOPMENT = 0.05;
    private static final double TIER_STANDARD = 0.34;
    private static final double TIER_PREMIER = 0.54;
    private static final double TIER_ELITE = 0.67;

    // Prestige bump added on top of the tier base.
    private static final double PRESTIGE_REGULAR = 0.00;
    private static final double PRESTIGE_SIGNATURE = 0.14;
    private static final double PRESTIGE_MAJOR = 0.36;

    // Each factor is interpolated from its easy end (difficulty 0) to its hard end (difficulty 1). Most of the
    // difficulty lives in the weather-independent width/pin levers; wind is a modest amplifier so windy days
    // play harder without runaway volatility.
    private static final double WIDTH_EASY = 1.50, WIDTH_HARD = 0.45;   // effective green/fairway width
    private static final double PIN_EASY = 0.45, PIN_HARD = 2.60;       // pin tuck/depth aggression
    private static final double WIND_EASY = 0.70, WIND_HARD = 1.50;     // wind/exposure scale

    /** The course setup for an event of the given tour tier and prestige. */
    public static CourseSetup forEvent(Tier tier, EventPrestige prestige) {
        double difficulty = clamp01(tierBase(tier) + prestigeBump(prestige));
        double widthScale = lerp(WIDTH_EASY, WIDTH_HARD, difficulty);
        double pinAggression = lerp(PIN_EASY, PIN_HARD, difficulty);
        double windScale = lerp(WIND_EASY, WIND_HARD, difficulty);
        return new CourseSetup(pinAggression, windScale, widthScale);
    }

    private static double tierBase(Tier tier) {
        return switch (tier) {
            case DEVELOPMENT -> TIER_DEVELOPMENT;
            case STANDARD -> TIER_STANDARD;
            case PREMIER -> TIER_PREMIER;
            case ELITE -> TIER_ELITE;
        };
    }

    private static double prestigeBump(EventPrestige prestige) {
        return switch (prestige) {
            case REGULAR -> PRESTIGE_REGULAR;
            case SIGNATURE -> PRESTIGE_SIGNATURE;
            case MAJOR -> PRESTIGE_MAJOR;
        };
    }

    private static double lerp(double easy, double hard, double difficulty) {
        return easy + (hard - easy) * difficulty;
    }

    private static double clamp01(double v) {
        return v < 0.0 ? 0.0 : Math.min(v, 1.0);
    }
}
