package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.statistics.StatLine;
import org.junit.jupiter.api.Test;

/** competitive-statistics: the world accumulates shot-level stats for the field, including the player. */
class WorldShotStatsTest {

    private static WorldConfig small() {
        return new WorldConfig(40, 6, 3, 20, 4);
    }

    @Test
    void careerStatsAccumulateSensibleShotRates() {
        World world = World.create(21L, small());
        world.advanceSeason();
        world.advanceSeason();

        // At least one golfer has played holes and their rates are within sensible bounds.
        boolean anyWithStats = false;
        for (String id : world.activeGolferIds()) {
            StatLine s = world.careerStatisticsOf(id);
            if (s.holesPlayed() > 0) {
                anyWithStats = true;
                assertThat(s.drivingAccuracy()).isBetween(0.0, 1.0);
                assertThat(s.greensInRegulationRate()).isBetween(0.0, 1.0);
                assertThat(s.fairwaysPossible()).isGreaterThan(0);
                assertThat(s.putts()).isGreaterThan(0);
                // With the putting make-% model in place, putts-per-round reads realistically (~28-34);
                // assert a sane band rather than an exact value (it varies by golfer and conditions).
                assertThat(s.puttsPerRound()).isBetween(20.0, 40.0);
            }
        }
        assertThat(anyWithStats).as("some golfer accumulated shot statistics").isTrue();
    }

    @Test
    void theCreatedPlayerAccumulatesTheirOwnShotStats() {
        World world = World.create(22L, small());
        String id = world.createPlayer("Ana", "Rivera", Nationality.ESP, 20, Archetype.ALL_ROUNDER);

        // Play (sim) the player's events so their own rounds' stats are captured.
        world.advanceSeason();
        while (world.hasPendingPlayerEvent()) {
            world.playerEvent().simEvent();
            world.completePlayerEvent();
        }

        StatLine s = world.careerStatisticsOf(id);
        assertThat(s.holesPlayed()).isGreaterThan(0); // the player's shot stats were recorded
        assertThat(s.putts()).isGreaterThan(0);
    }

    @Test
    void shotStatsAreReproducible() {
        World a = World.create(23L, small());
        World b = World.create(23L, small());
        a.advanceSeason();
        b.advanceSeason();
        String id = a.activeGolferIds().get(0);
        assertThat(a.careerStatisticsOf(id)).isEqualTo(b.careerStatisticsOf(id));
    }
}
