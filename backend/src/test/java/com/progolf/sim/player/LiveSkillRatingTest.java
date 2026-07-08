package com.progolf.sim.player;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Player-entity spec: Live Skill Rating is volatile, self-correcting, and bounded. */
class LiveSkillRatingTest {

    @Test
    void ratingMovesUpForStrongAndDownForPoorPerformance() {
        LiveSkillRating r = LiveSkillRating.atBaseline(50);
        assertThat(r.withPerformance(10).value()).isGreaterThan(50.0);
        assertThat(r.withPerformance(-10).value()).isLessThan(50.0);
    }

    @Test
    void ratingIsBounded() {
        LiveSkillRating r = LiveSkillRating.atBaseline(50);
        assertThat(r.withPerformance(1000).value()).isEqualTo(PlayerConstants.RATING_MAX);
        assertThat(r.withPerformance(-1000).value()).isEqualTo(PlayerConstants.RATING_MIN);
    }

    @Test
    void ratingSelfCorrectsTowardBaselineOnInactivity() {
        LiveSkillRating elevated = LiveSkillRating.atBaseline(50).withPerformance(30); // value 80
        LiveSkillRating decayed = elevated.decayedTowardBaseline(3);
        assertThat(decayed.value()).isLessThan(elevated.value());
        assertThat(decayed.value()).isGreaterThan(50.0);
        // Enough inactivity drives it all the way back to baseline (clamped by the fraction cap).
        assertThat(elevated.decayedTowardBaseline(100).value()).isEqualTo(50.0);
    }

    @Test
    void zeroStepsDecayIsANoOp() {
        LiveSkillRating r = LiveSkillRating.atBaseline(50).withPerformance(20);
        assertThat(r.decayedTowardBaseline(0)).isEqualTo(r);
    }
}
