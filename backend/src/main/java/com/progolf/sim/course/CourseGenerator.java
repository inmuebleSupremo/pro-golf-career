package com.progolf.sim.course;

import com.progolf.sim.core.Rng;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.core.Seeds;
import com.progolf.sim.core.SplitMix64Rng;
import java.util.ArrayList;
import java.util.List;

/** Pure, explicit-version registry for deterministic course generation. */
public final class CourseGenerator {
    private CourseGenerator() {
    }

    /** Compatibility entry point retained for existing fixtures; new worlds pass an explicit version. */
    public static Course generate(SeedCoordinate coordinate, EnvironmentClassification classification) {
        return generate(coordinate, classification, CourseGenConstants.V1_GENERATOR_VERSION);
    }

    /** Generates a course through one retained, explicitly selected implementation. */
    public static Course generate(SeedCoordinate coordinate, EnvironmentClassification classification, int version) {
        return switch (version) {
            case CourseGenConstants.V1_GENERATOR_VERSION -> generateV1(coordinate, classification);
            case CourseGenConstants.V2_GENERATOR_VERSION -> generateV2(coordinate, classification);
            case CourseGenConstants.V3_GENERATOR_VERSION -> generateV3(coordinate, classification);
            default -> throw new IllegalArgumentException("Unsupported course generator version: " + version);
        };
    }

    public static boolean supports(int version) {
        return version == CourseGenConstants.V1_GENERATOR_VERSION || version == CourseGenConstants.V2_GENERATOR_VERSION
                || version == CourseGenConstants.V3_GENERATOR_VERSION;
    }

    /** Historical V1 implementation retained unchanged in behavior. */
    private static Course generateV1(SeedCoordinate coordinate, EnvironmentClassification classification) {
        long courseSeed = Seeds.forCoordinate(coordinate);
        Rng rng = new SplitMix64Rng(courseSeed);
        CourseIdentity identity = CourseNames.generate(courseSeed, classification);
        List<Integer> pars = parTemplate();
        shuffle(pars, rng);
        List<GeneratedHole> holes = new ArrayList<>(18);
        for (int number = 1; number <= 18; number++) {
            int par = pars.get(number - 1);
            long holeSeed = Seeds.deriveSeed(courseSeed, number);
            holes.add(generateV1Hole(number, par, holeSeed, classification));
        }
        return new Course(identity, holes, CourseGenConstants.V1_GENERATOR_VERSION);
    }

