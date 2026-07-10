package com.progolf.sim.statistics;

import java.util.Objects;

/**
 * A recorded tournament victory in the Historical Archive (spec: records-archive, REQ-256): the season, the
 * tournament name, its tier, its event prestige, and the winning golfer. Preserved permanently; always
 * corresponds to a real result. Immutable.
 *
 * <p>Prestige (like tier) is stored as an opaque label so the archive depends only on core — the World
 * feeds {@code EventPrestige.name()}; {@link #MAJOR_PRESTIGE} mirrors that enum's major constant.
 */
public record Championship(int season, String tournamentName, String tier, String prestige, String winnerId) {

    /** The prestige label of a major, mirroring {@code EventPrestige.MAJOR.name()}. */
    public static final String MAJOR_PRESTIGE = "MAJOR";

    public Championship {
        Objects.requireNonNull(tournamentName, "tournamentName");
        Objects.requireNonNull(tier, "tier");
        Objects.requireNonNull(prestige, "prestige");
        Objects.requireNonNull(winnerId, "winnerId");
    }

    /** Whether this championship was a major — the marquee accomplishment (spec: event-prestige). */
    public boolean isMajor() {
        return MAJOR_PRESTIGE.equals(prestige);
    }
}
