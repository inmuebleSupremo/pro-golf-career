package com.progolf.sim.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.shot.Environment;
import com.progolf.sim.shot.GolferState;
import com.progolf.sim.shot.HoleModel;
import com.progolf.sim.shot.RoundOutcome;
import com.progolf.sim.shot.RoundResolver;
import com.progolf.sim.shot.ShotOutcome;
import com.progolf.sim.shot.SimConstants;
import com.progolf.sim.shot.Strategy;
import com.progolf.sim.spatial.ShotZoneProfile;
import com.progolf.sim.spatial.Surface;
import org.junit.jupiter.api.Test;

/** Course-generation spec: composition, reproducibility, valid zone-band emission, pins, integration. */
class CourseGeneratorTest {

    private static final long WORLD = 0xBEEF_CAFE_1234_5678L;

    private static SeedCoordinate courseCoord(long courseId) {
        return new SeedCoordinate(WORLD, 1, courseId, 0, 0, 0, 0);
    }

    private static Course generate(long courseId, EnvironmentClassification classification) {
        return CourseGenerator.generate(courseCoord(courseId), classification);
    }

    @Test
    void courseHasEighteenOrderedHolesAndDerivedParSeventyTwo() {
        Course course = generate(1, EnvironmentClassification.PARKLAND);
        assertThat(course.holes()).hasSize(18);
        for (int i = 0; i < 18; i++) {
            assertThat(course.holes().get(i).number()).isEqualTo(i + 1);
        }
        // 4 par-3s + 10 par-4s + 4 par-5s = 72.
        assertThat(course.totalPar()).isEqualTo(72);
    }

    @Test
    void regionCorrelatesWithClassification() {
        // A course's place must evoke its environment (the Development tour surfaces this region as the event's
        // location, so it can't read "Ayrshire" on a desert course). Prove correlation without pinning the exact
        // pools: the regions a classification produces are disjoint from another's, and stable per classification.
        java.util.Set<String> desert = new java.util.HashSet<>();
        java.util.Set<String> links = new java.util.HashSet<>();
        for (long seed = 0; seed < 60; seed++) {
            desert.add(generate(seed, EnvironmentClassification.DESERT).identity().region());
            links.add(generate(seed, EnvironmentClassification.LINKS).identity().region());
        }
        assertThat(desert).isNotEmpty();
        assertThat(links).isNotEmpty();
        assertThat(desert).doesNotContainAnyElementsOf(links);
    }

    @Test
    void sameSeedAndVersionReproducesIdenticalCourse() {
        Course a = generate(7, EnvironmentClassification.LINKS);
        Course b = generate(7, EnvironmentClassification.LINKS);
        assertThat(a).isEqualTo(b);
        assertThat(a.generatorVersion()).isEqualTo(CourseGenConstants.GENERATOR_VERSION);
    }

    @Test
    void differentSeedsProduceDifferentCourses() {
        assertThat(generate(1, EnvironmentClassification.LINKS))
                .isNotEqualTo(generate(2, EnvironmentClassification.LINKS));
    }

    @Test
    void everyHoleEmitsValidResolvableZoneProfilesAcrossManySeeds() {
        EnvironmentClassification[] classes = EnvironmentClassification.values();
        for (long courseId = 1; courseId <= 20; courseId++) {
            Course course = generate(courseId, classes[(int) (courseId % classes.length)]);
            for (GeneratedHole hole : course.holes()) {
                HoleModel model = hole.forRound(1);
                // Probe a range of remaining distances from tap-in to full length.
                for (double remaining = 3; remaining <= hole.length(); remaining += 25) {
                    ShotZoneProfile profile = model.zoneProfileFor(remaining);
                    // Construction already validated the partition; confirm every probe resolves to a surface.
                    for (double carry = 0; carry < profile.maxReach(); carry += 20) {
                        Surface s = profile.surfaceAt(carry, 0.0);
                        assertThat(s).isNotNull();
                    }
                    // A landing on the pin line at the target distance should be on the green.
                    assertThat(profile.surfaceAt(remaining, 0.0)).isEqualTo(Surface.GREEN);
                }
            }
        }
    }

    @Test
    void pinIsFixedWithinARoundAndVariesAcrossRoundsReproducibly() {
        GeneratedHole hole = generate(3, EnvironmentClassification.COASTAL).holes().get(0);
        // Deterministic per round.
        assertThat(hole.pinFor(2)).isEqualTo(hole.pinFor(2));
        // Rounds generally differ.
        assertThat(hole.pinFor(1)).isNotEqualTo(hole.pinFor(3));
        // The round model's start distance reflects the pin depth offset.
        HoleModel r1 = hole.forRound(1);
        assertThat(r1.startDistance()).isEqualTo(Math.max(1.0, hole.length() + hole.pinFor(1).depthOffset()));
    }

    @Test
    void generatedHoleResolvesEndToEndThroughRoundResolver() {
        // Find a hazard-free hole so the integration is not perturbed by stroke-and-distance, then play it.
        Course course = generate(11, EnvironmentClassification.PARKLAND);
        GeneratedHole clean = course.holes().stream()
                .filter(h -> !h.hasWater())
                .findFirst()
                .orElse(course.holes().get(0));

        RoundOutcome round = RoundResolver.resolveHole(
                clean.forRound(1),
                Attributes.uniform(72),
                GolferState.fresh(),
                Environment.calm(),
                Strategy.BALANCED,
                new SeedCoordinate(WORLD, 1, 11, 1, 501, clean.number(), 0));

        ShotOutcome last = round.shots().get(round.shots().size() - 1);
        assertThat(last.distanceRemaining()).isLessThanOrEqualTo(SimConstants.HOLED_THRESHOLD);
        assertThat(round.shots().size()).isLessThan(SimConstants.MAX_SHOTS_PER_HOLE);
    }
}
