package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.media.NewsEvent;
import com.progolf.sim.media.NewsType;
import org.junit.jupiter.api.Test;

/** news-generation: upset news is suppressed until the ranking is established (no season-1 false upsets). */
class WorldUpsetNewsTest {

    private static WorldConfig config() {
        return new WorldConfig(120, 12, 4, 54, 4);
    }

    private static long upsets(World world) {
        return world.newsFeed().stream().filter(n -> n.type() == NewsType.MAJOR_UPSET).count();
    }

    @Test
    void theOpeningSeasonHasNoUpsetNews() {
        World world = World.create(2026L, config());
        world.advanceSeason(); // season 1: the ranking is still forming, so every winner reads as "unranked"
        // Without the fix, every season-1 winner would be a false upset; now there are none.
        assertThat(upsets(world)).isZero();
        for (NewsEvent n : world.newsFeed()) {
            assertThat(n.type()).isNotEqualTo(NewsType.MAJOR_UPSET);
        }
    }

    @Test
    void upsetsAppearOnceTheRankingIsEstablished() {
        World world = World.create(2026L, config());
        for (int s = 0; s < 4; s++) {
            world.advanceSeason();
        }
        // From season 2 the ranking is established, so genuine low-ranked winners are reported as upsets,
        // and every upset is attributed to a season after the first.
        assertThat(upsets(world)).isPositive();
        for (NewsEvent n : world.newsFeed()) {
            if (n.type() == NewsType.MAJOR_UPSET) {
                assertThat(n.season()).isGreaterThan(1);
            }
        }
    }
}
