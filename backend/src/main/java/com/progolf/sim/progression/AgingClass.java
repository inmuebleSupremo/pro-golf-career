package com.progolf.sim.progression;

import com.progolf.sim.core.Attribute;

/**
 * How an attribute ages (REQ-159). Each class has a peak age and pre-/post-peak slopes so aging is
 * attribute-specific and never uniform: physical gifts fade early, skills hold, judgment keeps rising.
 */
public enum AgingClass {
    /** Driving distance/accuracy: peak in the late 20s, then decline. */
    PHYSICAL(27, 0.6, -0.9),
    /** Irons, wedges, putting: peak in the early-mid 30s, hold longer. */
    SKILL(33, 0.5, -0.4),
    /** Composure, course management: keep rising into the 40s, ease off gently. */
    MENTAL(44, 0.5, -0.2);

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
            case DRIVING_DISTANCE, DRIVING_ACCURACY -> PHYSICAL;
            case IRONS_ACCURACY, IRONS_CONTROL, WEDGES, PUTTING_ACCURACY, PUTTING_PROXIMITY -> SKILL;
            case COMPOSURE, COURSE_MANAGEMENT -> MENTAL;
        };
    }
}
