package com.progolf.sim.health;

/**
 * The severity of an {@link Injury} (spec: injury-recovery, REQ-219/220): the weeks of rehabilitation it
 * requires and whether it is significant enough to enter health history.
 */
public enum InjurySeverity {
    MINOR(2, false),
    MODERATE(5, true),
    SEVERE(12, true);

    private final int rehabWeeks;
    private final boolean significant;

    InjurySeverity(int rehabWeeks, boolean significant) {
        this.rehabWeeks = rehabWeeks;
        this.significant = significant;
    }

    /** Weeks of rehabilitation a fresh injury of this severity requires. */
    public int rehabWeeks() {
        return rehabWeeks;
    }

    /** Whether an injury of this severity is preserved in health history (REQ-219). */
    public boolean significant() {
        return significant;
    }
}
