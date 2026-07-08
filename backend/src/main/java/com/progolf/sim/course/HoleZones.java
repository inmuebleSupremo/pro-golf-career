package com.progolf.sim.course;

import com.progolf.sim.spatial.LateralRegion;
import com.progolf.sim.spatial.ShotZoneProfile;
import com.progolf.sim.spatial.Surface;
import com.progolf.sim.spatial.ZoneBand;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a valid {@link ShotZoneProfile} for a shot played from a given remaining distance on a hole
 * (the "compute in 1D" side of the hybrid model). The reachable carry line is partitioned into an
 * approach corridor, a green complex centred on the pin distance, and an over-green trouble zone —
 * always contiguous and gap-free so every landing resolves to exactly one surface.
 */
final class HoleZones {

    private HoleZones() {
    }

    static ShotZoneProfile profileFor(GeneratedHole hole, double remainingDistance) {
        double greenHalfDepth = hole.greenDepth() / 2.0;
        double greenStart = remainingDistance - greenHalfDepth - CourseGenConstants.APPROACH_FRINGE;
        double greenEnd = remainingDistance + greenHalfDepth + CourseGenConstants.APPROACH_FRINGE;
        double reachCap = Math.max(remainingDistance * 1.5, greenEnd + CourseGenConstants.OVER_GREEN_MARGIN);

        List<ZoneBand> bands = new ArrayList<>();
        double cursor = 0.0;

        // Approach corridor (only when there is meaningful room before the green complex).
        if (greenStart > cursor + 1.0) {
            bands.add(fairwayBand(cursor, greenStart, hole));
            cursor = greenStart;
        }

        // Green complex, centred on the pin distance.
        double gEnd = Math.max(greenEnd, cursor + 1.0);
        bands.add(greenBand(cursor, gEnd, hole));
        cursor = gEnd;

        // Over-green trouble.
        if (reachCap > cursor + 1.0) {
            bands.add(overGreenBand(cursor, reachCap, hole));
        }

        return new ShotZoneProfile(bands);
    }

    private static ZoneBand fairwayBand(double start, double end, GeneratedHole hole) {
        List<LateralRegion> regions = new ArrayList<>();
        double w = hole.fairwayHalfWidth();
        regions.add(new LateralRegion(w, Surface.FAIRWAY));
        w += CourseGenConstants.FAIRWAY_FIRST_CUT_EXTRA;
        regions.add(new LateralRegion(w, Surface.FIRST_CUT));
        w += CourseGenConstants.FAIRWAY_ROUGH_EXTRA;
        regions.add(new LateralRegion(w, Surface.PRIMARY_ROUGH));
        w += CourseGenConstants.FAIRWAY_DEEP_EXTRA;
        regions.add(new LateralRegion(w, Surface.DEEP_ROUGH));
        addOuterHazard(regions, w, hole); // beyond the widest region -> OUT_OF_BOUNDS (implicit)
        return new ZoneBand(start, end, regions);
    }

    private static ZoneBand greenBand(double start, double end, GeneratedHole hole) {
        List<LateralRegion> regions = new ArrayList<>();
        double w = hole.greenHalfWidth();
        regions.add(new LateralRegion(w, Surface.GREEN));
        w += CourseGenConstants.GREEN_FRINGE_EXTRA;
        regions.add(new LateralRegion(w, Surface.FRINGE));
        if (hole.hasGreensideBunker()) {
            w += CourseGenConstants.GREEN_BUNKER_EXTRA;
            regions.add(new LateralRegion(w, Surface.BUNKER));
        }
        w += CourseGenConstants.GREEN_ROUGH_EXTRA;
        regions.add(new LateralRegion(w, Surface.PRIMARY_ROUGH));
        addOuterHazard(regions, w, hole);
        return new ZoneBand(start, end, regions);
    }

    private static ZoneBand overGreenBand(double start, double end, GeneratedHole hole) {
        List<LateralRegion> regions = new ArrayList<>();
        double w = CourseGenConstants.FRINGE_WIDTH + CourseGenConstants.GREEN_FRINGE_EXTRA;
        regions.add(new LateralRegion(w, Surface.FRINGE));
        w += CourseGenConstants.GREEN_ROUGH_EXTRA;
        regions.add(new LateralRegion(w, Surface.PRIMARY_ROUGH));
        w += CourseGenConstants.FAIRWAY_DEEP_EXTRA;
        regions.add(new LateralRegion(w, Surface.DEEP_ROUGH));
        addOuterHazard(regions, w, hole);
        return new ZoneBand(start, end, regions);
    }

    /** Adds a water or tree outer band when the hole carries that hazard; otherwise leaves OB as the flank. */
    private static void addOuterHazard(List<LateralRegion> regions, double currentWidth, GeneratedHole hole) {
        if (hole.hasWater()) {
            regions.add(new LateralRegion(currentWidth + CourseGenConstants.HAZARD_EXTRA, Surface.WATER));
        } else if (hole.hasTrees()) {
            regions.add(new LateralRegion(currentWidth + CourseGenConstants.HAZARD_EXTRA, Surface.TREES));
        }
        // Anything beyond the widest region resolves to OUT_OF_BOUNDS by the ZoneBand contract.
    }
}
