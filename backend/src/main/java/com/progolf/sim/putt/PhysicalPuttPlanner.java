package com.progolf.sim.putt;

import com.progolf.sim.course.GreenSurface;
import com.progolf.sim.course.Position2d;
import com.progolf.sim.shot.BallState;
import com.progolf.sim.shot.HoleModel;
import com.progolf.sim.spatial.Surface;
import java.util.Objects;

/** Minimal deterministic AI line/pace decision for physical-policy rounds. */
public final class PhysicalPuttPlanner {
    private PhysicalPuttPlanner() {
    }

    /** A conservative AI choice; the player remains free to putt from any actual fringe lie. */
    public static boolean shouldPuttFromFringe(HoleModel hole, BallState ball, double remainingYards) {
        return hole.puttingPolicy().usesPhysicalPutting() && ball != null && ball.lie() == Surface.FRINGE
                && hole.geometry() != null && hole.geometry().surfaceAt(ball.position()) == Surface.FRINGE
                && remainingYards <= 10.0;
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
        double fringeDistance = surface.contains(start) ? 0.0 : distanceToGreenAlongLine(surface, start, cup);
        double fringeDragAdjustment = fringeDistance * (PhysicalPuttSolver.FRINGE_ROLLING_DECELERATION_YARDS_PER_SECOND_SQUARED
                / PhysicalPuttSolver.ROLLING_DECELERATION_YARDS_PER_SECOND_SQUARED - 1.0);
        return new PhysicalPuttIntent(new Position2d(dx / distance, dy / distance),
                Math.max(0.5, (distance + elevationAdjustment + fringeDragAdjustment) * 3.0));
    }

    private static double distanceToGreenAlongLine(GreenSurface surface, Position2d start, Position2d cup) {
        double rayX = cup.x() - start.x(), rayY = cup.y() - start.y();
        double firstCrossing = Double.POSITIVE_INFINITY;
        var boundary = surface.footprint();
        for (int index = 0; index < boundary.size(); index++) {
            Position2d a = boundary.get(index), b = boundary.get((index + 1) % boundary.size());
            double edgeX = b.x() - a.x(), edgeY = b.y() - a.y();
            double denominator = rayX * edgeY - rayY * edgeX;
            if (Math.abs(denominator) < 1.0e-10) continue;
            double offsetX = a.x() - start.x(), offsetY = a.y() - start.y();
            double rayFraction = (offsetX * edgeY - offsetY * edgeX) / denominator;
            double edgeFraction = (offsetX * rayY - offsetY * rayX) / denominator;
            if (rayFraction >= 0.0 && rayFraction <= 1.0 && edgeFraction >= 0.0 && edgeFraction <= 1.0) {
                firstCrossing = Math.min(firstCrossing, rayFraction);
            }
        }
        if (!Double.isFinite(firstCrossing)) throw new IllegalArgumentException("fringe line does not reach green");
        return firstCrossing * start.distanceTo(cup);
    }
}
