package com.progolf.sim.staff;

import com.progolf.sim.core.Rng;

/**
 * Generates candidate staff members (spec: staff-influence, REQ-195). A candidate's quality is seeded and
 * its costs scale with quality (better staff cost more), so hiring is an affordability trade-off.
 * Stateless and deterministic — every draw comes from the supplied {@link Rng}.
 */
public final class StaffMarket {

    /** A candidate for the given role. */
    public StaffMember generate(StaffRole role, Rng rng) {
        double quality = clamp(
                StaffConstants.QUALITY_MEAN + rng.nextGaussian() * StaffConstants.QUALITY_SPREAD,
                StaffConstants.QUALITY_MIN, StaffConstants.QUALITY_MAX);
        double salary = role.baseSalary()
                * (StaffConstants.SALARY_QUALITY_FLOOR + quality * StaffConstants.SALARY_QUALITY_SPAN);
        double hiringCost = salary * StaffConstants.HIRING_COST_FRACTION;
        String name = role.name() + "-" + Integer.toString((int) (rng.nextDouble() * 100_000));
        return new StaffMember(role, name, quality, hiringCost, salary);
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : Math.min(v, hi);
    }
}
