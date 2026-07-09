package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** World-progression spec: over many seasons golfers retire and the population is replenished. */
class WorldLongevityTest {

    @Test
    void overManySeasonsGolfersRetireAndThePopulationStaysSufficient() {
        WorldConfig tiny = new WorldConfig(24, 4, 1, 12, 3);
        World world = World.create(555L, tiny);

        // Capture an original golfer; start ages are 16-22, so they retire within ~49 seasons.
        String original = world.activeGolferIds().get(0);

        for (int s = 0; s < 50; s++) {
            world.advanceSeason();
            // Population is replenished one-for-one, so the active size never drops.
            assertThat(world.activePopulationSize()).isEqualTo(24);
        }

        assertThat(world.currentSeason()).isGreaterThan(50);
        // The original golfer has retired (and their career record is still retrievable).
        assertThat(world.careerOf(original).isRetired()).isTrue();
        // A retired golfer no longer holds a tour membership.
        assertThat(world.tourOf(original)).isEmpty();
    }
}
