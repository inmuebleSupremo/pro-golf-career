package com.progolf.sim.staff;

/**
 * The V1 support-staff roles (spec: support-team, REQ-192/194), each with a clearly defined responsibility
 * and a base seasonal salary. Future versions may add roles without changing this set.
 */
public enum StaffRole {
    COACH("Long-term player development", 120_000.0),
    CADDIE("Strategic support", 80_000.0),
    FITNESS_COACH("Physical preparation", 70_000.0),
    PHYSIOTHERAPIST("Recovery support", 70_000.0),
    SPORTS_PSYCHOLOGIST("Mental preparation", 60_000.0);

    private final String responsibility;
    private final double baseSalary;

    StaffRole(String responsibility, double baseSalary) {
        this.responsibility = responsibility;
        this.baseSalary = baseSalary;
    }

    /** The role's defined responsibility (REQ-194). */
    public String responsibility() {
        return responsibility;
    }

    /** The base seasonal salary for the role, before quality scaling. */
    public double baseSalary() {
        return baseSalary;
    }
}
