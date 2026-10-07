package com.progolf.sim.shot;

import java.util.Objects;

/** The complete deliberate decision for a non-putting ball strike. */
public record BallStrikeIntent(ClubId club, AimPoint aimPoint) implements ShotIntent {
    public BallStrikeIntent {
        Objects.requireNonNull(club, "club");
        Objects.requireNonNull(aimPoint, "aimPoint");
    }
}
