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
    void newWorldUsesOnlyCoreCourseFamiliesForBothTours() {
        World world = World.create(SEED, CONFIG);
        assertThat(world.courseGeneratorVersion())
                .isEqualTo(CourseGenConstants.V8_PRINCIPLED_ARCHITECTURE_GENERATOR_VERSION);
        assertThat(world.snapshot().coursePoolPolicy()).isEqualTo(CoursePoolPolicy.CORE_FOUR);
        assertThat(world.currentSchedule()).extracting(ScheduledTournament::tier)
                .contains(TourTier.PRO, TourTier.DEVELOPMENT);
        EnvironmentClassification[] expected = {
                EnvironmentClassification.LINKS, EnvironmentClassification.PARKLAND,
                EnvironmentClassification.DESERT, EnvironmentClassification.WOODLAND
        };
        for (int i = 0; i < CONFIG.coursePoolSize(); i++) {
            assertThat(world.courseAt(i).identity().classification()).isEqualTo(expected[i % expected.length]);
            assertThat(world.courseAt(i)).isEqualTo(CourseGenerator.generate(
                    new SeedCoordinate(SEED, 0, i, 0, 0, 0, 0), expected[i % expected.length],
                    world.courseGeneratorVersion()));
        }
        assertThat(world.currentSchedule()).allSatisfy(event -> assertThat(
                world.courseAt(event.courseIndex()).identity().classification())
                .isIn(expected[0], expected[1], expected[2], expected[3]));
        assertThat(world.currentSchedule().stream().filter(event -> event.tier() == TourTier.DEVELOPMENT))
                .allSatisfy(event -> assertThat(world.courseTypeFor(event))
                        .isIn("LINKS", "PARKLAND", "DESERT", "HEATHLAND"));
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
    }

    private static WorldSnapshot withoutPoolPolicy(WorldSnapshot s) {
        return new WorldSnapshot(s.season(), s.week(), s.nextTournamentId(), s.replenishCounter(), s.previousNumberOne(),
                s.golfers(), s.careers(), s.activeGolfers(), s.accounts(), s.physicalStates(), s.supportTeams(),
                s.equipment(), s.loadouts(), s.tours(), s.ranking(), s.statistics(), s.media(),
                s.hallOfFameInductions(), s.hallOfFameMembers(), s.retirementSeason(), s.archives(),
                s.rankingSnapshots(), s.environmentalHistory(), s.healthHistory(), s.seasonResults(),
                s.schedule(), s.announcedProspects(), s.playerControl(), s.playerPendingOffers(),
                s.playerPendingStaff(), s.playerPendingEquipment(), s.achievedGoals(), s.staffPool(),
                s.playerActiveEquipmentDeal(), s.playerPendingEquipmentDeals(), s.unlockedAchievements(),
                s.majorsWonThisSeason(), s.playerCareerRecords(), s.courseGeneratorVersion(),
                s.defaultPinPlacementVersion(), null);
    }
}
