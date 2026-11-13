package com.progolf.sim.tournament;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeasonProfile;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.course.Course;
import com.progolf.sim.course.CourseGenConstants;
import com.progolf.sim.course.CourseGenerator;
import com.progolf.sim.course.CourseSetup;
import com.progolf.sim.course.EnvironmentClassification;
import com.progolf.sim.course.PinPlacementVersion;
import com.progolf.sim.putt.PuttingPolicy;
import com.progolf.sim.shot.Environment;
import com.progolf.sim.shot.GolferState;
import com.progolf.sim.shot.HoleModel;
import com.progolf.sim.shot.RoundResolver;
import com.progolf.sim.shot.Strategy;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class TournamentRoundHoleSharingTest {
    private static final long WORLD = 0x524F554E44484F4CL;

    @Test
    void sharesModelsAndGeneratesOnlyRequestedPhysicalGreens() {
        Course course = course(CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION,
                EnvironmentClassification.PARKLAND);
        Tournament tournament = tournament(course, PuttingPolicy.PHYSICAL_V1);
        Tournament other = tournament(course, PuttingPolicy.PHYSICAL_V1);

        try (SeasonProfile profile = SeasonProfile.start()) {
            HoleModel firstGolfer = tournament.holeModel(7, 1);
            HoleModel secondGolfer = tournament.holeModel(7, 1);
            assertThat(secondGolfer).isSameAs(firstGolfer);
            assertThat(profile.summary("TEST", 0)).contains("holeModels=1", "greenSurfaces=0");

            var green = firstGolfer.greenSurface();
            assertThat(secondGolfer.greenSurface()).isSameAs(green);
            assertThat(secondGolfer.greenSurface()).isSameAs(green);
            assertThat(profile.summary("TEST", 0)).contains("holeModels=1", "greenSurfaces=1");

            HoleModel nextRound = tournament.holeModel(7, 2);
            assertThat(nextRound).isNotSameAs(firstGolfer);
            assertThat(tournament.holeModel(8, 1)).isNotSameAs(firstGolfer);
            assertThat(other.holeModel(7, 1)).isNotSameAs(firstGolfer);
            assertThat(profile.summary("TEST", 0)).contains("holeModels=4", "greenSurfaces=1");

            assertThat(tournament.holeModel(7, 91)).isNotSameAs(firstGolfer);
            assertThat(tournament.holeModel(7, 91)).isSameAs(tournament.holeModel(7, 91));
            assertThat(profile.summary("TEST", 0)).contains("holeModels=5", "greenSurfaces=1");
        }
        assertThat(SeasonProfile.current()).isNull();
    }

    @Test
    void legacyModelsNeverGeneratePhysicalGreens() {
        Tournament tournament = tournament(course(CourseGenConstants.V9_COASTAL_LINKS_GENERATOR_VERSION,
                EnvironmentClassification.LINKS), PuttingPolicy.LEGACY);
        try (SeasonProfile profile = SeasonProfile.start()) {
            assertThat(tournament.holeModel(1, 1).greenSurface()).isNull();
            assertThat(tournament.holeModel(1, 1)).isSameAs(tournament.holeModel(1, 1));
            assertThat(profile.summary("TEST", 0)).contains("holeModels=1", "greenSurfaces=0");
        }
    }

    @Test
    void sharedModelsMatchFreshResolutionAcrossSupportedGenerators() {
        int[] versions = {
                CourseGenConstants.V5_REFINED_GENERATOR_VERSION,
                CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION,
                CourseGenConstants.V9_COASTAL_LINKS_GENERATOR_VERSION
        };
        EnvironmentClassification[] families = {
                EnvironmentClassification.HEATHLAND,
                EnvironmentClassification.PARKLAND,
                EnvironmentClassification.LINKS
        };
        CourseSetup setup = new CourseSetup(1.2, 1.1, 0.85);
        for (int i = 0; i < versions.length; i++) {
            Course course = course(versions[i], families[i]);
            Tournament tournament = tournament(course, PuttingPolicy.PHYSICAL_V1, setup);
            HoleModel shared = tournament.holeModel(7, 2);
            HoleModel fresh = course.holeModel(7, 2, setup, PinPlacementVersion.V5_EFFECTIVE_GREEN,
                    PuttingPolicy.PHYSICAL_V1);
            assertThat(shared.geometry()).isEqualTo(fresh.geometry());
            assertThat(shared.cupPosition()).isEqualTo(fresh.cupPosition());
            assertThat(shared.greenSurface().provenance()).isEqualTo(fresh.greenSurface().provenance());
            for (int golfer : new int[] {3, 4}) {
                SeedCoordinate shot = new SeedCoordinate(WORLD, 1, 7, 2, golfer, 7, 0);
                var expected = RoundResolver.resolveHole(fresh, Attributes.uniform(80), GolferState.fresh(),
                        Environment.calm(), Strategy.BALANCED, shot);
                var actual = RoundResolver.resolveHole(shared, Attributes.uniform(80), GolferState.fresh(),
                        Environment.calm(), Strategy.BALANCED, shot);
                assertThat(actual).isEqualTo(expected);
                assertThat(actual.shots()).anyMatch(outcome -> outcome.physicalPutt() != null);
            }
        }
    }

    @Test
    void concurrentRequestsReturnOneModelAndOneSurface() throws Exception {
        Tournament tournament = tournament(course(CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION,
                EnvironmentClassification.WOODLAND), PuttingPolicy.PHYSICAL_V1);
        try (var executor = Executors.newFixedThreadPool(8)) {
            List<Future<HoleModel>> requests = java.util.stream.IntStream.range(0, 8)
                    .mapToObj(ignored -> executor.submit(() -> {
                        HoleModel model = tournament.holeModel(7, 1);
                        model.greenSurface();
                        return model;
                    })).toList();
            HoleModel first = requests.getFirst().get();
            for (Future<HoleModel> request : requests) {
                assertThat(request.get()).isSameAs(first);
                assertThat(request.get().greenSurface()).isSameAs(first.greenSurface());
            }
        }
    }

    private static Course course(int version, EnvironmentClassification family) {
        return CourseGenerator.generate(new SeedCoordinate(WORLD, 1, 1, 0, 0, 0, 0), family, version);
    }

    private static Tournament tournament(Course course, PuttingPolicy policy) {
        return tournament(course, policy, CourseSetup.standard());
    }

    private static Tournament tournament(Course course, PuttingPolicy policy, CourseSetup setup) {
        TournamentDefinition definition = new TournamentDefinition("Shared Hole Test", course, Tier.STANDARD,
                EventPrestige.REGULAR, new EntryRequirements(2, true), PrizeStructure.standard(),
                TournamentFormat.standard(), LocalDate.of(2026, 6, 1), WORLD, 1, 7,
                PinPlacementVersion.V5_EFFECTIVE_GREEN, policy);
        return new Tournament(definition, com.progolf.sim.weather.TournamentWeather.calm(), setup);
    }
}
