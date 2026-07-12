package com.progolf.sim.course;

/**
 * The difficulty setup applied to a course as a specific event plays it (spec: course-setup). Three
 * factors scale how hard the field's shots play, without changing the shot-resolution math or the course's
 * generated geometry:
 *
 * <ul>
 *   <li>{@code pinAggression} — how tucked (laterally) and deep the flags are cut; 1.0 = baseline, higher =
 *       more tucked/testing.</li>
 *   <li>{@code windScale} — multiplier on the event's wind exposure; 1.0 = baseline, higher = windier.</li>
 *   <li>{@code widthScale} — multiplier on the effective green and fairway playing width; 1.0 = baseline,
 *       higher = wider/easier, lower = tighter/harder.</li>
 * </ul>
 *
 * <p>A {@link #standard()} setup (all factors 1.0) reproduces the course's baseline behaviour exactly, so
 * any resolution not given an event setup is unchanged. The tier/prestige mapping that produces a setup lives
 * with the tournament domain ({@code SetupDifficulty}); this record is a pure presentation-scaling descriptor.
 */
public record CourseSetup(double pinAggression, double windScale, double widthScale) {

    private static final CourseSetup STANDARD = new CourseSetup(1.0, 1.0, 1.0);

    public CourseSetup {
        require(pinAggression, "pinAggression");
        require(windScale, "windScale");
        require(widthScale, "widthScale");
    }

    /** The neutral setup: every factor 1.0, reproducing the course's baseline difficulty. */
    public static CourseSetup standard() {
        return STANDARD;
    }

    private static void require(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be finite and positive: " + value);
        }
    }
}
