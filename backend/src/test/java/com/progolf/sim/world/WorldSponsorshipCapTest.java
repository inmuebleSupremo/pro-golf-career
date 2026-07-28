package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** sponsorship: the concurrent-agreement cap refuses a sign once the book is full (reported, not silent). */
class WorldSponsorshipCapTest {

    @Test
    void aFullSponsorshipBookRefusesFurtherOffers() {
        World world = World.create(3L, WorldConfig.defaults());
        String id = world.activeGolferIds().get(0); // the strongest golfer builds reputation → plenty of offers
        world.assignPlayer(id);
        int cap = world.maxConcurrentSponsorships();

        // Greedily accept offers each season; a full book eventually refuses one (returns false).
        boolean sawRefusal = false;
        for (int s = 0; s < 30 && !sawRefusal; s++) {
            world.advanceSeason();
            while (!world.pendingSponsorships().isEmpty()) {
                if (!world.acceptSponsorship(0)) {
                    sawRefusal = true;
                    assertThat(world.activeSponsorships().size())
                            .as("a sign is refused only when the book is at the cap").isGreaterThanOrEqualTo(cap);
                    break;
                }
            }
        }
        assertThat(sawRefusal).as("a full book refuses a further offer").isTrue();
        assertThat(world.activeSponsorships().size()).isLessThanOrEqualTo(cap);
    }

    @Test
    void anAcceptedSponsorshipShowsAsActiveWithSeasonsRemaining() {
        World world = World.create(5L, WorldConfig.defaults());
        String id = world.activeGolferIds().get(0);
        world.assignPlayer(id);
        world.advanceSeason();
        assertThat(world.pendingSponsorships()).isNotEmpty();

        var offer = world.pendingSponsorships().get(0).agreement();
        assertThat(world.acceptSponsorship(0)).isTrue();
        assertThat(world.activeSponsorships())
                .anySatisfy(a -> {
                    assertThat(a.sponsor()).isEqualTo(offer.sponsor());
                    assertThat(a.isActiveIn(world.currentSeason())).isTrue();
                });
    }
}
