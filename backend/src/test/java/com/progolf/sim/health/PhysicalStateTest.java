package com.progolf.sim.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/** physical-state / availability spec: state model and availability derivation. */
class PhysicalStateTest {

    @Test
    void freshStateIsAvailable() {
        PhysicalState s = PhysicalState.healthy(0.6);
        assertThat(s.fatigue()).isZero();
        assertThat(s.injury()).isEmpty();
        assertThat(s.availability()).isEqualTo(Availability.AVAILABLE);
        assertThat(s.canCompete()).isTrue();
    }

    @Test
    void excessiveFatigueForcesRest() {
        PhysicalState s = PhysicalState.healthy(0.6).withFatigue(0.9);
        assertThat(s.availability()).isEqualTo(Availability.RESTING);
        assertThat(s.canCompete()).isFalse();
    }

    @Test
    void activeInjuryBlocksCompetitionAndTransitionsToRecovering() {
        PhysicalState injured = PhysicalState.healthy(0.6).withInjury(Injury.of(InjuryType.BACK, InjurySeverity.MODERATE));
        assertThat(injured.availability()).isEqualTo(Availability.INJURED); // 5 weeks > recovering threshold
        assertThat(injured.canCompete()).isFalse();

        PhysicalState lateRehab = PhysicalState.healthy(0.6).withInjury(new Injury(InjuryType.BACK, InjurySeverity.MODERATE, 2));
        assertThat(lateRehab.availability()).isEqualTo(Availability.RECOVERING);
        assertThat(lateRehab.canCompete()).isFalse();
    }

    @Test
    void clearingInjuryRestoresAvailabilityFromFatigue() {
        PhysicalState s = PhysicalState.healthy(0.6).withFatigue(0.2)
                .withInjury(Injury.of(InjuryType.WRIST, InjurySeverity.MINOR))
                .clearedInjury();
        assertThat(s.injury()).isEmpty();
        assertThat(s.availability()).isEqualTo(Availability.AVAILABLE);
    }

    @Test
    void rejectsOutOfRangeAndNullInjury() {
        assertThatThrownBy(() -> new PhysicalState(1.5, 0.0, Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PhysicalState(0.6, 1.2, Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PhysicalState(0.6, 0.0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
