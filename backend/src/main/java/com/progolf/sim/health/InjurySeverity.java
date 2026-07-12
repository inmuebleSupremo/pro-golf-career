package com.progolf.sim.health;

/**
 * The severity of an {@link Injury} (spec: injury-recovery): the weeks of rehabilitation it requires,
 * whether it is significant enough to enter health history, and the shot impairment a golfer suffers if
 * they play through it while recovering (spec: injury-recovery play-through).
 */
public enum InjurySeverity {
    MINOR(2, false, 0.05),
    MODERATE(5, true, 0.15),
    SEVERE(12, true, 0.30);

    private final int rehabWeeks;
    private final boolean significant;
    private final double impairment;

    InjurySeverity(int rehabWeeks, boolean significant, double impairment) {
        this.rehabWeeks = rehabWeeks;
        this.significant = significant;
        this.impairment = impairment;
    }

    /** Weeks of rehabilitation a fresh injury of this severity requires. */
    public int rehabWeeks() {
        return rehabWeeks;
    }

    /** Whether an injury of this severity is preserved in health history (REQ-219). */
    public boolean significant() {
        return significant;
    }

    /** The shot impairment ([0,1]) suffered when playing through an injury of this severity while recovering. */
    public double impairment() {
        return impairment;
    }
}
