package com.progolf.sim.shot;

import com.progolf.sim.course.Position2d;
import com.progolf.sim.spatial.Surface;
import java.util.Objects;

/** The legal, playable origin of the next shot. */
public record BallState(Position2d position, Surface lie) {
    public BallState {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(lie, "lie");
        if (!lie.isPlayable()) {
            throw new IllegalArgumentException("ball state must be playable: " + lie);
        }
    }
}
