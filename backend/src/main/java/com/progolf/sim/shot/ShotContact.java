package com.progolf.sim.shot;

import com.progolf.sim.course.Position2d;
import com.progolf.sim.spatial.Surface;
import java.util.Objects;

/** The sampled physical contact of a shot, including penalty surfaces. */
public record ShotContact(Position2d position, Surface surface) {
    public ShotContact {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(surface, "surface");
    }
}
