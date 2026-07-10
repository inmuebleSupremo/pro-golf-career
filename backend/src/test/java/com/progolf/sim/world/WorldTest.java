package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.career.Career;
import com.progolf.sim.tour.TourTier;
import org.junit.jupiter.api.Test;

/** World-calendar/schedule/progression specs: bootstrap, weekly resolution, transition, reproducibility. */
class WorldTest {

    private static WorldConfig small() {
        return new WorldConfig(40, 6, 3, 20, 4); // pop 40, 6 weeks, 3 events/tier, field 20, 4 courses
    }

    @Test
    void bootstrapDistributesPopulationAndGeneratesASchedule() {
        World world = World.create(1234L, small());
        assertThat(world.currentSeason()).isEqualTo(1);
        assertThat(world.currentWeek()).isEqualTo(1);
        assertThat(world.activePopulationSize()).isEqualTo(40);
        // 4 tiers x 3 events, plus the cross-tour majors (spec: event-prestige).
        assertThat(world.currentSchedule())
                .hasSize(TourTier.values().length * 3 + WorldConstants.MAJORS_PER_SEASON);
        // Golfers exist across at least the development and one higher tier.
        long tiers = world.activeGolferIds().stream().map(world::tourOf).flatMap(java.util.Optional::stream).distinct().count();
        assertThat(tiers).isGreaterThan(1);
    }

    @Test
    void advancingASeasonResolvesEventsAndFeedsRankingStandingsAndCareers() {
        World world = World.create(2345L, small());
        world.advanceSeason();

        // Ranking has been populated from results.
        assertThat(world.currentRanking().size()).isGreaterThan(0);
        // At least one golfer has played events (career updated).
        boolean anyPlayed = world.activeGolferIds().stream()
                .map(world::careerOf)
                .anyMatch(c -> c.statistics().eventsPlayed() > 0);
        assertThat(anyPlayed).isTrue();
        // A completed season is archived with results, and a season-ending ranking snapshot was taken.
        assertThat(world.archives()).hasSize(1);
        assertThat(world.archives().get(0).results()).isNotEmpty();
        assertThat(world.rankingSnapshots()).hasSize(1);
    }

    @Test
    void seasonalTransitionAgesCareersAndAdvancesTheSeason() {
        World world = World.create(3456L, small());
        String someGolfer = world.activeGolferIds().get(0);
        int ageBefore = world.careerOf(someGolfer).age();

        world.advanceSeason();

        assertThat(world.currentSeason()).isEqualTo(2);
        // Every career advanced exactly one season.
        Career career = world.careerOf(someGolfer);
        // (The golfer is very likely still active — start ages are 16-22, far from retirement.)
        assertThat(career.age()).isEqualTo(ageBefore + 1);
        // A fresh schedule for season 2 exists.
        assertThat(world.currentSchedule()).isNotEmpty();
    }

    @Test
    void worldProgressesWithoutAnyPlayerAction() {
        World world = World.create(4567L, small());
        world.advanceWeek(); // no player input at all
        world.advanceWeek();
        // Events have been resolved into the ranking / careers purely by world progression.
        boolean anyPlayed = world.activeGolferIds().stream()
                .map(world::careerOf).anyMatch(c -> c.statistics().eventsPlayed() > 0);
        assertThat(anyPlayed).isTrue();
    }

    @Test
    void sameSeedReproducesTheWorld() {
        World a = World.create(9999L, small());
        World b = World.create(9999L, small());
        a.advanceSeason();
        a.advanceSeason();
        b.advanceSeason();
        b.advanceSeason();

        assertThat(a.currentSeason()).isEqualTo(b.currentSeason());
        assertThat(a.currentRanking().standings()).isEqualTo(b.currentRanking().standings());
        // Same winners across archived seasons.
        assertThat(winners(a)).isEqualTo(winners(b));
    }

    private static java.util.List<String> winners(World w) {
        java.util.List<String> ids = new java.util.ArrayList<>();
        w.archives().forEach(a -> a.results().forEach(r -> ids.add(r.winner().player().id())));
        return ids;
    }
}
