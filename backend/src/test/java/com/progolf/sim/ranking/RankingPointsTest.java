package com.progolf.sim.ranking;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.tournament.Tier;
import org.junit.jupiter.api.Test;

/** World-ranking spec: the points model is monotone in position, tier, and field strength, and decays. */
class RankingPointsTest {

    @Test
    void positionWeightDecreasesAndZeroesOutBeyondPayingPositions() {
        assertThat(RankingPoints.positionWeight(1)).isGreaterThan(RankingPoints.positionWeight(2));
        assertThat(RankingPoints.positionWeight(2)).isGreaterThan(RankingPoints.positionWeight(10));
        assertThat(RankingPoints.positionWeight(RankingConstants.POINT_PAYING_POSITIONS + 1)).isEqualTo(0.0);
    }

    @Test
    void betterFinishHigherTierAndStrongerFieldAwardMore() {
        assertThat(RankingPoints.award(Tier.STANDARD, 1, 1.0))
                .isGreaterThan(RankingPoints.award(Tier.STANDARD, 5, 1.0));
        assertThat(RankingPoints.award(Tier.MAJOR, 1, 1.0))
                .isGreaterThan(RankingPoints.award(Tier.STANDARD, 1, 1.0));
        assertThat(RankingPoints.award(Tier.STANDARD, 1, 2.0))
                .isGreaterThan(RankingPoints.award(Tier.STANDARD, 1, 1.0));
    }

    @Test
    void fieldStrengthFactorIsMonotoneAndClamped() {
        assertThat(RankingPoints.fieldStrengthFactor(10)).isLessThan(RankingPoints.fieldStrengthFactor(40));
        assertThat(RankingPoints.fieldStrengthFactor(0)).isEqualTo(RankingConstants.MIN_FIELD_FACTOR);
        assertThat(RankingPoints.fieldStrengthFactor(1_000_000)).isEqualTo(RankingConstants.MAX_FIELD_FACTOR);
    }

    @Test
    void decayIsFullThenDeclinesThenExpires() {
        assertThat(RankingPoints.decay(0)).isEqualTo(1.0);
        assertThat(RankingPoints.decay(RankingConstants.FULL_VALUE_DAYS)).isEqualTo(1.0);
        assertThat(RankingPoints.decay(RankingConstants.WINDOW_DAYS)).isEqualTo(0.0);
        assertThat(RankingPoints.decay(RankingConstants.WINDOW_DAYS + 100)).isEqualTo(0.0);
        double mid = RankingPoints.decay((RankingConstants.FULL_VALUE_DAYS + RankingConstants.WINDOW_DAYS) / 2);
        assertThat(mid).isBetween(0.0, 1.0);
        // Monotonic decline through the window.
        assertThat(RankingPoints.decay(200)).isGreaterThan(RankingPoints.decay(500));
    }
}
