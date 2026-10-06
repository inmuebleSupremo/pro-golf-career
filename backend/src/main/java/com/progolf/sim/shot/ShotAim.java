package com.progolf.sim.shot;

import com.progolf.sim.course.Position2d;
import java.util.Objects;

/** Shared compatibility bridge from an optional V3 route target to the existing carry/lateral shot engine. */
public final class ShotAim {
    private ShotAim() {
    }

    public record Reference(Position2d target, double pinLateral, boolean progressionTarget) {
    }

    public static Reference forBall(HoleModel hole, BallState ball, Strategy strategy) {
        Objects.requireNonNull(hole, "hole");
        Objects.requireNonNull(ball, "ball");
        Objects.requireNonNull(strategy, "strategy");
        Position2d cup = Objects.requireNonNull(hole.cupPosition(), "spatial hole cupPosition");
        Position2d legacy = new Position2d(hole.geometry().greenCenter().x(), cup.y());
        Position2d target = hole.progressionTarget(ball.position(), strategy);
        if (target == null) {
            ShotFrame frame = ShotFrame.towardGreenCentreReference(ball.position(), hole.geometry().greenCenter(), cup);
            return new Reference(legacy, frame.lateralTo(cup), false);
        }
        ShotFrame frame = ShotFrame.toward(ball.position(), target);
        boolean finalApproach = target.distanceTo(hole.geometry().greenCenter()) <= 1.0;
        return new Reference(target, finalApproach ? frame.lateralTo(cup) : 0.0, !finalApproach);
    }
}
