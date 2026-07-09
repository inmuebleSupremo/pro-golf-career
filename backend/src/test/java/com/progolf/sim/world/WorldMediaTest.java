package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.media.CareerNarrative;
import com.progolf.sim.media.NewsEvent;
import com.progolf.sim.media.NewsType;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** world-progression (modified): the World publishes diverse news from real events; reproducible feed. */
class WorldMediaTest {

    private static WorldConfig small() {
        return new WorldConfig(40, 6, 3, 20, 4);
    }

    @Test
    void resolvingEventsPublishesNewsFromRealOutcomes() {
        World world = World.create(11L, small());
        world.advanceSeason();

        List<NewsEvent> feed = world.newsFeed();
        assertThat(feed).isNotEmpty();
        // Victories are reported, and every victory names a real golfer still in the world after one season.
        Set<String> active = Set.copyOf(world.activeGolferIds());
        List<NewsEvent> victories = feed.stream()
                .filter(e -> e.type() == NewsType.TOURNAMENT_VICTORY).toList();
        assertThat(victories).isNotEmpty();
        assertThat(victories).allSatisfy(e -> {
            assertThat(e.subjectGolferId()).isPresent();
            assertThat(active).contains(e.subjectGolferId().orElseThrow());
        });
    }

    @Test
    void theFeedRecognisesDiverseEventTypesOverTime() {
        World world = World.create(22L, small());
        world.advanceSeason();
        world.advanceSeason();
        world.advanceSeason();

        Set<NewsType> types = world.newsFeed().stream().map(NewsEvent::type).collect(Collectors.toSet());
        // Beyond wins: number-one changes, promotions, retirements, etc. should appear across seasons.
        assertThat(types).contains(NewsType.TOURNAMENT_VICTORY);
        assertThat(types.size()).isGreaterThanOrEqualTo(3);
        assertThat(world.significantNews()).allSatisfy(e -> assertThat(e.isSignificant()).isTrue());
    }

    @Test
    void careerNarrativesAreDerivable() {
        World world = World.create(33L, small());
        world.advanceSeason();
        world.advanceSeason();
        for (String id : world.activeGolferIds()) {
            assertThat(world.careerNarrativeOf(id)).isInstanceOf(CareerNarrative.class);
        }
    }

    @Test
    void theNewsFeedIsReproducibleFromTheSeed() {
        World a = World.create(44L, small());
        World b = World.create(44L, small());
        a.advanceSeason();
        a.advanceSeason();
        b.advanceSeason();
        b.advanceSeason();

        assertThat(a.newsFeed()).isEqualTo(b.newsFeed());
    }
}
