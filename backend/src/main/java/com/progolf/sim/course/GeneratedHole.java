package com.progolf.sim.course;

import com.progolf.sim.core.SplitMix64Rng;
import com.progolf.sim.core.Seeds;
import com.progolf.sim.shot.HoleModel;

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
        long holeSeed) {

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
    }

    /** Deterministically derives the active pin for {@code round} (REQ-076). Same round -> same pin. */
    public PinPosition pinFor(int round) {
        SplitMix64Rng rng = new SplitMix64Rng(Seeds.deriveSeed(holeSeed, round));
        double depth = (rng.nextDouble() * 2.0 - 1.0) * CourseGenConstants.PIN_DEPTH_RANGE;
        double lateral = (rng.nextDouble() * 2.0 - 1.0) * greenHalfWidth * CourseGenConstants.PIN_LATERAL_FACTOR;
        return new PinPosition(depth, lateral);
    }

    /** Returns the playable {@link HoleModel} for {@code round}, with that round's pin applied. */
    public HoleModel forRound(int round) {
        return new RoundHole(this, pinFor(round));
    }
}
