package com.progolf.sim.world;

import com.progolf.sim.tour.TourTier;
import com.progolf.sim.tournament.EventPrestige;
import java.util.Objects;

/**
 * One event on the player's reviewable schedule (spec: player-control): an upcoming event the player is
 * eligible for, with its week, tour tier, and {@link EventPrestige} (the reward side of the trade-off) and
 * whether the player is currently entered ({@code entered} = not resting and not skipped — the player's
 * choice, independent of transient health). Immutable.
 */
public record PlayerScheduleEntry(long tournamentId, int week, TourTier tier, EventPrestige prestige,
                                  boolean entered, String name) {

    public PlayerScheduleEntry {
        Objects.requireNonNull(tier, "tier");
        Objects.requireNonNull(prestige, "prestige");
        Objects.requireNonNull(name, "name");
    }
}
