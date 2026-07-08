package com.progolf.sim.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Deterministic-rng spec: reproducible, order-independent, sibling-isolated seed derivation. */
class SeedsTest {

    private static final long MASTER = 0x1234_5678_9ABC_DEF0L;

    @Test
    void sameCoordinateYieldsSameSeed() {
        SeedCoordinate c = new SeedCoordinate(MASTER, 3, 7, 2, 99, 12, 4);
        assertThat(Seeds.forCoordinate(c)).isEqualTo(Seeds.forCoordinate(c));
    }

    @Test
    void derivationIsOrderIndependent() {
        SeedCoordinate a = new SeedCoordinate(MASTER, 1, 1, 1, 1, 1, 1);
        SeedCoordinate b = new SeedCoordinate(MASTER, 2, 2, 2, 2, 2, 2);
        long a1 = Seeds.forCoordinate(a);
        long b1 = Seeds.forCoordinate(b);
        // Resolve in the opposite order; each depends only on its own coordinate.
        long b2 = Seeds.forCoordinate(b);
        long a2 = Seeds.forCoordinate(a);
        assertThat(a1).isEqualTo(a2);
        assertThat(b1).isEqualTo(b2);
    }

    @Test
    void siblingsDifferingInOneIdentifierGetIndependentSeeds() {
        SeedCoordinate golferA = new SeedCoordinate(MASTER, 1, 1, 1, 10, 5, 1);
        SeedCoordinate golferB = new SeedCoordinate(MASTER, 1, 1, 1, 11, 5, 1);
        assertThat(Seeds.forCoordinate(golferA)).isNotEqualTo(Seeds.forCoordinate(golferB));

        SeedCoordinate shot1 = golferA.withShot(1);
        SeedCoordinate shot2 = golferA.withShot(2);
        assertThat(Seeds.forCoordinate(shot1)).isNotEqualTo(Seeds.forCoordinate(shot2));
    }

    @Test
    void derivationIsStableRegardlessOfPriorGeneratorUse() {
        SeedCoordinate c = new SeedCoordinate(MASTER, 5, 5, 5, 5, 5, 5);
        long expected = Seeds.forCoordinate(c);
        // Exercise generators heavily; a pure function of the coordinate must be unaffected.
        Rng noise = new SplitMix64Rng(1);
        for (int i = 0; i < 10_000; i++) {
            noise.nextLong();
        }
        assertThat(Seeds.forCoordinate(c)).isEqualTo(expected);
    }

    @Test
    void generatorStreamIsReproducibleFromSeed() {
        Rng a = new SplitMix64Rng(777);
        Rng b = new SplitMix64Rng(777);
        for (int i = 0; i < 1000; i++) {
            assertThat(a.nextLong()).isEqualTo(b.nextLong());
        }
    }
}
