package com.progolf.sim.putt;

import com.progolf.sim.course.GreenSurface;
import com.progolf.sim.course.Position2d;
import java.util.Objects;

/** Minimal deterministic AI line/pace decision for physical-policy rounds. */
public final class PhysicalPuttPlanner {
    private PhysicalPuttPlanner() {
    }

    /**
     * Starts on the cup line and uses the authoritative elevation difference for a first-order pace
     * adjustment. It intentionally does not optimise break; execution and rolling remain the shared solver's job.
     */
    public static PhysicalPuttIntent decide(GreenSurface surface, Position2d start, Position2d cup) {
        Objects.requireNonNull(surface, "surface");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(cup, "cup");
        double dx = cup.x() - start.x();
        double dy = cup.y() - start.y();
        double distance = StrictMath.hypot(dx, dy);
        if (distance < 1.0e-9) throw new IllegalArgumentException("physical AI putt requires a non-zero cup distance");
        // Convert gravitational potential between start and cup into equivalent level-ground roll distance.
        double elevationAdjustment = PhysicalPuttSolver.GRAVITY_YARDS_PER_SECOND_SQUARED
                / PhysicalPuttSolver.ROLLING_DECELERATION_YARDS_PER_SECOND_SQUARED
                * (surface.elevationAt(cup) - surface.elevationAt(start));
        return new PhysicalPuttIntent(new Position2d(dx / distance, dy / distance),
                Math.max(0.5, (distance + elevationAdjustment) * 3.0));
    }
}
