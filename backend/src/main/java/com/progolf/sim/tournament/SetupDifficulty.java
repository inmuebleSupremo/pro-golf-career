package com.progolf.sim.tournament;

import com.progolf.sim.course.CourseSetup;

/**
 * Maps an event's tour tier and prestige to a {@link CourseSetup} (spec: course-setup / event-prestige).
 * A stronger-field tour plays a mildly tougher course, and prestige adds difficulty on top (Signature harder
 * than Regular, Majors hardest).
 *
 * <p>A single {@code difficulty} in [0,1] per event class (tier base + prestige bump) is mapped linearly to
 * each of the three setup factors between an easy end (difficulty 0) and a hard end (difficulty 1).
 *
 * <p>The tier component deliberately does NOT normalize scores across tours. Calibrated (throwaway
 * difficulty-grid diagnostic, {@code SetupCalibrationHarnessTest}) so each tour's typical field averages
 * near even par given the golfers who actually play it, which lands winners around −15 to −20 for four
 * rounds on every tour. Scores differ between tours because the fields do.
 *
 * <p><b>This file is coupled to the shot engine's calibration.</b> It maps difficulty to geometry, and what
 * that geometry is worth in strokes depends on {@code SimConstants.ATTRIBUTE_FACTOR_K} and the dispersion
 * fractions. It is not self-correcting: when the engine's skill scaling changed and this did not, the entry
 * tour's winners went to −40. Re-run the difficulty grid after any shot-engine or population-ability change.
 */
public final class SetupDifficulty {

    private SetupDifficulty() {
    }

    // Tier base difficulty. Ascends with field strength — a stronger tour plays a tougher course, as it does
    // in life — but only mildly. These sit in a narrow band on purpose: what separates a tour from the one
    // below it is the FIELD, not the golf course.
    //
    // They were previously spread 0.05-0.67, which flattened the entry tour to a 1.45x-wide, centre-pinned
    // course in pursuit of an "every tier scores ~even par" target. That target is a mistake twice over. It
    // asks the weakest tour to play the easiest course, which removes the very difficulty that separates a
    // good golfer from a poor one — and it breaks down completely the moment a strong golfer is present on a
    // weak tour, which is exactly what an entry tour full of future stars is: a 90-rated golfer on a course
    // set up for 60-rated golfers shot -40 for four rounds. A tour's scores should follow from who is playing
    // it: the entry tour's winner shoots -15 because the field is weak, the elite tour's -20 because they are
    // brilliant.
    private static final double TIER_DEVELOPMENT = 0.50;
    private static final double TIER_STANDARD = 0.58;
    private static final double TIER_PREMIER = 0.66;
    private static final double TIER_ELITE = 0.74;

    // Prestige bump added on top of the tier base. Sized so the hardest combination (an Elite major) lands
    // just under the maximum rather than past it: bumps that clamp make a major and a tour championship on
    // the top tours play identically, and silently flatten the prestige ladder they exist to create.
    private static final double PRESTIGE_REGULAR = 0.00;
    private static final double PRESTIGE_SIGNATURE = 0.09;
    private static final double PRESTIGE_TOUR_CHAMPIONSHIP = 0.16;
    private static final double PRESTIGE_MAJOR = 0.24;

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
            case TOUR_CHAMPIONSHIP -> PRESTIGE_TOUR_CHAMPIONSHIP;
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
