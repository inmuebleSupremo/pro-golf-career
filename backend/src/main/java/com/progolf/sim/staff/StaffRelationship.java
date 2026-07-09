package com.progolf.sim.staff;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * A recorded professional relationship in a golfer's staff history (spec: staff-relationships,
 * REQ-193/201): the role, the staff member's name and quality, the season it began, and the season it
 * ended (empty while ongoing). Immutable; departures never remove history, they close the relationship.
 */
public record StaffRelationship(StaffRole role, String staffName, double quality, int startSeason, OptionalInt endSeason) {

    public StaffRelationship {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(staffName, "staffName");
        Objects.requireNonNull(endSeason, "endSeason");
    }

    /** Opens a new, ongoing relationship. */
    public static StaffRelationship opened(StaffMember member, int season) {
        return new StaffRelationship(member.role(), member.name(), member.quality(), season, OptionalInt.empty());
    }

    /** Whether the relationship is still ongoing. */
    public boolean isActive() {
        return endSeason.isEmpty();
    }

    /** The same relationship closed at {@code season}. */
    public StaffRelationship closedAt(int season) {
        return new StaffRelationship(role, staffName, quality, startSeason, OptionalInt.of(season));
    }

    /** Seasons spanned as of {@code asOfSeason} for an ongoing relationship, or its full closed span. */
    public int seasonsSpanned(int asOfSeason) {
        int end = endSeason.orElse(asOfSeason);
        return Math.max(1, end - startSeason + 1);
    }
}
