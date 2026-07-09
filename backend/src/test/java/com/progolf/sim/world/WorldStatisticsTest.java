package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.statistics.Championship;
import com.progolf.sim.statistics.RecordType;
import java.util.List;
import org.junit.jupiter.api.Test;

/** world-progression (modified): the World builds the statistics archive from real events; reproducible. */
class WorldStatisticsTest {

    private static WorldConfig small() {
        return new WorldConfig(40, 6, 3, 20, 4);
    }

    @Test
    void resolvingEventsPopulatesStatisticsChampionsAndRecords() {
        World world = World.create(11L, small());
        world.advanceSeason();

        // Someone has accumulated competitive statistics.
        boolean anyStats = world.activeGolferIds().stream()
                .map(world::careerStatisticsOf)
                .anyMatch(s -> s.events() > 0);
        assertThat(anyStats).isTrue();

        // Every event produced a champion; each references a real golfer.
        List<Championship> champions = world.championsOfSeason(1);
        assertThat(champions).isNotEmpty();
        assertThat(champions).allSatisfy(c -> assertThat(c.winnerId()).isNotBlank());

        // Records emerged from gameplay.
        assertThat(world.records()).containsKey(RecordType.LOWEST_TOURNAMENT_SCORE);
        assertThat(world.records()).containsKey(RecordType.LONGEST_CAREER);
    }

    @Test
    void careerStatisticsAccumulateAndPersistAcrossSeasons() {
        World world = World.create(22L, small());
        String id = world.activeGolferIds().get(0);
        world.advanceSeason();
        int afterOne = world.careerStatisticsOf(id).events();
        world.advanceSeason();
        int afterTwo = world.careerStatisticsOf(id).events();

        // Statistics accumulate over a career and are never lost (REQ-253).
        assertThat(afterTwo).isGreaterThanOrEqualTo(afterOne);
        assertThat(world.seasonStatisticsOf(id, 1)).isNotNull();
    }

    @Test
    void recordProgressionIsPreserved() {
        World world = World.create(33L, small());
        world.advanceSeason();
        world.advanceSeason();
        // The lowest-score record has at least one holder, and its progression is preserved (REQ-255).
        assertThat(world.recordProgression(RecordType.LOWEST_TOURNAMENT_SCORE)).isNotEmpty();
    }

    @Test
    void theArchiveIsReproducibleFromTheSeed() {
        World a = World.create(44L, small());
        World b = World.create(44L, small());
        a.advanceSeason();
        a.advanceSeason();
        b.advanceSeason();
        b.advanceSeason();

        assertThat(a.records()).isEqualTo(b.records());
        assertThat(a.championsOfSeason(1)).isEqualTo(b.championsOfSeason(1));
        assertThat(a.championsOfSeason(2)).isEqualTo(b.championsOfSeason(2));
        List<String> ids = a.activeGolferIds().stream().sorted().toList();
        assertThat(ids.stream().map(a::careerStatisticsOf).toList())
                .isEqualTo(ids.stream().map(b::careerStatisticsOf).toList());
    }
}
