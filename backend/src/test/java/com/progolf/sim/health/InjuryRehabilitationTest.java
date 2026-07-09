package com.progolf.sim.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** injury-recovery spec: injuries heal only through gradual, multi-week rehabilitation (REQ-220). */
class InjuryRehabilitationTest {

    @Test
    void injuryAdvancesAndHeals() {
        Injury injury = Injury.of(InjuryType.WRIST, InjurySeverity.MODERATE);
        assertThat(injury.rehabWeeksRemaining()).isEqualTo(5);
        assertThat(injury.isHealed()).isFalse();
        assertThat(injury.advance(1).rehabWeeksRemaining()).isEqualTo(4);
        assertThat(injury.advance(5).isHealed()).isTrue();
        assertThat(injury.advance(99).rehabWeeksRemaining()).isZero(); // never negative
    }

    @Test
    void rehabilitationTakesTheFullDurationAndIsNotInstant() {
        PhysicalState state = PhysicalState.healthy(0.6).withInjury(Injury.of(InjuryType.BACK, InjurySeverity.MODERATE));

        // Four weeks in, still not fit to play.
        for (int week = 0; week < 4; week++) {
            state = HealthSystem.recoverWeek(state, 30);
            assertThat(state.injury()).as("still injured at week " + (week + 1)).isPresent();
            assertThat(state.canCompete()).isFalse();
        }
        // The fifth week completes rehabilitation and the golfer can compete again.
        state = HealthSystem.recoverWeek(state, 30);
        assertThat(state.injury()).isEmpty();
        assertThat(state.canCompete()).isTrue();
    }
}
