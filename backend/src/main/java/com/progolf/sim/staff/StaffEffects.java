package com.progolf.sim.staff;

/**
 * The aggregated, role-consistent influence of a Support Team (spec: staff-influence, REQ-197), as pure
 * read-only factors the World applies through the owning domains — never by modifying results directly.
 * {@code developmentBonus} scales seasonal Development Points (coach); {@code recoveryBonus} is extra
 * weekly fatigue recovery (fitness coach + physiotherapist). {@code mentalSupport} and
 * {@code strategicSupport} are exposed for future shot-context application (psychologist / caddie).
 */
public record StaffEffects(double developmentBonus, double recoveryBonus, double mentalSupport, double strategicSupport) {

    public static final StaffEffects NONE = new StaffEffects(0, 0, 0, 0);
}
