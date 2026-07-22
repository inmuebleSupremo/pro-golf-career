package com.progolf.sim.staff;

/**
 * The aggregated, role-consistent influence of a Support Team (spec: staff-influence, REQ-197), as pure
 * read-only factors the World applies through the owning domains — never by modifying results directly.
 * {@code developmentBonus} scales seasonal Development Points (coach); {@code recoveryBonus} is extra
 * weekly fatigue recovery (fitness coach + physiotherapist); {@code conditioningBonus} lifts the golfer's
 * long-term fitness target each season (fitness coach). {@code mentalSupport} and {@code strategicSupport}
 * are exposed for shot-context application (psychologist / caddie).
 */
public record StaffEffects(double developmentBonus, double recoveryBonus, double mentalSupport,
                           double strategicSupport, double conditioningBonus) {

    public static final StaffEffects NONE = new StaffEffects(0, 0, 0, 0, 0);
}
