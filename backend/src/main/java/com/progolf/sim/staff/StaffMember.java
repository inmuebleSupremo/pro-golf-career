package com.progolf.sim.staff;

import java.util.Objects;

/**
 * A single professional staff member (spec: support-team, REQ-191/192): their role, name, quality in
 * [0,1], one-off hiring cost, and ongoing seasonal salary. An independent entity belonging to one golfer's
 * Support Team (REQ-200). Immutable.
 */
public record StaffMember(StaffRole role, String name, double quality, double hiringCost, double seasonalSalary) {

    public StaffMember {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(name, "name");
        if (!Double.isFinite(quality) || quality < 0 || quality > 1) {
            throw new IllegalArgumentException("quality must be in [0,1]: " + quality);
        }
        if (hiringCost < 0 || seasonalSalary < 0) {
            throw new IllegalArgumentException("costs must be >= 0");
        }
    }
}
