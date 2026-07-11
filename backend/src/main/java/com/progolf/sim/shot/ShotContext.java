package com.progolf.sim.shot;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.spatial.ShotZoneProfile;
import com.progolf.sim.spatial.Surface;
import java.util.Objects;

/**
 * The complete, immutable input to resolving one shot. A {@code ShotContext} fully determines the shot's
 * probability distribution; combined with its {@link SeedCoordinate} it also determines the sampled
 * outcome (spec: shot-resolution, deterministic-rng).
 *
 * <p>Note there is deliberately no control-type field: human and AI golfers are resolved by the same
 * model (REQ-104). {@code pinDistance} is the distance from the ball to the pin along the shot line.
 * {@code lie} is the surface the ball is played FROM; a shot from {@link Surface#GREEN} is a putt and is
 * resolved by the dedicated putting model (spec: shot-resolution putting).
 */
public record ShotContext(
        Attributes attributes,
        GolferState state,
        Environment environment,
        double pinDistance,
        ShotZoneProfile zoneProfile,
        ShotDecision decision,
        SeedCoordinate coordinate,
        Surface lie) {

    public ShotContext {
        Objects.requireNonNull(attributes, "attributes");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(zoneProfile, "zoneProfile");
        Objects.requireNonNull(decision, "decision");
        Objects.requireNonNull(coordinate, "coordinate");
        Objects.requireNonNull(lie, "lie");
        if (!Double.isFinite(pinDistance) || pinDistance < 0) {
            throw new IllegalArgumentException("pinDistance must be finite and >= 0: " + pinDistance);
        }
    }

    /**
     * Backward-compatible constructor for callers that do not track the ball's lie (tests and fixtures
     * that resolve a single tee-box shot). Defaults {@code lie} to {@link Surface#TEE_BOX}, so the putting
     * model is not triggered.
     */
    public ShotContext(Attributes attributes, GolferState state, Environment environment, double pinDistance,
                       ShotZoneProfile zoneProfile, ShotDecision decision, SeedCoordinate coordinate) {
        this(attributes, state, environment, pinDistance, zoneProfile, decision, coordinate, Surface.TEE_BOX);
    }
}
