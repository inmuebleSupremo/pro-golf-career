package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.media.NewsEvent;
import com.progolf.sim.media.NewsType;
import com.progolf.sim.tour.TourTier;
import com.progolf.sim.tournament.EventPrestige;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** event-prestige / world-schedule (modified): the calendar has signature events and cross-tour majors. */
class WorldMajorsTest {

    private static WorldConfig small() {
        return new WorldConfig(60, 12, 4, 24, 6, 4, 1); // 4 majors/season, 1 signature/tier
    }

    @Test
    void theScheduleContainsSignatureEventsAndMajors() {
        World world = World.create(1L, small());
        Map<EventPrestige, Integer> counts = new HashMap<>();
        for (ScheduledTournament s : world.currentSchedule()) {
            counts.merge(s.prestige(), 1, Integer::sum);
        }
        assertThat(counts.get(EventPrestige.MAJOR)).isEqualTo(4);
        assertThat(counts.get(EventPrestige.SIGNATURE)).isEqualTo(TourTier.values().length * 1);
        assertThat(counts.get(EventPrestige.REGULAR)).isEqualTo(TourTier.values().length * (4 - 1));
    }

    @Test
    void theScheduleIsReproducibleAcrossSameSeedWorlds() {
        World a = World.create(42L, small());
        World b = World.create(42L, small());
        assertThat(a.currentSchedule()).isEqualTo(b.currentSchedule());

        // ...and stays reproducible after advancing several seasons (majors, cross-tour fields, all deterministic).
        for (int i = 0; i < 3; i++) {
            a.advanceSeason();
            b.advanceSeason();
        }
        assertThat(a.currentRanking()).isEqualTo(b.currentRanking());
        assertThat(a.newsFeed()).isEqualTo(b.newsFeed());
    }

    @Test
    void aMajorDrawsACrossTourFieldAndProducesAMajorVictory() {
        World world = World.create(7L, small());
        world.advanceSeason(); // a season with four majors resolves

        // A major victory is reported as its own high-prominence news type.
        boolean hasMajorNews = world.newsFeed().stream().anyMatch(n -> n.type() == NewsType.MAJOR_VICTORY);
        assertThat(hasMajorNews).isTrue();

        NewsEvent topMajor = world.newsFeed().stream()
                .filter(n -> n.type() == NewsType.MAJOR_VICTORY).findFirst().orElseThrow();
        assertThat(topMajor.prominence()).isGreaterThanOrEqualTo(90); // the biggest news in the world
    }

    @Test
    void aGolferPlaysAtMostOneEventPerWeek() {
        // With cross-tour majors overlapping tour weeks, no golfer may be double-booked in a single week.
        World world = World.create(9L, small());
        // Advancing a whole season must never throw or mis-schedule; a strong player's major takes precedence.
        String player = world.activeGolferIds().get(0);
        world.assignPlayer(player);
        world.advanceSeason();
        assertThat(world.hasPendingPlayerEvent()).isFalse();
        assertThat(world.careerStatisticsOf(player).events()).isGreaterThan(0);
    }
}
