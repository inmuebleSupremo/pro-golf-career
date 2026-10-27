package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.course.CourseGenConstants;
import com.progolf.sim.course.CourseGenerator;
import com.progolf.sim.course.EnvironmentClassification;
import com.progolf.sim.tour.TourTier;
import org.junit.jupiter.api.Test;

class CoursePoolPolicyTest {
    private static final long SEED = 2409L;
    private static final WorldConfig CONFIG = new WorldConfig(8, 4, 1, 4, 8);

    @Test
    void newWorldUsesFiveIdentitiesAcrossFourFamiliesForBothTours() {
        World world = World.create(SEED, CONFIG);
        assertThat(world.courseGeneratorVersion())
                .isEqualTo(CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION);
        assertThat(world.snapshot().coursePoolPolicy()).isEqualTo(CoursePoolPolicy.FOUR_FAMILIES);
        assertThat(world.currentSchedule()).extracting(ScheduledTournament::tier)
                .contains(TourTier.PRO, TourTier.DEVELOPMENT);
        EnvironmentClassification[] expected = {
                EnvironmentClassification.LINKS, EnvironmentClassification.PARKLAND,
                EnvironmentClassification.DESERT, EnvironmentClassification.WOODLAND,
                EnvironmentClassification.HEATHLAND
        };
        for (int i = 0; i < CONFIG.coursePoolSize(); i++) {
            assertThat(world.courseAt(i).identity().classification()).isEqualTo(expected[i % expected.length]);
            assertThat(world.courseAt(i)).isEqualTo(CourseGenerator.generate(
                    new SeedCoordinate(SEED, 0, i, 0, 0, 0, 0), expected[i % expected.length],
                    world.courseGeneratorVersion()));
        }
        assertThat(world.currentSchedule()).allSatisfy(event -> assertThat(
                world.courseAt(event.courseIndex()).identity().classification())
                .isIn(expected[0], expected[1], expected[2], expected[3], expected[4]));
        assertThat(world.currentSchedule().stream().filter(event -> event.tier() == TourTier.DEVELOPMENT))
                .allSatisfy(event -> assertThat(world.courseTypeFor(event))
                        .isIn("LINKS", "PARKLAND", "DESERT", "WOODLAND", "HEATHLAND"));
        assertThat(world.courseFamilyFor(EnvironmentClassification.WOODLAND)).isEqualTo("PARKLAND");
        assertThat(world.courseFamilyFor(EnvironmentClassification.HEATHLAND)).isEqualTo("HEATHLAND");
        assertThat(World.create(SEED, CONFIG).currentSchedule()).isEqualTo(world.currentSchedule());
    }

    @Test
    void missingPolicyRestoresHistoricalSixCourseCycleAndKeepsItOnResave() {
        World fresh = World.create(SEED, CONFIG);
        World legacy = World.restore(SEED, CONFIG, withoutPoolPolicy(fresh.snapshot()));
        assertThat(legacy.snapshot().coursePoolPolicy()).isEqualTo(CoursePoolPolicy.LEGACY_SIX);
        for (int i = 0; i < CONFIG.coursePoolSize(); i++) {
            EnvironmentClassification classification = EnvironmentClassification.values()[i % 6];
            assertThat(legacy.courseAt(i)).isEqualTo(CourseGenerator.generate(
                    new SeedCoordinate(SEED, 0, i, 0, 0, 0, 0), classification,
                    legacy.courseGeneratorVersion()));
        }
        assertThat(legacy.courseAt(3).identity().classification()).isEqualTo(EnvironmentClassification.MOUNTAIN);
        assertThat(legacy.courseAt(4).identity().classification()).isEqualTo(EnvironmentClassification.COASTAL);
        assertThat(World.restore(SEED, CONFIG, legacy.snapshot()).snapshot()).isEqualTo(legacy.snapshot());
        assertThat(legacy.courseFamilyFor(EnvironmentClassification.WOODLAND)).isEqualTo("HEATHLAND");
    }

    @Test
    void previousCoreFourSnapshotKeepsWoodlandAsHeathland() {
        World fresh = World.create(SEED, CONFIG);
        World previous = World.restore(SEED, CONFIG, withPoolPolicy(fresh.snapshot(), CoursePoolPolicy.CORE_FOUR));
        assertThat(previous.courseAt(3).identity().classification()).isEqualTo(EnvironmentClassification.WOODLAND);
        assertThat(previous.courseAt(4).identity().classification()).isEqualTo(EnvironmentClassification.LINKS);
        assertThat(previous.courseFamilyFor(EnvironmentClassification.WOODLAND)).isEqualTo("HEATHLAND");
        assertThat(previous.snapshot().coursePoolPolicy()).isEqualTo(CoursePoolPolicy.CORE_FOUR);
        assertThat(World.restore(SEED, CONFIG, previous.snapshot()).courseAt(3)).isEqualTo(previous.courseAt(3));
    }

    @Test
    void heathlandReusesWoodlandArchitectureWithoutChangingHistoricalWoodland() {
        SeedCoordinate coordinate = new SeedCoordinate(SEED, 0, 4, 0, 0, 0, 0);
        var woodland = CourseGenerator.generate(coordinate, EnvironmentClassification.WOODLAND,
                CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION);
        var heathland = CourseGenerator.generate(coordinate, EnvironmentClassification.HEATHLAND,
                CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION);
        assertThat(heathland.holes()).isEqualTo(woodland.holes());
        assertThat(heathland.identity().classification()).isEqualTo(EnvironmentClassification.HEATHLAND);
        assertThat(heathland.identity().region()).isNotEqualTo(woodland.identity().region());
        assertThat(CourseGenerator.generate(coordinate, EnvironmentClassification.HEATHLAND,
                CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION)).isEqualTo(heathland);
    }

    private static WorldSnapshot withoutPoolPolicy(WorldSnapshot s) {
        return withPoolPolicy(s, null);
    }

    private static WorldSnapshot withPoolPolicy(WorldSnapshot s, CoursePoolPolicy policy) {
        return new WorldSnapshot(s.season(), s.week(), s.nextTournamentId(), s.replenishCounter(), s.previousNumberOne(),
                s.golfers(), s.careers(), s.activeGolfers(), s.accounts(), s.physicalStates(), s.supportTeams(),
                s.equipment(), s.loadouts(), s.tours(), s.ranking(), s.statistics(), s.media(),
                s.hallOfFameInductions(), s.hallOfFameMembers(), s.retirementSeason(), s.archives(),
                s.rankingSnapshots(), s.environmentalHistory(), s.healthHistory(), s.seasonResults(),
                s.schedule(), s.announcedProspects(), s.playerControl(), s.playerPendingOffers(),
                s.playerPendingStaff(), s.playerPendingEquipment(), s.achievedGoals(), s.staffPool(),
                s.playerActiveEquipmentDeal(), s.playerPendingEquipmentDeals(), s.unlockedAchievements(),
                s.majorsWonThisSeason(), s.playerCareerRecords(), s.courseGeneratorVersion(),
                s.defaultPinPlacementVersion(), policy);
    }
}
