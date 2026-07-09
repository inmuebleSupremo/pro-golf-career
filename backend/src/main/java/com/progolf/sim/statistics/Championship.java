package com.progolf.sim.statistics;

import java.util.Objects;

/**
 * A recorded tournament victory in the Historical Archive (spec: records-archive, REQ-256): the season, the
 * tournament name, its tier, and the winning golfer. Preserved permanently; always corresponds to a real
 * result. Immutable.
 */
public record Championship(int season, String tournamentName, String tier, String winnerId) {

    public Championship {
        Objects.requireNonNull(tournamentName, "tournamentName");
        Objects.requireNonNull(tier, "tier");
        Objects.requireNonNull(winnerId, "winnerId");
    }
}
