package com.progolf.sim.staff;

import java.util.List;
import java.util.Optional;

/**
 * The deterministic policy by which a golfer decides staff changes (spec: staff-influence, REQ-195/199).
 * It targets a team size that grows with career stage and fills roles in a fixed priority order, at most
 * one hire per season (continuity). Affordability is checked separately by the caller against the
 * candidate's costs. The same rules apply to every golfer, so decisions are reproducible.
 */
public final class HiringPolicy {

    /** The order in which roles are filled: development first, then recovery, then strategic/mental. */
    static final List<StaffRole> PRIORITY = List.of(
            StaffRole.COACH,
            StaffRole.PHYSIOTHERAPIST,
            StaffRole.FITNESS_COACH,
            StaffRole.CADDIE,
            StaffRole.SPORTS_PSYCHOLOGIST);

    private HiringPolicy() {
    }

    /** The next role to try to hire this season, or empty if the team is already at its stage target. */
    public static Optional<StaffRole> chooseRole(SupportTeam team, int age) {
        if (team.size() >= targetTeamSize(age)) {
            return Optional.empty();
        }
        for (StaffRole role : PRIORITY) {
            if (!team.has(role)) {
                return Optional.of(role);
            }
        }
        return Optional.empty();
    }

    /** Whether the golfer can afford to hire and keep the candidate (hiring cost plus a season of salary). */
    public static boolean canAfford(double availableFunds, StaffMember candidate) {
        return availableFunds >= candidate.hiringCost() + candidate.seasonalSalary();
    }

    /** The target team size for a golfer's career stage (grows into the prime, holds after). */
    public static int targetTeamSize(int age) {
        if (age <= StaffConstants.DEVELOPMENT_STAGE_MAX_AGE) {
            return StaffConstants.TARGET_TEAM_DEVELOPMENT;
        }
        if (age <= StaffConstants.PRIME_STAGE_MAX_AGE) {
            return StaffConstants.TARGET_TEAM_PRIME;
        }
        return StaffConstants.TARGET_TEAM_LATE;
    }
}
