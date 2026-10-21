package com.progolf.sim.putt;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.core.Seeds;
import com.progolf.sim.course.GreenSurface;
import com.progolf.sim.course.Position2d;
import com.progolf.sim.shot.BallState;
import com.progolf.sim.shot.FactorBreakdown;
import com.progolf.sim.shot.GolferState;
import com.progolf.sim.shot.HoleModel;
import com.progolf.sim.shot.RecoveryKind;
import com.progolf.sim.shot.ShotContact;
import com.progolf.sim.shot.ShotOutcome;
import com.progolf.sim.shot.ShotSettlement;
import com.progolf.sim.spatial.Surface;
import java.util.Objects;

/** Shared human/AI adapter from an event-pinned physical intent to the established shot outcome contract. */
public final class PhysicalPuttResolution {
    private PhysicalPuttResolution() {
    }

    public static ShotOutcome resolve(HoleModel hole, Attributes attributes, GolferState state,
                                      SeedCoordinate coordinate, BallState ball, PhysicalPuttIntent intent) {
        Objects.requireNonNull(hole, "hole");
        Objects.requireNonNull(attributes, "attributes");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(coordinate, "coordinate");
        Objects.requireNonNull(ball, "ball");
        Objects.requireNonNull(intent, "intent");
        if (hole.puttingPolicy() == null || !hole.puttingPolicy().usesPhysicalPutting()) {
            throw new IllegalStateException("physical putt requires a PHYSICAL_V1 hole policy");
        }
        if (ball.lie() != Surface.GREEN) {
            throw new IllegalArgumentException("physical putting is currently supported only from the green");
        }
        GreenSurface surface = Objects.requireNonNull(hole.greenSurface(), "physical putting requires GreenSurface V1");
        Position2d cup = Objects.requireNonNull(hole.cupPosition(), "physical putting requires an effective cup");
        PhysicalPuttOutcome physical = PhysicalPuttSolver.resolve(new PhysicalPuttRequest(
                hole.puttingPolicy().physicalModelVersion(), surface, ball.position(), cup, intent, attributes, state,
                Seeds.forCoordinate(coordinate), PhysicalPuttExecutionMode.ATTRIBUTE_DRIVEN));
        Position2d finalPosition = physical.finalPosition();
        Surface finalSurface = physical.holed() ? Surface.GREEN : hole.geometry().surfaceAt(finalPosition);
        boolean hazard = !physical.holed() && finalSurface.isHazard();
        int penalty = hazard ? finalSurface.penaltyStrokes() : 0;
        ShotSettlement settlement = finalSurface.isPlayable()
                ? new ShotSettlement(new ShotContact(finalPosition, finalSurface), null, RecoveryKind.NONE,
                new BallState(finalPosition, finalSurface)) : null;
        return new ShotOutcome(finalSurface, ball.position().distanceTo(finalPosition), 0.0,
                finalPosition.distanceTo(cup), hazard, penalty, 1 + penalty,
                new FactorBreakdown(0.0, 0.0, 0.0, physical.holed() ? 0.5 : -0.5), settlement, true, null, physical);
    }
}
