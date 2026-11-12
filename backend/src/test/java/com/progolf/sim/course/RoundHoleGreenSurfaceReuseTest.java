package com.progolf.sim.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeasonProfile;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.putt.PuttingPolicy;
import com.progolf.sim.shot.Environment;
import com.progolf.sim.shot.GolferState;
import com.progolf.sim.shot.RoundResolver;
import com.progolf.sim.shot.Strategy;
import org.junit.jupiter.api.Test;

class RoundHoleGreenSurfaceReuseTest {
    private static final SeedCoordinate SEED = new SeedCoordinate(0x505859534943414cL, 2, 43, 1, 4, 7, 0);
    private static final Course COURSE = course(SEED);

    @Test
    void generatesLazilyOncePerPhysicalModelAndNeverForLegacy() {
        RoundHole physical = model(COURSE, 7, 1, CourseSetup.standard(), PuttingPolicy.PHYSICAL_V1);
        RoundHole legacy = model(COURSE, 7, 1, CourseSetup.standard(), PuttingPolicy.LEGACY);

        try (SeasonProfile profile = SeasonProfile.start()) {
            assertThat(legacy.greenSurface()).isNull();
            assertThat(profile.summary("TEST", 0)).contains("greenSurfaces=0");

            GreenSurface first = physical.greenSurface();
            assertThat(physical.greenSurface()).isSameAs(first);
            assertThat(physical.greenSurface()).isSameAs(first);
            assertThat(profile.summary("TEST", 0)).contains("greenSurfaces=1");
        }
        assertThat(SeasonProfile.current()).isNull();
    }

    @Test
    void cachedFieldMatchesDirectGenerationAndOtherModelsRemainIndependent() {
        RoundHole first = model(COURSE, 7, 1, CourseSetup.standard(), PuttingPolicy.PHYSICAL_V1);
        RoundHole second = model(COURSE, 7, 2, CourseSetup.standard(), PuttingPolicy.PHYSICAL_V1);
        GeneratedHole generated = COURSE.holes().get(6);
        GreenSurface direct = GreenSurfaceGenerator.generate(first.geometry(), generated.holeSeed(),
                generated.spatialPlan().greenComplex().approachDirection(), GreenSurfaceVersion.V1);

        assertSameField(first.greenSurface(), direct);
        assertThat(second.greenSurface()).isNotSameAs(first.greenSurface());
        assertSameField(second.greenSurface(), direct); // the pin changes by round; the field does not

        RoundHole narrow = model(COURSE, 7, 1, new CourseSetup(1.0, 1.0, 0.85), PuttingPolicy.PHYSICAL_V1);
        assertThat(narrow.greenSurface()).isNotSameAs(first.greenSurface());
        assertThat(narrow.greenSurface().provenance().footprintFingerprint())
                .isNotEqualTo(first.greenSurface().provenance().footprintFingerprint());

        RoundHole otherSeed = model(course(new SeedCoordinate(SEED.worldSeed(), SEED.seasonId(), 44,
                SEED.roundNo(), SEED.golferId(), SEED.holeNo(), SEED.shotNo())), 7, 1,
                CourseSetup.standard(), PuttingPolicy.PHYSICAL_V1);
        assertThat(otherSeed.greenSurface()).isNotSameAs(first.greenSurface());
        assertThat(otherSeed.greenSurface().provenance().surfaceSeed())
                .isNotEqualTo(first.greenSurface().provenance().surfaceSeed());
    }

    @Test
    void prewarmingTheSurfaceDoesNotChangePhysicalRoundOutcomes() {
        RoundHole warmed = model(COURSE, 7, 1, CourseSetup.standard(), PuttingPolicy.PHYSICAL_V1);
        RoundHole fresh = model(COURSE, 7, 1, CourseSetup.standard(), PuttingPolicy.PHYSICAL_V1);
        warmed.greenSurface();

        var withReuse = RoundResolver.resolveHole(warmed, Attributes.uniform(80), GolferState.fresh(),
                Environment.calm(), Strategy.BALANCED, SEED);
        var withoutPrewarming = RoundResolver.resolveHole(fresh, Attributes.uniform(80), GolferState.fresh(),
                Environment.calm(), Strategy.BALANCED, SEED);

        assertThat(withReuse).isEqualTo(withoutPrewarming);
        assertThat(withReuse.shots()).anyMatch(shot -> shot.physicalPutt() != null);
    }

    private static RoundHole model(Course course, int hole, int round, CourseSetup setup, PuttingPolicy policy) {
        return (RoundHole) course.holeModel(hole, round, setup, PinPlacementVersion.V5_EFFECTIVE_GREEN, policy);
    }

    private static Course course(SeedCoordinate seed) {
        return CourseGenerator.generate(seed, EnvironmentClassification.PARKLAND,
                CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION);
    }

    private static void assertSameField(GreenSurface actual, GreenSurface expected) {
        assertThat(actual.provenance()).isEqualTo(expected.provenance());
        assertThat(actual.footprint()).isEqualTo(expected.footprint());
        assertThat(actual.columns()).isEqualTo(expected.columns());
        assertThat(actual.rows()).isEqualTo(expected.rows());
        for (int row = 0; row < actual.rows(); row++) for (int column = 0; column < actual.columns(); column++) {
            assertThat(actual.gridElevation(column, row)).isEqualTo(expected.gridElevation(column, row));
        }
    }
}
