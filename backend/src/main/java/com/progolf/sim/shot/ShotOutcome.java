package com.progolf.sim.shot;

import com.progolf.sim.spatial.Surface;
import com.progolf.sim.putt.PhysicalPuttOutcome;
import java.util.Objects;

/**
 * The complete result of a resolved shot (REQ-062). Every field is populated; nothing is left
 * undefined. {@code strokes} counts this shot plus any penalty strokes incurred.
 */
public record ShotOutcome(
        Surface finalSurface,
        double carry,
        double lateral,
        double distanceRemaining,
        boolean hazardEntered,
        int penaltyStrokes,
        int strokes,
        FactorBreakdown factors,
        ShotSettlement settlement,
        boolean putt,
        ShotTrace trace,
        PhysicalPuttOutcome physicalPutt) {

    public ShotOutcome {
        Objects.requireNonNull(finalSurface, "finalSurface");
        Objects.requireNonNull(factors, "factors");
    }

    /** Compatibility constructor for unit fixtures and legacy zone resolution. */
    public ShotOutcome(Surface finalSurface, double carry, double lateral, double distanceRemaining,
                       boolean hazardEntered, int penaltyStrokes, int strokes, FactorBreakdown factors) {
        this(finalSurface, carry, lateral, distanceRemaining, hazardEntered, penaltyStrokes, strokes, factors,
                null, false, null, null);
    }

    /** Compatibility constructor for callers that have settlement metadata but no observable trace. */
    public ShotOutcome(Surface finalSurface, double carry, double lateral, double distanceRemaining,
                       boolean hazardEntered, int penaltyStrokes, int strokes, FactorBreakdown factors,
                       ShotSettlement settlement, boolean putt) {
        this(finalSurface, carry, lateral, distanceRemaining, hazardEntered, penaltyStrokes, strokes, factors,
                settlement, putt, null, null);
    }

    /** Compatibility constructor for existing callers that do not carry a physical-putt trajectory. */
    public ShotOutcome(Surface finalSurface, double carry, double lateral, double distanceRemaining,
                       boolean hazardEntered, int penaltyStrokes, int strokes, FactorBreakdown factors,
                       ShotSettlement settlement, boolean putt, ShotTrace trace) {
        this(finalSurface, carry, lateral, distanceRemaining, hazardEntered, penaltyStrokes, strokes, factors,
                settlement, putt, trace, null);
    }

    /** Surface at first canonical contact; equals final surface for legacy/non-spatial outcomes. */
    public Surface contactSurface() {
        return settlement == null ? finalSurface : settlement.contact().surface();
    }

    /**
     * Whether this resolved shot has finished the hole under the simulation's authoritative
     * cup threshold. Consumers should use this rather than interpreting a rounded distance.
     */
    public boolean holed() {
        return physicalPutt != null ? physicalPutt.holed() : !hazardEntered && distanceRemaining <= SimConstants.HOLED_THRESHOLD;
    }
}
