package com.progolf.sim.staff;

import java.util.Objects;

/**
 * A single professional staff member (spec: support-team, REQ-191/192): their role, name, age, nationality
 * (a country code, kept as a String so the staff domain stays core-only), personality, quality in [0,1],
 * one-off hiring cost, and ongoing seasonal salary. An independent entity belonging to one golfer's Support
 * Team (REQ-200). Age/nationality/personality are descriptive; only role and quality affect influence.
 * Immutable.
 */
public record StaffMember(StaffRole role, String name, int age, String nationality,
                          StaffPersonality personality, double quality, double hiringCost, double seasonalSalary) {

    public StaffMember {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(nationality, "nationality");
        Objects.requireNonNull(personality, "personality");
        if (age < 0) {
            throw new IllegalArgumentException("age must be >= 0: " + age);
        }
        if (!Double.isFinite(quality) || quality < 0 || quality > 1) {
            throw new IllegalArgumentException("quality must be in [0,1]: " + quality);
        }
        if (hiringCost < 0 || seasonalSalary < 0) {
            throw new IllegalArgumentException("costs must be >= 0");
        }
    }
}
