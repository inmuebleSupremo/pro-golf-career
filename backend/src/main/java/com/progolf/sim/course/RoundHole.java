package com.progolf.sim.course;

import com.progolf.sim.shot.HoleModel;
import com.progolf.sim.shot.Strategy;
import com.progolf.sim.spatial.ShotZoneProfile;
import java.util.Objects;

/**
 * A {@link GeneratedHole} bound to a specific round's {@link PinPosition}, implementing the exact
 * {@link HoleModel} contract the shot engine consumes. The pin's depth offset shifts the effective
 * playing length; the hole geometry itself stays immutable (REQ-076).
 */
public final class RoundHole implements HoleModel {

    private final GeneratedHole hole;
    private final PinPosition pin;
    private final CourseSetup setup;
    private final CourseGeometry geometry;
    private final Position2d cup;

    RoundHole(GeneratedHole hole, PinPosition pin) {
        this(hole, pin, CourseSetup.standard());
    }

    RoundHole(GeneratedHole hole, PinPosition pin, CourseSetup setup) {
        this(hole, pin, setup, null);
    }

    RoundHole(GeneratedHole hole, PinPosition pin, CourseSetup setup, Position2d cup) {
        this.hole = Objects.requireNonNull(hole, "hole");
        this.pin = Objects.requireNonNull(pin, "pin");
        this.setup = Objects.requireNonNull(setup, "setup");
        this.geometry = hole.geometryForWidth(setup.widthScale());
        this.cup = cup == null ? new Position2d(geometry.greenCenter().x() + pin.lateralOffset(),
                geometry.greenCenter().y() + pin.depthOffset()) : cup;
    }

    /** The hole this round-model is derived from. */
    public GeneratedHole hole() {
        return hole;
    }

    /** The active pin for this round. */
    public PinPosition pin() {
        return pin;
    }

    @Override
    public double startDistance() {
        return Math.max(1.0, hole.length() + pin.depthOffset());
    }

    @Override
    public ShotZoneProfile zoneProfileFor(double remainingDistance) {
        return HoleZones.profileFor(hole, remainingDistance, pin.depthOffset(), setup.widthScale());
    }

    /** The active pin's lateral offset from the green centre (spec: shot-resolution) — live in resolution. */
    @Override
    public double pinLateral() {
        return pin.lateralOffset();
    }

    @Override
    public int par() {
        return hole.par();
    }

    @Override
    public CourseGeometry geometry() {
        return geometry;
    }

    @Override
    public Position2d cupPosition() {
        return cup;
    }

    @Override
    public Position2d progressionTarget(Position2d ball, Strategy strategy) {
        return hole.progressionTarget(ball, strategy, setup.widthScale());
    }
}
