package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Attribute;
import org.junit.jupiter.api.Test;

/** World-progression (modified): the seasonal transition evolves active golfers' attributes, reproducibly. */
class WorldProgressionEvolutionTest {

    private static WorldConfig small() {
        return new WorldConfig(40, 6, 3, 20, 4);
    }

    @Test
    void runningSeasonsEvolvesActiveGolfersAttributes() {
        World world = World.create(1111L, small());
        String id = world.activeGolferIds().get(0);
        world.advanceSeason();
        world.advanceSeason();
        // The seasonal transition applied development + aging, recorded as attribute changes.
        assertThat(world.careerOf(id).player().attributeChanges()).isNotEmpty();
    }

    @Test
    void progressionKeepsTheWorldReproducible() {
        World a = World.create(2222L, small());
        World b = World.create(2222L, small());
        a.advanceSeason();
        a.advanceSeason();
        b.advanceSeason();
        b.advanceSeason();

        String id = a.activeGolferIds().get(0);
        assertThat(b.activeGolferIds()).contains(id);
        for (Attribute attr : Attribute.values()) {
            assertThat(a.careerOf(id).player().attributes().get(attr))
                    .isEqualTo(b.careerOf(id).player().attributes().get(attr));
        }
    }
}
