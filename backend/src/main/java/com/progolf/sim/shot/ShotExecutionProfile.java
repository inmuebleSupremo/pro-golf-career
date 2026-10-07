package com.progolf.sim.shot;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.spatial.Surface;
import java.util.Objects;

/** Pure, internal calibration profile for a legal non-putting strike. */
public record ShotExecutionProfile(
        ShotFamilyEligibility.Result eligibility,
        double effectiveCarryCapMultiplier,
        double lateralDispersionMultiplier,
        double distanceDispersionMultiplier,
        double mishitMultiplier,
        double baseGroundResponseYards) {

    public ShotExecutionProfile {
        Objects.requireNonNull(eligibility, "eligibility");
        if (!Double.isFinite(effectiveCarryCapMultiplier) || !Double.isFinite(lateralDispersionMultiplier)
                || !Double.isFinite(distanceDispersionMultiplier) || !Double.isFinite(mishitMultiplier)
                || !Double.isFinite(baseGroundResponseYards)) {
            throw new IllegalArgumentException("profile values must be finite");
        }
    }

    public static ShotExecutionProfile derive(Surface lie, ClubSpec club, ShotFamily family,
                                              Attributes attributes, Environment environment) {
        Objects.requireNonNull(attributes, "attributes");
        Objects.requireNonNull(environment, "environment");
        ShotFamilyEligibility.Result eligibility = ShotFamilyEligibility.evaluate(lie, club, family);
        double carry = 1.0, lateral = 1.0, distance = 1.0, mishit = 1.0, roll = 0.0;
        switch (family) {
            case FULL -> { }
            case CONTROLLED -> { carry = 0.82; lateral = 0.78; distance = 0.76; mishit = 0.88; }
            case PITCH -> { carry = 0.42; lateral = 0.82; distance = 0.78; roll = 0.75; }
            case CHIP -> { carry = 0.28; lateral = 0.76; distance = 0.74; roll = 2.0 + club.baseCarry() * 0.005; }
            // Generic bunkers do not preserve fairway-vs-greenside semantics yet. The extraction must
            // therefore reach a safe ordinary lie rather than repeatedly trapping the ball in a long bunker.
            case BUNKER -> { carry = 1.00; lateral = 1.28; distance = 1.30; mishit = 1.22; }
        }
        if (lie == Surface.DEEP_ROUGH || lie == Surface.RECOVERY_AREA || lie == Surface.TREES) {
            // Keep difficult lies meaningful without turning pre-existing recovery geometry into a
            // repeat-shot trap under the simplified resolver.
            carry *= 0.97;
            lateral *= 1.05;
            distance *= 1.05;
            mishit *= 1.06;
        }
        return new ShotExecutionProfile(eligibility, carry, lateral, distance, mishit, roll);
    }

    /** Bounded endpoint-only ground response; no traversal or physics claim is made. */
    public double rollYardsOn(Surface contactSurface) {
        if (baseGroundResponseYards <= 0 || !contactSurface.isPlayable()) return 0.0;
        return baseGroundResponseYards * switch (contactSurface) {
            case GREEN -> 0.70;
            case FRINGE -> 0.90;
            case FAIRWAY, TEE_BOX -> 1.0;
            case FIRST_CUT -> 0.70;
            case PRIMARY_ROUGH -> 0.45;
            case DEEP_ROUGH, RECOVERY_AREA, TREES, BUNKER, WASTE_AREA, WATER, OUT_OF_BOUNDS -> 0.0;
        };
    }
}
