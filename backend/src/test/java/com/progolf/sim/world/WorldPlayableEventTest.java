package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** playable-event / world-progression (modified): the player's event yields, is played, and counts. */
class WorldPlayableEventTest {

    private static WorldConfig small() {
        return new WorldConfig(40, 6, 3, 20, 4);
    }

    @Test
    void thePlayersEventYieldsAndPausesTheWeek() {
        World world = World.create(10L, small());
        String id = world.activeGolferIds().get(0); // the strongest golfer — an Elite-tour regular
        world.assignPlayer(id);

        int guard = 0;
        while (!world.hasPendingPlayerEvent() && guard++ < 50) {
            world.advanceWeek();
        }
        assertThat(world.hasPendingPlayerEvent()).as("the player's event should come up").isTrue();

        // Paused: the calendar has not advanced and advancing again is rejected until the event completes.
        int weekAtPause = world.currentWeek();
        assertThatThrownBy(world::advanceWeek).isInstanceOf(IllegalStateException.class);
        assertThat(world.currentWeek()).isEqualTo(weekAtPause);
        assertThat(world.careerStatisticsOf(id).events()).isZero(); // not yet fed to consumers
    }

    @Test
    void playingThePlayersEventMakesItCountAndResumesTheWeek() {
        World world = World.create(11L, small());
        String id = world.activeGolferIds().get(0);
        world.assignPlayer(id);

        int guard = 0;
        while (!world.hasPendingPlayerEvent() && guard++ < 50) {
            world.advanceWeek();
        }
        assertThat(world.hasPendingPlayerEvent()).isTrue();
        int eventsBefore = world.careerStatisticsOf(id).events();

        world.playerEvent().simEvent();
        world.completePlayerEvent();

        assertThat(world.hasPendingPlayerEvent()).isFalse();
        assertThat(world.careerStatisticsOf(id).events()).isEqualTo(eventsBefore + 1); // the played event counted
    }

    @Test
    void advanceSeasonSimsThePlayersEventsUnattended() {
        World world = World.create(12L, small());
        String id = world.activeGolferIds().get(0);
        world.assignPlayer(id);

        world.advanceSeason();

        assertThat(world.hasPendingPlayerEvent()).isFalse();
        assertThat(world.currentSeason()).isEqualTo(2);
        assertThat(world.careerStatisticsOf(id).events()).isGreaterThan(0); // played their scheduled events
    }

    @Test
    void aRestingPlayerNeverYields() {
        World world = World.create(13L, small());
        String id = world.activeGolferIds().get(0);
        world.assignPlayer(id);
        world.setResting(true);

        int guard = 0;
        while (guard++ < 6) {
            assertThat(world.hasPendingPlayerEvent()).isFalse();
            world.advanceWeek();
        }
        assertThat(world.careerStatisticsOf(id).events()).isZero();
    }
}
