package com.progolf.sim.putt;

import com.progolf.sim.course.Position2d;
import java.util.Objects;

/**
 * A player or AI's physical putting decision in the hole's canonical yard-space. Direction is a unit
 * vector in that space; pace is the intended level-ground roll in feet.
 */
public record PhysicalPuttIntent(Position2d direction, double intendedRollFeet) {
    public PhysicalPuttIntent {
        Objects.requireNonNull(direction, "direction");
        double length = StrictMath.hypot(direction.x(), direction.y());
        if (length < 1.0e-9 || !Double.isFinite(intendedRollFeet) || intendedRollFeet <= 0.0) {
            throw new IllegalArgumentException("physical putt direction must be non-zero and pace must be positive");
        }
        direction = new Position2d(direction.x() / length, direction.y() / length);
    }

    public double intendedRollYards() {
        return intendedRollFeet / 3.0;
    }
}
