package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.tournament.EventPrestige;
import java.util.List;
import org.junit.jupiter.api.Test;

/** player-control (modified): event-by-event entry — entered by default, the player skips to rest. */
class WorldPlayerSchedulingTest {

    private static WorldConfig small() {
        return new WorldConfig(40, 6, 3, 20, 4);
    }

    /** The tournament ids on the player's eligible schedule, in order. */
    private static List<Long> eligibleIds(World world) {
        return world.playerSchedule().stream().map(PlayerScheduleEntry::tournamentId).toList();
    }

    @Test
    void anAssignedPlayerIsEnteredInEligibleEventsByDefault() {
        World world = World.create(1L, small());
        String id = world.activeGolferIds().get(0);
        world.assignPlayer(id);

        // The player has an eligible schedule and is entered in all of it by default.
        assertThat(world.playerSchedule()).isNotEmpty();
        assertThat(world.playerSchedule()).allMatch(PlayerScheduleEntry::entered);

        // Advancing a season, the player competes (default-enter preserved).
        world.advanceSeason();
        while (world.hasPendingPlayerEvent()) { // sim any events the player is entered in
            world.playerEvent().simEvent();
            world.completePlayerEvent();
        }
        assertThat(world.careerStatisticsOf(id).events()).isGreaterThan(0);
    }

    @Test
    void skippingAnEventExcludesThePlayerFromIt() {
        World world = World.create(2L, small());
        String id = world.activeGolferIds().get(0); // the strongest golfer — always qualifies for a major
        world.assignPlayer(id);

        // Skip a major (majors always resolve with a full cross-tour field, so this is deterministic).
        long skipped = world.playerSchedule().stream()
                .filter(e -> e.prestige() == EventPrestige.MAJOR)
                .map(PlayerScheduleEntry::tournamentId)
                .findFirst().orElseThrow();
        world.skipEvent(skipped);
        assertThat(entryOf(world, skipped)).isFalse();

        // Advance the season, simming every event the player DID enter (they never pause on the skipped one).
        int guard = 0;
        while (world.currentSeason() == 1 && guard++ < 100) {
            world.advanceWeek();
            while (world.hasPendingPlayerEvent()) { // only entered events ever pause here
                world.playerEvent().simEvent();
                world.completePlayerEvent();
            }
        }

        // The skipped major resolved automatically, without the player among its finishers...
        SeasonArchive season1 = world.archives().get(0);
        var skippedResult = season1.results().stream()
                .filter(r -> r.tournamentId() == skipped).findFirst().orElseThrow();
        assertThat(skippedResult.finishingOrder()).noneMatch(f -> f.golfer().player().id().equals(id));
        // ...while the player still played their other entered events.
        assertThat(world.careerStatisticsOf(id).events()).isGreaterThan(0);
    }

    @Test
    void reEnteringRestoresDefaultEntry() {
        World world = World.create(3L, small());
        String id = world.activeGolferIds().get(0);
        world.assignPlayer(id);
        long event = eligibleIds(world).get(0);

        world.skipEvent(event);
        assertThat(entryOf(world, event)).isFalse();
        world.enterEvent(event);
        assertThat(entryOf(world, event)).isTrue();
    }

    @Test
    void restingIsStillABlanketSitOut() {
        World world = World.create(4L, small());
        String id = world.activeGolferIds().get(0);
        world.assignPlayer(id);
        world.setResting(true);

        // Every eligible event now reads entered=false.
        assertThat(world.playerSchedule()).noneMatch(PlayerScheduleEntry::entered);
        world.advanceSeason();
        assertThat(world.careerStatisticsOf(id).events()).isZero(); // sat out everything
    }

    private static boolean entryOf(World world, long tournamentId) {
        return world.playerSchedule().stream()
                .filter(e -> e.tournamentId() == tournamentId).findFirst().orElseThrow().entered();
    }
}
