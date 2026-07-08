package com.progolf.sim.course;

import com.progolf.sim.core.Rng;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.core.Seeds;
import com.progolf.sim.core.SplitMix64Rng;
import java.util.ArrayList;
import java.util.List;

/**
 * Procedurally generates a complete 18-hole {@link Course} deterministically from a seed coordinate
 * (REQ-068). All randomness flows through the world seed hierarchy, so a Course is fully reproducible
 * from its seed and {@link CourseGenConstants#GENERATOR_VERSION} (REQ-082, REQ-265/299).
 */
public final class CourseGenerator {

    private CourseGenerator() {
    }

    /** Generates a Course at the given course-scope coordinate with the given environment classification. */
    public static Course generate(SeedCoordinate courseCoordinate, EnvironmentClassification classification) {
        long courseSeed = Seeds.forCoordinate(courseCoordinate);
        Rng rng = new SplitMix64Rng(courseSeed);

        CourseIdentity identity = CourseNames.generate(courseSeed, classification);

        List<Integer> pars = parTemplate();
        shuffle(pars, rng);

        List<GeneratedHole> holes = new ArrayList<>(18);
        for (int number = 1; number <= 18; number++) {
            int par = pars.get(number - 1);
            long holeSeed = Seeds.deriveSeed(courseSeed, number);
            holes.add(generateHole(number, par, holeSeed, classification));
        }

        return new Course(identity, holes, CourseGenConstants.GENERATOR_VERSION);
    }

    private static GeneratedHole generateHole(int number, int par, long holeSeed, EnvironmentClassification classification) {
        Rng rng = new SplitMix64Rng(holeSeed);
        double length = switch (par) {
            case 3 -> range(rng, CourseGenConstants.PAR3_MIN, CourseGenConstants.PAR3_MAX);
            case 4 -> range(rng, CourseGenConstants.PAR4_MIN, CourseGenConstants.PAR4_MAX);
            default -> range(rng, CourseGenConstants.PAR5_MIN, CourseGenConstants.PAR5_MAX);
        };
        double fairwayHalf = range(rng, CourseGenConstants.FAIRWAY_HALF_MIN, CourseGenConstants.FAIRWAY_HALF_MAX);
        double greenHalf = range(rng, CourseGenConstants.GREEN_HALF_MIN, CourseGenConstants.GREEN_HALF_MAX);
        double greenDepth = range(rng, CourseGenConstants.GREEN_DEPTH_MIN, CourseGenConstants.GREEN_DEPTH_MAX);
        boolean bunker = rng.nextDouble() < CourseGenConstants.GREENSIDE_BUNKER_PROB;
        boolean water = rng.nextDouble() < classification.waterBias();
        boolean trees = rng.nextDouble() < classification.treeBias();
        double elevation = (rng.nextDouble() * 2.0 - 1.0) * CourseGenConstants.ELEVATION_RANGE;

        return new GeneratedHole(number, par, length, fairwayHalf, greenHalf, greenDepth,
                bunker, water, trees, elevation, holeSeed);
    }

    private static List<Integer> parTemplate() {
        List<Integer> pars = new ArrayList<>(18);
        for (int i = 0; i < CourseGenConstants.PAR3_COUNT; i++) {
            pars.add(3);
        }
        for (int i = 0; i < CourseGenConstants.PAR4_COUNT; i++) {
            pars.add(4);
        }
        for (int i = 0; i < CourseGenConstants.PAR5_COUNT; i++) {
            pars.add(5);
        }
        return pars;
    }

    /** Deterministic Fisher-Yates shuffle driven by the seeded generator. */
    private static void shuffle(List<Integer> list, Rng rng) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = (int) Math.floorMod(rng.nextLong(), i + 1);
            Integer tmp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, tmp);
        }
    }

    private static double range(Rng rng, double min, double max) {
        return min + rng.nextDouble() * (max - min);
    }
}