    private static GeneratedHole generateV1Hole(int number, int par, long holeSeed,
                                                EnvironmentClassification classification) {
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

    private static Course generateV2(SeedCoordinate coordinate, EnvironmentClassification classification) {
        long courseSeed = Seeds.forCoordinate(coordinate);
        CoursePlan plan = CoursePlanGenerator.generate(courseSeed);
        return generateV2(coordinate, classification, plan);
    }

    /** Package-private calibration seam for exercising every legal design profile without API exposure. */
    static Course generateV2(SeedCoordinate coordinate, EnvironmentClassification classification,
                             CourseDesignProfile profile) {
        return generateV2(coordinate, classification, CoursePlanGenerator.generate(Seeds.forCoordinate(coordinate), profile));
    }

    private static Course generateV2(SeedCoordinate coordinate, EnvironmentClassification classification, CoursePlan plan) {
        long courseSeed = Seeds.forCoordinate(coordinate);
        CourseIdentity identity = CourseNames.generate(courseSeed, classification);
        List<GeneratedHole> holes = new ArrayList<>(18);
        for (HoleBrief brief : plan.briefs()) {
            holes.add(generateV2Hole(brief, Seeds.deriveSeed(courseSeed, brief.number()), classification, plan.profile()));
        }
        return new Course(identity, holes, CourseGenConstants.V2_GENERATOR_VERSION, plan.profile(), plan);
    }

    private static GeneratedHole generateV2Hole(HoleBrief brief, long holeSeed,
                                                EnvironmentClassification classification, CourseDesignProfile profile) {
        Rng rng = new SplitMix64Rng(holeSeed);
        double[] range = parRange(brief.par());
        double length = lengthForBand(rng, range[0], range[1], brief.lengthBand());
        double widthMultiplier = switch (profile.widthTendency()) {
            case GENEROUS -> 1.18;
            case BALANCED -> 1.0;
            case EXACTING -> 0.84;
        };
        widthMultiplier *= switch (brief.archetype()) {
            case POSITIONAL -> 0.92;
            case BALANCED -> 1.0;
            case RISK_REWARD -> 0.97;
        };
        double fairwayHalf = Math.clamp(range(rng, CourseGenConstants.FAIRWAY_HALF_MIN,
                CourseGenConstants.FAIRWAY_HALF_MAX) * widthMultiplier, 12.0, 28.0);
        double greenHalf = range(rng, CourseGenConstants.GREEN_HALF_MIN, CourseGenConstants.GREEN_HALF_MAX);
        double greenDepth = range(rng, CourseGenConstants.GREEN_DEPTH_MIN, CourseGenConstants.GREEN_DEPTH_MAX);
        double bunkerProbability = switch (brief.recoverySeverity()) {
            case FORGIVING -> 0.45;
            case BALANCED -> CourseGenConstants.GREENSIDE_BUNKER_PROB;
            case PENAL -> 0.85;
        };
        double waterProbability = recoveryProbability(classification.waterBias(), brief.recoverySeverity());
        double treeProbability = recoveryProbability(classification.treeBias(), brief.recoverySeverity());
        boolean bunker = rng.nextDouble() < bunkerProbability;
        boolean water = rng.nextDouble() < waterProbability;
        boolean trees = rng.nextDouble() < treeProbability;
        double elevation = (rng.nextDouble() * 2.0 - 1.0) * CourseGenConstants.ELEVATION_RANGE;
        return new GeneratedHole(brief.number(), brief.par(), length, fairwayHalf, greenHalf, greenDepth,
                bunker, water, trees, elevation, holeSeed);
    }

    private static Course generateV3(SeedCoordinate coordinate, EnvironmentClassification classification) {
        long courseSeed = Seeds.forCoordinate(coordinate);
        CoursePlan plan = CoursePlanGenerator.generate(courseSeed);
        return generateV3(coordinate, classification, plan);
    }

    /** Package-private calibration seam for the locked V3 profile corpus. */
    static Course generateV3(SeedCoordinate coordinate, EnvironmentClassification classification,
                             CourseDesignProfile profile) {
        return generateV3(coordinate, classification, CoursePlanGenerator.generate(Seeds.forCoordinate(coordinate), profile));
    }

    private static Course generateV3(SeedCoordinate coordinate, EnvironmentClassification classification, CoursePlan plan) {
        long courseSeed = Seeds.forCoordinate(coordinate);
        CourseIdentity identity = CourseNames.generate(courseSeed, classification);
        List<GeneratedHole> holes = new ArrayList<>(18);
        for (HoleBrief brief : plan.briefs()) {
            holes.add(generateV3Hole(brief, Seeds.deriveSeed(courseSeed, brief.number()), classification, plan.profile()));
        }
        return new Course(identity, holes, CourseGenConstants.V3_GENERATOR_VERSION, plan.profile(), plan);
    }

    private static GeneratedHole generateV3Hole(HoleBrief brief, long holeSeed,
                                                EnvironmentClassification classification, CourseDesignProfile profile) {
        Rng rng = new SplitMix64Rng(holeSeed);
        double[] range = parRange(brief.par());
        double length = lengthForBand(rng, range[0], range[1], brief.lengthBand());
        double widthMultiplier = switch (profile.widthTendency()) {
            case GENEROUS -> 1.18;
            case BALANCED -> 1.0;
            case EXACTING -> 0.84;
        };
        widthMultiplier *= switch (brief.archetype()) {
            case POSITIONAL -> 0.92;
            case BALANCED -> 1.0;
            case RISK_REWARD -> 0.97;
        };
        double fairwayHalf = Math.clamp(range(rng, CourseGenConstants.FAIRWAY_HALF_MIN,
                CourseGenConstants.FAIRWAY_HALF_MAX) * widthMultiplier, 12.0, 28.0);
        double greenHalf = range(rng, CourseGenConstants.GREEN_HALF_MIN, CourseGenConstants.GREEN_HALF_MAX);
        double greenDepth = range(rng, CourseGenConstants.GREEN_DEPTH_MIN, CourseGenConstants.GREEN_DEPTH_MAX);
        double bunkerProbability = switch (brief.recoverySeverity()) {
            case FORGIVING -> 0.45;
            case BALANCED -> CourseGenConstants.GREENSIDE_BUNKER_PROB;
            case PENAL -> 0.85;
        };
        boolean bunker = rng.nextDouble() < bunkerProbability;
        boolean water = rng.nextDouble() < recoveryProbability(classification.waterBias(), brief.recoverySeverity());
        boolean trees = rng.nextDouble() < recoveryProbability(classification.treeBias(), brief.recoverySeverity());
        double elevation = (rng.nextDouble() * 2.0 - 1.0) * CourseGenConstants.ELEVATION_RANGE;
        HoleSpatialPlan spatialPlan = V3HolePlanner.plan(brief, length, fairwayHalf, greenHalf, greenDepth, holeSeed);
        CourseGeometry geometry = CanonicalGeometryGenerator.generateV3(fairwayHalf, bunker, water, trees, holeSeed,
                spatialPlan);
        return new GeneratedHole(brief.number(), brief.par(), length, fairwayHalf, greenHalf, greenDepth,
                bunker, water, trees, elevation, holeSeed, geometry, spatialPlan);
    }

    private static double recoveryProbability(double biomeProbability, RecoverySeverity recovery) {
        return switch (recovery) {
            case FORGIVING -> biomeProbability * 0.65;
            case BALANCED -> biomeProbability;
            case PENAL -> Math.min(0.85, biomeProbability + 0.16);
        };
    }

    private static double[] parRange(int par) {
        return switch (par) {
            case 3 -> new double[] {CourseGenConstants.PAR3_MIN, CourseGenConstants.PAR3_MAX};
            case 4 -> new double[] {CourseGenConstants.PAR4_MIN, CourseGenConstants.PAR4_MAX};
            case 5 -> new double[] {CourseGenConstants.PAR5_MIN, CourseGenConstants.PAR5_MAX};
            default -> throw new IllegalArgumentException("Par must be 3..5");
        };
    }

    private static double lengthForBand(Rng rng, double min, double max, LengthBand band) {
        double span = max - min;
        return switch (band) {
            case SHORT -> range(rng, min, min + span * 0.32);
            case STANDARD -> range(rng, min + span * 0.28, min + span * 0.65);
            case LONG -> range(rng, min + span * 0.58, min + span * 0.96);
        };
    }

    private static List<Integer> parTemplate() {
        List<Integer> pars = new ArrayList<>(18);
        for (int i = 0; i < CourseGenConstants.PAR3_COUNT; i++) pars.add(3);
        for (int i = 0; i < CourseGenConstants.PAR4_COUNT; i++) pars.add(4);
        for (int i = 0; i < CourseGenConstants.PAR5_COUNT; i++) pars.add(5);
        return pars;
    }

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
