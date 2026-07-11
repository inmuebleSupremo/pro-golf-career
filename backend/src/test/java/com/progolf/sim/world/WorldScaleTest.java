package com.progolf.sim.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.tournament.TournamentResult;
import org.junit.jupiter.api.Test;

/** add-world-scale / financial-strategy: a realistic-scale world with a biting cut and a make-cut pay line. */
class WorldScaleTest {

    private static WorldConfig small() {
        return new WorldConfig(40, 6, 3, 20, 4);
    }

    @Test
    void theCutBitesAndMissingItEarnsNothing() {
        World world = World.create(11L, small());
        world.advanceSeason();

        // Find an event whose cut actually eliminated part of the field (impossible before this change).
        TournamentResult result = world.archives().get(0).results().stream()
                .filter(r -> r.finishingOrder().stream().anyMatch(f -> !f.madeCut()))
                .findFirst().orElseThrow();

        // Every missed-cut finisher earned nothing...
        assertThat(result.finishingOrder().stream().filter(f -> !f.madeCut()))
                .allMatch(f -> f.prize() == 0.0);
        // ...while a made-cut finisher near the top earned prize money (the pay line is the cut).
        assertThat(result.finishingOrder()).anyMatch(f -> f.madeCut() && f.prize() > 0.0);
    }

    @Test
    void theDefaultWorldRunsAtRealisticScale() {
        World world = World.create(7L); // defaults: population 640, fields up to 120
        assertThat(world.activePopulationSize()).isEqualTo(640);

        world.advanceSeason();
        int maxFinishers = world.archives().get(0).results().stream()
                .mapToInt(r -> r.finishingOrder().size()).max().orElse(0);
        assertThat(maxFinishers).isGreaterThan(40);          // far bigger than the old 40-cap
        assertThat(maxFinishers).isLessThanOrEqualTo(120);   // ...up to the new field size
    }

    @Test
    void theDefaultWorldIsReproducible() {
        World a = World.create(9L);
        World b = World.create(9L);
        a.advanceSeason();
        b.advanceSeason();
        assertThat(a.currentRanking()).isEqualTo(b.currentRanking());
    }
}
