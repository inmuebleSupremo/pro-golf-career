package com.progolf.sim.tournament;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** shot-resolution pressure: the situational-pressure model — round, prestige, and contention shaping. */
class PressureModelTest {

    @Test
    void openingRoundsCarryNoPressure() {
        assertThat(PressureModel.forRound(1, EventPrestige.MAJOR, 0)).isZero();
        assertThat(PressureModel.forRound(2, EventPrestige.MAJOR, 0)).isZero();
    }

    @Test
    void pressureBuildsTowardTheFinalRound() {
        double movingDay = PressureModel.forRound(3, EventPrestige.MAJOR, 0);
        double sunday = PressureModel.forRound(4, EventPrestige.MAJOR, 0);
        assertThat(movingDay).isGreaterThan(0.0);
        assertThat(sunday).isGreaterThan(movingDay);
    }

    @Test
    void higherPrestigeMeansMorePressure() {
        double regular = PressureModel.forRound(4, EventPrestige.REGULAR, 0);
        double signature = PressureModel.forRound(4, EventPrestige.SIGNATURE, 0);
        double major = PressureModel.forRound(4, EventPrestige.MAJOR, 0);
        assertThat(major).isGreaterThan(signature);
        assertThat(signature).isGreaterThan(regular);
    }

    @Test
    void pressureWeighsOnlyOnThoseInContention() {
        double leader = PressureModel.forRound(4, EventPrestige.MAJOR, 0);
        double chasing = PressureModel.forRound(4, EventPrestige.MAJOR, 4);
        double outOfIt = PressureModel.forRound(4, EventPrestige.MAJOR, TournamentConstants.PRESSURE_CONTENTION_STROKES);
        double wayBack = PressureModel.forRound(4, EventPrestige.MAJOR, 40);
        assertThat(leader).isGreaterThan(chasing);
        assertThat(chasing).isGreaterThan(0.0);
        assertThat(outOfIt).isZero();          // beyond the contention window
        assertThat(wayBack).isZero();          // never negative
    }

    @Test
    void pressureIsAlwaysWithinUnitRange() {
        for (int round = 1; round <= 4; round++) {
            for (EventPrestige p : EventPrestige.values()) {
                for (int behind = -5; behind <= 30; behind++) {
                    double v = PressureModel.forRound(round, p, behind);
                    assertThat(v).isBetween(0.0, 1.0);
                }
            }
        }
    }
}
