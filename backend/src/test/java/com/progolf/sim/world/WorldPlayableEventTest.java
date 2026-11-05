package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.sim.putt.PuttingPolicy;
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
        assertThat(world.playerEvent().puttingPolicy()).isEqualTo(PuttingPolicy.PHYSICAL_V1);
        assertThat(world.playerSchedule()).filteredOn(entry -> entry.week() == world.currentWeek()
                && entry.entered()).anySatisfy(entry ->
                assertThat(entry.puttingPolicy()).isEqualTo(PuttingPolicy.PHYSICAL_V1));

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
        assertThat(world.playerEvent().puttingPolicy()).isEqualTo(PuttingPolicy.PHYSICAL_V1);
        int eventsBefore = world.careerStatisticsOf(id).events();

        world.playerEvent().simEvent();
        world.completePlayerEvent();

        assertThat(world.hasPendingPlayerEvent()).isFalse();
        assertThat(world.careerStatisticsOf(id).events()).isEqualTo(eventsBefore + 1); // the played event counted
        assertThat(world.snapshot().schedule()).filteredOn(event -> event.week() == world.currentWeek() - 1
                && event.puttingPolicy() == PuttingPolicy.PHYSICAL_V1).isNotEmpty();
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
        assertThat(world.snapshot().archives()).anySatisfy(archive ->
                assertThat(archive.schedule()).anySatisfy(event ->
                        assertThat(event.puttingPolicy()).isEqualTo(PuttingPolicy.PHYSICAL_V1)));
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
        assertThat(world.snapshot().schedule()).allSatisfy(event ->
                assertThat(event.puttingPolicy()).isEqualTo(PuttingPolicy.LEGACY));
    }
}
