package com.progolf.sim.shot;

import java.util.Objects;

/** The complete deliberate decision for a non-putting ball strike. */
public record BallStrikeIntent(ClubId club, AimPoint aimPoint, ShotFamily shotFamily, ShotShape shotShape) implements ShotIntent {
    public BallStrikeIntent {
        Objects.requireNonNull(club, "club");
        Objects.requireNonNull(aimPoint, "aimPoint");
        Objects.requireNonNull(shotFamily, "shotFamily");
        Objects.requireNonNull(shotShape, "shotShape");
    }

    /** API/fixture compatibility; production callers should state technique explicitly. */
    public BallStrikeIntent(ClubId club, AimPoint aimPoint) {
        this(club, aimPoint, ShotFamily.FULL, ShotShape.STRAIGHT);
    }
    public BallStrikeIntent(ClubId club, AimPoint aimPoint, ShotFamily shotFamily) { this(club, aimPoint, shotFamily, ShotShape.STRAIGHT); }
}
