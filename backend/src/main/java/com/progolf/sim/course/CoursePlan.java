package com.progolf.sim.course;

import java.util.List;
import java.util.Objects;

/** Immutable 18-hole V2 design plan, generated before any terrain is compiled. */
public record CoursePlan(CourseDesignProfile profile, List<HoleBrief> briefs) {
    public CoursePlan {
        Objects.requireNonNull(profile, "profile");
        briefs = List.copyOf(briefs);
        if (briefs.size() != 18) throw new IllegalArgumentException("A CoursePlan must have exactly 18 briefs");
        for (int i = 0; i < briefs.size(); i++) {
            if (briefs.get(i).number() != i + 1) {
                throw new IllegalArgumentException("Briefs must be numbered 1..18 in order");
            }
        }
        validateNine(briefs.subList(0, 9));
        validateNine(briefs.subList(9, 18));
        for (int par = 3; par <= 5; par++) {
            int expectedPar = par;
            if (briefs.stream().filter(b -> b.par() == expectedPar).map(HoleBrief::lengthBand).distinct().count() < 2) {
                throw new IllegalArgumentException("Each par class must use at least two length bands");
            }
        }
        List<HoleBrief> scoringHoles = briefs.stream().filter(b -> b.par() >= 4).toList();
        if (scoringHoles.stream().noneMatch(b -> b.archetype() == StrategicArchetype.POSITIONAL)
                || scoringHoles.stream().noneMatch(b -> b.archetype() == StrategicArchetype.RISK_REWARD)) {
            throw new IllegalArgumentException("Par 4s and par 5s must include positional and risk/reward opportunities");
        }
        for (int i = 3; i < briefs.size(); i++) {
            StrategicArchetype archetype = briefs.get(i).archetype();
            if (archetype == briefs.get(i - 1).archetype() && archetype == briefs.get(i - 2).archetype()
                    && archetype == briefs.get(i - 3).archetype()) {
                throw new IllegalArgumentException("No more than three adjacent briefs may share an archetype");
            }
        }
    }

    private static void validateNine(List<HoleBrief> nine) {
        long par3 = nine.stream().filter(b -> b.par() == 3).count();
        long par4 = nine.stream().filter(b -> b.par() == 4).count();
        long par5 = nine.stream().filter(b -> b.par() == 5).count();
        if (par3 != 2 || par4 != 5 || par5 != 2) {
            throw new IllegalArgumentException("Each nine must contain 2 par 3s, 5 par 4s, and 2 par 5s");
        }
    }
}
