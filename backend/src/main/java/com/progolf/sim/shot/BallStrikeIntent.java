package com.progolf.sim.shot;

import java.util.Objects;

/** The complete deliberate decision for a non-putting ball strike. */
public record BallStrikeIntent(ClubId club, AimPoint aimPoint, ShotFamily shotFamily) implements ShotIntent {
    public BallStrikeIntent {
        Objects.requireNonNull(club, "club");
        Objects.requireNonNull(aimPoint, "aimPoint");
        Objects.requireNonNull(shotFamily, "shotFamily");
    }

    /** API/fixture compatibility; production callers should state technique explicitly. */
    public BallStrikeIntent(ClubId club, AimPoint aimPoint) {
        this(club, aimPoint, ShotFamily.FULL);
    }
}
