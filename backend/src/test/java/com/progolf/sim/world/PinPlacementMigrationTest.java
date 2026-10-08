package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.sim.course.PinPlacementVersion;
import org.junit.jupiter.api.Test;

/** Future-only V5 adoption never rewrites archived provenance or consumes simulation entropy. */
class PinPlacementMigrationTest {

    private static final WorldConfig SMALL = new WorldConfig(40, 6, 3, 20, 4);

    @Test
    void newCareersPinV5ToTheirSchedule() {
        World world = World.create(15L, SMALL);
        assertThat(world.defaultPinPlacementVersion()).isEqualTo(PinPlacementVersion.V5_EFFECTIVE_GREEN);
        assertThat(world.snapshot().schedule()).allSatisfy(event ->
                assertThat(event.pinPlacementVersion()).isEqualTo(PinPlacementVersion.V5_EFFECTIVE_GREEN));
    }

    @Test
    void legacyCareerAdoptsOnlyItsCurrentAndFutureScheduleIdempotently() {
        World world = World.restore(16L, SMALL, legacySnapshot(World.create(16L, SMALL).snapshot()));
        assertThat(world.defaultPinPlacementVersion()).isEqualTo(PinPlacementVersion.LEGACY_V1);
        assertThat(world.pinPlacementMigrationStatus().legacyScheduledEvents()).isPositive();

        world.advanceSeason();
        var archivedBefore = world.snapshot().archives();
        assertThat(archivedBefore).isNotEmpty();
        assertThat(archivedBefore.getFirst().schedule()).allSatisfy(event ->
                assertThat(event.pinPlacementVersion()).isEqualTo(PinPlacementVersion.LEGACY_V1));

        world.adoptV5PinPlacementForFutureEvents();
        WorldSnapshot migrated = world.snapshot();
        assertThat(migrated.defaultPinPlacementVersion()).isEqualTo(PinPlacementVersion.V5_EFFECTIVE_GREEN);
        assertThat(migrated.schedule()).allSatisfy(event ->
                assertThat(event.pinPlacementVersion()).isEqualTo(PinPlacementVersion.V5_EFFECTIVE_GREEN));
        assertThat(migrated.archives()).isEqualTo(archivedBefore);

        world.adoptV5PinPlacementForFutureEvents();
        assertThat(world.snapshot()).isEqualTo(migrated);
    }

    @Test
    void migrationRejectsAnActivePlayerEvent() {
        World world = World.restore(17L, SMALL, legacySnapshot(World.create(17L, SMALL).snapshot()));
        world.assignPlayer(world.activeGolferIds().getFirst());
        while (!world.hasPendingPlayerEvent()) world.advanceWeek();
        assertThatThrownBy(world::adoptV5PinPlacementForFutureEvents).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pending");
    }

    @Test
    void migrationDoesNotRewriteAlreadyCompletedEntriesInTheCurrentSeasonSchedule() {
        World world = World.restore(18L, SMALL, legacySnapshot(World.create(18L, SMALL).snapshot()));
        world.advanceWeek();
        WorldSnapshot before = world.snapshot();
        assertThat(before.seasonResults()).isNotEmpty();

        world.adoptV5PinPlacementForFutureEvents();
        WorldSnapshot after = world.snapshot();
        for (var result : after.seasonResults()) {
            var event = after.schedule().stream().filter(s -> s.tournamentId() == result.tournamentId()).findFirst().orElseThrow();
            assertThat(event.pinPlacementVersion()).isEqualTo(PinPlacementVersion.LEGACY_V1);
        }
        assertThat(after.schedule().stream().filter(s -> after.seasonResults().stream()
                .noneMatch(result -> result.tournamentId() == s.tournamentId())))
                .allSatisfy(event -> assertThat(event.pinPlacementVersion()).isEqualTo(PinPlacementVersion.V5_EFFECTIVE_GREEN));
    }

    private static WorldSnapshot legacySnapshot(WorldSnapshot s) {
        return new WorldSnapshot(s.season(), s.week(), s.nextTournamentId(), s.replenishCounter(), s.previousNumberOne(),
                s.golfers(), s.careers(), s.activeGolfers(), s.accounts(), s.physicalStates(), s.supportTeams(),
                s.equipment(), s.loadouts(), s.tours(), s.ranking(), s.statistics(), s.media(),
                s.hallOfFameInductions(), s.hallOfFameMembers(), s.retirementSeason(),
                s.archives().stream().map(a -> new WorldSnapshot.ArchiveSnapshot(a.season(),
                        a.schedule().stream().map(PinPlacementMigrationTest::legacy).toList(), a.results())).toList(),
                s.rankingSnapshots(), s.environmentalHistory(), s.healthHistory(), s.seasonResults(),
                s.schedule().stream().map(PinPlacementMigrationTest::legacy).toList(), s.announcedProspects(),
                s.playerControl(), s.playerPendingOffers(), s.playerPendingStaff(), s.playerPendingEquipment(),
                s.achievedGoals(), s.staffPool(), s.playerActiveEquipmentDeal(), s.playerPendingEquipmentDeals(),
                s.unlockedAchievements(), s.majorsWonThisSeason(), s.playerCareerRecords(), s.courseGeneratorVersion(), null);
    }

    private static ScheduledTournament legacy(ScheduledTournament event) {
        return new ScheduledTournament(event.week(), event.tier(), event.courseIndex(), event.prestige(),
                event.tournamentId(), PinPlacementVersion.LEGACY_V1);
    }
}
