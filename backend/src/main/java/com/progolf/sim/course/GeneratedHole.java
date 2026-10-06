package com.progolf.sim.course;

import com.progolf.sim.core.SplitMix64Rng;
import com.progolf.sim.core.Seeds;
import com.progolf.sim.shot.HoleModel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The immutable generated geometry of a single hole (REQ-070). It carries a hole seed from which
 * per-round pin positions are derived deterministically. It is not itself a {@link HoleModel} because
 * the playable model depends on the round's pin; call {@link #forRound(int)} to obtain one.
 */
public record GeneratedHole(
        int number,
        int par,
        double length,
        double fairwayHalfWidth,
        double greenHalfWidth,
        double greenDepth,
        boolean hasGreensideBunker,
        boolean hasWater,
        boolean hasTrees,
        double elevationDelta,
        long holeSeed,
        CourseGeometry geometry,
        HoleSpatialPlan spatialPlan,
        HazardPlan hazardPlan) {

    /** Cached immutable setup variants, shared by every competitor using the same generated hole. */
    private static final Map<GeometryVariantKey, CourseGeometry> SETUP_GEOMETRIES = new ConcurrentHashMap<>();

    public GeneratedHole {
        if (number < 1 || number > 18) {
            throw new IllegalArgumentException("Hole number must be 1..18: " + number);
        }
        if (par < 3 || par > 5) {
            throw new IllegalArgumentException("Par must be 3..5: " + par);
        }
        if (length <= 0 || fairwayHalfWidth <= 0 || greenHalfWidth <= 0 || greenDepth <= 0) {
            throw new IllegalArgumentException("Hole dimensions must be positive");
        }
        geometry = geometry == null
                ? CanonicalGeometryGenerator.generate(length, fairwayHalfWidth, greenHalfWidth, greenDepth,
                hasGreensideBunker, hasWater, hasTrees, holeSeed)
                : geometry;
    }

    /** Source-compatible constructor for existing fixtures; production generation supplies the same derived geometry. */
    public GeneratedHole(int number, int par, double length, double fairwayHalfWidth, double greenHalfWidth,
                         double greenDepth, boolean hasGreensideBunker, boolean hasWater, boolean hasTrees,
                         double elevationDelta, long holeSeed) {
        this(number, par, length, fairwayHalfWidth, greenHalfWidth, greenDepth, hasGreensideBunker, hasWater,
                hasTrees, elevationDelta, holeSeed, null, null, null);
    }

    /** Compatibility constructor for V3 callers; V4 supplies a semantic hazard plan. */
    public GeneratedHole(int number, int par, double length, double fairwayHalfWidth, double greenHalfWidth,
                         double greenDepth, boolean hasGreensideBunker, boolean hasWater, boolean hasTrees,
                         double elevationDelta, long holeSeed, CourseGeometry geometry, HoleSpatialPlan spatialPlan) {
        this(number, par, length, fairwayHalfWidth, greenHalfWidth, greenDepth, hasGreensideBunker, hasWater,
                hasTrees, elevationDelta, holeSeed, geometry, spatialPlan, null);
    }

    /** Deterministically derives the active pin for {@code round} under the neutral setup (REQ-076). */
    public PinPosition pinFor(int round) {
        return pinFor(round, CourseSetup.standard());
    }

    /**
     * Deterministically derives the active pin for {@code round} under {@code setup} (REQ-076). Same round
     * and setup -> same pin. A more aggressive setup tucks the pin deeper and closer to the (width-scaled)
     * green edge; the RNG draw is identical regardless of setup, so a setup only scales the offsets.
     */
    public PinPosition pinFor(int round, CourseSetup setup) {
        SplitMix64Rng rng = new SplitMix64Rng(Seeds.deriveSeed(holeSeed, round));
        double depth = (rng.nextDouble() * 2.0 - 1.0) * CourseGenConstants.PIN_DEPTH_RANGE * setup.pinAggression();
        // The flag stays on the (width-scaled) green: aggression tucks it toward the edge but never past it.
        double greenEdge = greenHalfWidth * setup.widthScale();
        double lateral = (rng.nextDouble() * 2.0 - 1.0)
                * greenEdge * CourseGenConstants.PIN_LATERAL_FACTOR * setup.pinAggression();
        lateral = Math.max(-greenEdge, Math.min(greenEdge, lateral));
        return new PinPosition(depth, lateral);
    }

    /** Returns the playable {@link HoleModel} for {@code round} under the neutral setup. */
    public HoleModel forRound(int round) {
        return forRound(round, CourseSetup.standard());
    }

    /** Returns the playable {@link HoleModel} for {@code round}, with that round's pin and setup applied. */
    public HoleModel forRound(int round, CourseSetup setup) {
        return new RoundHole(this, pinFor(round, setup), setup);
    }

    /** The active round cup in the same local coordinates as {@link #geometry()}. */
    public Position2d cupFor(PinPosition pin) {
        return new Position2d(geometry.greenCenter().x() + pin.lateralOffset(),
                geometry.greenCenter().y() + pin.depthOffset());
    }

    /**
     * Produces the setup-specific authoritative geometry. This deliberately regenerates the canonical
     * shapes from scaled fairway and green cores, rather than uniformly stretching all x coordinates:
     * {@link HoleZones} historically kept its rough, hazard, and recovery-envelope additions fixed.
     */
    CourseGeometry geometryForWidth(double widthScale) {
        if (widthScale == 1.0) {
            return geometry;
        }
        if (spatialPlan != null) {
            return geometry.withLateralScale(widthScale);
        }
        GeometryVariantKey key = new GeometryVariantKey(holeSeed, length, fairwayHalfWidth, greenHalfWidth,
                greenDepth, hasGreensideBunker, hasWater, hasTrees, widthScale);
        return SETUP_GEOMETRIES.computeIfAbsent(key, ignored -> CanonicalGeometryGenerator.generate(length,
                fairwayHalfWidth * widthScale, greenHalfWidth * widthScale, greenDepth, hasGreensideBunker,
                hasWater, hasTrees, holeSeed));
    }

    /** V3 progression target under the round's effective lateral setup; null keeps legacy green-centre behavior. */
    Position2d progressionTarget(Position2d ball, com.progolf.sim.shot.Strategy strategy, double widthScale) {
        if (spatialPlan == null) return null;
        return spatialPlan.withLateralScale(widthScale).progressionTarget(ball, strategy);
    }

    /**
     * Retains the historical record rendering for V1/V2 fingerprints. V3 intentionally includes its semantic
     * plan so diagnostics can inspect it, but null legacy plans do not perturb the previously locked corpus.
     */
    @Override
    public String toString() {
        String legacy = "GeneratedHole[number=" + number + ", par=" + par + ", length=" + length
                + ", fairwayHalfWidth=" + fairwayHalfWidth + ", greenHalfWidth=" + greenHalfWidth
                + ", greenDepth=" + greenDepth + ", hasGreensideBunker=" + hasGreensideBunker + ", hasWater="
                + hasWater + ", hasTrees=" + hasTrees + ", elevationDelta=" + elevationDelta + ", holeSeed="
                + holeSeed + ", geometry=" + geometry;
        if (spatialPlan == null) return legacy + "]";
        return hazardPlan == null ? legacy + ", spatialPlan=" + spatialPlan + "]"
                : legacy + ", spatialPlan=" + spatialPlan + ", hazardPlan=" + hazardPlan + "]";
    }

    private record GeometryVariantKey(long holeSeed, double length, double fairwayHalfWidth, double greenHalfWidth,
                                      double greenDepth, boolean bunker, boolean water, boolean trees,
                                      double widthScale) {
    }
}
