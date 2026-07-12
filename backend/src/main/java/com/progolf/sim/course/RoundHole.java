package com.progolf.sim.course;

import com.progolf.sim.shot.HoleModel;
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

    RoundHole(GeneratedHole hole, PinPosition pin) {
        this.hole = Objects.requireNonNull(hole, "hole");
        this.pin = Objects.requireNonNull(pin, "pin");
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
        return HoleZones.profileFor(hole, remainingDistance, pin.depthOffset());
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
}
