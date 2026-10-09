package com.progolf.sim.course;

import com.progolf.sim.core.Rng;
import com.progolf.sim.core.Seeds;
import com.progolf.sim.core.SplitMix64Rng;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Course-level environmental composition for V5; it ranks generated holes and never selects authored templates. */
final class V5EnvironmentPlanner {
    private static final long ENVIRONMENT_SALT = 0x5635454e564952L;

    private V5EnvironmentPlanner() { }

    static List<EnvironmentHoleCharacter> plan(CoursePlan coursePlan, EnvironmentClassification environment,
                                                CourseArchitectureProfile architecture, long courseSeed) {
        List<TreeEnclosure> enclosure = new ArrayList<>();
        for (int index = 0; index < coursePlan.briefs().size(); index++) {
            enclosure.add(initialEnclosure(environment, index, courseSeed));
        }
        switch (environment) {
            case PARKLAND -> promote(enclosure, 8 + (int) StrictMath.round(architecture.widthRhythm() * 3.0),
                    TreeEnclosure.BROKEN_LINED, courseSeed, 0x5041524bL);
            case WOODLAND -> {
                promote(enclosure, 7 + (int) StrictMath.round(architecture.greenDefence() * 3.0),
                        TreeEnclosure.BROKEN_LINED, courseSeed, 0x574f4f44L);
                promote(enclosure, 5 + (int) StrictMath.round(architecture.lateralAsymmetry() * 3.0),
                        TreeEnclosure.WOODED, courseSeed, 0x574f4f32L);
            }
            default -> { }
        }
        boolean coastal = environment == EnvironmentClassification.COASTAL || environment == EnvironmentClassification.LINKS;
        int coastTarget = coastal ? 4 + (int) StrictMath.floor(architecture.approachOpenness() * 2.0) : 0;
        List<Integer> coastalHoles = rankedHoles(coursePlan, courseSeed, 0x434f415354L).stream().limit(coastTarget).toList();
        List<EnvironmentHoleCharacter> result = new ArrayList<>();
        for (int index = 0; index < enclosure.size(); index++) {
            result.add(new EnvironmentHoleCharacter(enclosure.get(index), coastalHoles.contains(index)));
        }
        return List.copyOf(result);
    }

    private static TreeEnclosure initialEnclosure(EnvironmentClassification environment, int index, long courseSeed) {
        Rng rng = new SplitMix64Rng(Seeds.deriveSeed(courseSeed, ENVIRONMENT_SALT + index));
        return switch (environment) {
            case PARKLAND -> rng.nextDouble() < .48 ? TreeEnclosure.SCATTERED : TreeEnclosure.BROKEN_LINED;
            case WOODLAND -> rng.nextDouble() < .35 ? TreeEnclosure.BROKEN_LINED : TreeEnclosure.WOODED;
            case MOUNTAIN -> rng.nextDouble() < .42 ? TreeEnclosure.SCATTERED : TreeEnclosure.OPEN;
            case COASTAL -> rng.nextDouble() < .18 ? TreeEnclosure.SCATTERED : TreeEnclosure.OPEN;
            case LINKS, DESERT -> rng.nextDouble() < .08 ? TreeEnclosure.SCATTERED : TreeEnclosure.OPEN;
        };
    }

    private static void promote(List<TreeEnclosure> enclosure, int target, TreeEnclosure promoted, long courseSeed, long salt) {
        for (int index : rankedIndices(enclosure.size(), courseSeed, salt).stream().limit(target).toList()) {
            if (enclosure.get(index).ordinal() < promoted.ordinal()) enclosure.set(index, promoted);
        }
    }

    private static List<Integer> rankedHoles(CoursePlan coursePlan, long courseSeed, long salt) {
        List<Integer> order = rankedIndices(coursePlan.briefs().size(), courseSeed, salt);
        order.sort(Comparator.comparingDouble(index -> suitability(coursePlan.briefs().get(index), courseSeed, salt)));
        return order;
    }

    private static List<Integer> rankedIndices(int size, long courseSeed, long salt) {
        List<Integer> order = new ArrayList<>();
        for (int index = 0; index < size; index++) order.add(index);
        order.sort(Comparator.comparingLong(index -> Seeds.deriveSeed(courseSeed, salt + index)));
        return order;
    }

    private static double suitability(HoleBrief brief, long courseSeed, long salt) {
        // Longer holes can show an extended shore without putting a green or tee into misleading disconnected land.
        double parPreference = brief.par() == 5 ? -2.0 : brief.par() == 4 ? -1.0 : 0.4;
        return parPreference + Math.floorMod(Seeds.deriveSeed(courseSeed, salt + brief.number()), 10_000) / 10_000.0;
    }
}
