package com.progolf.sim.course;

import com.progolf.sim.core.Rng;
import com.progolf.sim.core.Seeds;
import com.progolf.sim.core.SplitMix64Rng;
import java.util.ArrayList;
import java.util.List;

/** Seeded, lightweight composition planner for V2 courses; it deliberately creates no terrain. */
final class CoursePlanGenerator {
    private static final long PROFILE_SALT = 0x50524f46494c45L; // PROFILE
    private static final long PLAN_SALT = 0x504c414eL; // PLAN

    private CoursePlanGenerator() {
    }

    static CoursePlan generate(long courseSeed) {
        return generate(courseSeed, profileFor(courseSeed));
    }

    static CoursePlan generate(long courseSeed, CourseDesignProfile profile) {
        Rng rng = new SplitMix64Rng(Seeds.deriveSeed(courseSeed, PLAN_SALT));
        List<Integer> pars = nineTemplate();
        shuffle(pars, rng);
        List<Integer> backNine = nineTemplate();
        shuffle(backNine, rng);
        pars.addAll(backNine);

        List<StrategicArchetype> archetypes = archetypesFor(profile.strategicEmphasis(), pars.size(), rng);
        List<HoleBrief> briefs = new ArrayList<>(18);
        int[] parOccurrences = new int[6];
        for (int i = 0; i < pars.size(); i++) {
            int par = pars.get(i);
            StrategicArchetype archetype = archetypes.get(i);
            LengthBand band = bandFor(profile.strategicEmphasis(), parOccurrences[par]++, archetype, par);
            briefs.add(new HoleBrief(i + 1, par, band, archetype, profile.recoverySeverity()));
        }
        ensureScoringArchetypes(briefs, profile.recoverySeverity());
        ensureLengthVariety(briefs, profile.recoverySeverity());
        return new CoursePlan(profile, briefs);
    }

    static CourseDesignProfile profileFor(long courseSeed) {
        Rng rng = new SplitMix64Rng(Seeds.deriveSeed(courseSeed, PROFILE_SALT));
        return new CourseDesignProfile(
                StrategicEmphasis.values()[(int) Math.floorMod(rng.nextLong(), StrategicEmphasis.values().length)],
                WidthTendency.values()[(int) Math.floorMod(rng.nextLong(), WidthTendency.values().length)],
                RecoverySeverity.values()[(int) Math.floorMod(rng.nextLong(), RecoverySeverity.values().length)]);
    }

    private static List<Integer> nineTemplate() {
        List<Integer> pars = new ArrayList<>(9);
        pars.addAll(List.of(3, 3));
        for (int i = 0; i < 5; i++) pars.add(4);
        pars.addAll(List.of(5, 5));
        return pars;
    }

    private static List<StrategicArchetype> archetypesFor(StrategicEmphasis emphasis, int size, Rng rng) {
        StrategicArchetype[] pattern = switch (emphasis) {
            case POSITIONAL -> new StrategicArchetype[] {StrategicArchetype.POSITIONAL, StrategicArchetype.BALANCED,
                    StrategicArchetype.POSITIONAL, StrategicArchetype.RISK_REWARD, StrategicArchetype.POSITIONAL,
                    StrategicArchetype.BALANCED};
            case RISK_REWARD -> new StrategicArchetype[] {StrategicArchetype.RISK_REWARD, StrategicArchetype.BALANCED,
                    StrategicArchetype.RISK_REWARD, StrategicArchetype.POSITIONAL, StrategicArchetype.RISK_REWARD,
                    StrategicArchetype.BALANCED};
            case BALANCED -> new StrategicArchetype[] {StrategicArchetype.BALANCED, StrategicArchetype.POSITIONAL,
                    StrategicArchetype.RISK_REWARD};
        };
        int offset = (int) Math.floorMod(rng.nextLong(), pattern.length);
        List<StrategicArchetype> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) result.add(pattern[(i + offset) % pattern.length]);
        return result;
    }

    private static LengthBand bandFor(StrategicEmphasis emphasis, int occurrence, StrategicArchetype archetype, int par) {
        if (par >= 4 && archetype == StrategicArchetype.RISK_REWARD && occurrence % 3 != 2) return LengthBand.SHORT;
        if (par >= 4 && archetype == StrategicArchetype.POSITIONAL && occurrence % 3 != 2) return LengthBand.LONG;
        LengthBand[] cycle = switch (emphasis) {
            case POSITIONAL -> new LengthBand[] {LengthBand.LONG, LengthBand.STANDARD, LengthBand.SHORT};
            case RISK_REWARD -> new LengthBand[] {LengthBand.SHORT, LengthBand.STANDARD, LengthBand.LONG};
            case BALANCED -> new LengthBand[] {LengthBand.STANDARD, LengthBand.SHORT, LengthBand.LONG};
        };
        return cycle[occurrence % cycle.length];
    }

    private static void shuffle(List<Integer> values, Rng rng) {
        for (int i = values.size() - 1; i > 0; i--) {
            int index = (int) Math.floorMod(rng.nextLong(), i + 1);
            Integer value = values.get(i);
            values.set(i, values.get(index));
            values.set(index, value);
        }
    }

    private static void ensureScoringArchetypes(List<HoleBrief> briefs, RecoverySeverity recovery) {
        ensureScoringArchetype(briefs, recovery, StrategicArchetype.POSITIONAL, 4);
        ensureScoringArchetype(briefs, recovery, StrategicArchetype.RISK_REWARD, 5);
    }

    private static void ensureScoringArchetype(List<HoleBrief> briefs, RecoverySeverity recovery,
                                                StrategicArchetype required, int preferredPar) {
        if (briefs.stream().anyMatch(b -> b.par() >= 4 && b.archetype() == required)) return;
        int index = -1;
        for (int i = 0; i < briefs.size(); i++) {
            if (briefs.get(i).par() == preferredPar) {
                index = i;
                break;
            }
        }
        HoleBrief prior = briefs.get(index);
        LengthBand band = required == StrategicArchetype.POSITIONAL ? LengthBand.LONG : LengthBand.SHORT;
        briefs.set(index, new HoleBrief(prior.number(), prior.par(), band, required, recovery));
    }

    private static void ensureLengthVariety(List<HoleBrief> briefs, RecoverySeverity recovery) {
        for (int par = 3; par <= 5; par++) {
            int targetPar = par;
            long variants = briefs.stream().filter(b -> b.par() == targetPar).map(HoleBrief::lengthBand).distinct().count();
            if (variants >= 2) continue;
            for (int i = 0; i < briefs.size(); i++) {
                HoleBrief prior = briefs.get(i);
                if (prior.par() == par) {
                    LengthBand alternate = prior.lengthBand() == LengthBand.SHORT ? LengthBand.STANDARD : LengthBand.SHORT;
                    briefs.set(i, new HoleBrief(prior.number(), prior.par(), alternate, prior.archetype(), recovery));
                    break;
                }
            }
        }
    }
}
