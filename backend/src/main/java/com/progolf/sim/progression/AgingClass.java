package com.progolf.sim.progression;

import com.progolf.sim.core.Attribute;

/**
 * How an attribute ages (REQ-159). Each class has a peak age and pre-/post-peak slopes so aging is
 * attribute-specific and never uniform: raw power fades from around 30, struck-shot technique sharpens into
 * the mid-40s, and judgment accrues into the 50s. Only driving distance declines materially inside a normal
 * career — a golfer in their 40s is a better player than they were at 30, just a shorter one.
 *
 * <p>Aging is an <em>overlay</em>, not the engine of a career: growing into your ability is development's
 * job (it walks a golfer toward their {@link com.progolf.sim.player.Player#potential()}), so the pre-peak
 * slopes here are deliberately gentle. If they matched development's pace the two would double-count and a
 * golfer would improve for reasons they never chose — which is the opposite of the point.
 */
public enum AgingClass {
    /** Driving distance: raw power. The one athletic gift that fades — peaks around 30, then eases off. */
    PHYSICAL(30, 0.15, -0.35),
    /**
     * Struck-shot skills — driving accuracy, irons, wedges, putting. Technique keeps sharpening well past
     * the physical peak and holds deep into a career; it is practised, not inherited.
     */
    SKILL(45, 0.12, -0.15),
    /** Composure, course management: judgment accrues with experience and barely fades at all. */
    MENTAL(55, 0.15, -0.1);

    private final int peakAge;
    private final double prePeakSlope;   // per-year rise before the peak
    private final double postPeakSlope;  // per-year change after the peak (negative)

    AgingClass(int peakAge, double prePeakSlope, double postPeakSlope) {
        this.peakAge = peakAge;
        this.prePeakSlope = prePeakSlope;
        this.postPeakSlope = postPeakSlope;
    }

    public int peakAge() {
        return peakAge;
    }

    /**
     * The cumulative attribute offset at a given age relative to the reference age: a rising line to the
     * peak, then a declining line after. Per-season change is the difference of rounded offsets, so aging
     * is gradual and integer while summing correctly over a career.
     */
    public double offsetAt(int age) {
        int ref = ProgressionConstants.AGING_REFERENCE_AGE;
        if (age <= peakAge) {
            return (age - ref) * prePeakSlope;
        }
        return (peakAge - ref) * prePeakSlope + (age - peakAge) * postPeakSlope;
    }

    /** The aging class of an attribute. */
    public static AgingClass of(Attribute attribute) {
        return switch (attribute) {
            case DRIVING_DISTANCE -> PHYSICAL;
            // Driving accuracy is a struck-shot skill, not an athletic gift: finding the fairway is technique
            // and club selection, which is why it is the one driving stat that survives a golfer's 30s.
            case DRIVING_ACCURACY, IRONS_ACCURACY, IRONS_CONTROL, WEDGES, PUTTING_ACCURACY, PUTTING_PROXIMITY -> SKILL;
            case COMPOSURE, COURSE_MANAGEMENT -> MENTAL;
        };
    }
}
