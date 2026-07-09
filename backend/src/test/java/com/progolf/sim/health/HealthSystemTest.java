package com.progolf.sim.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.core.Rng;
import com.progolf.sim.core.SplitMix64Rng;
import org.junit.jupiter.api.Test;

/** physical-state / injury-recovery spec: fitness seeding, fatigue accrual/recovery, injury rolls. */
class HealthSystemTest {

    private static Rng rng(long seed) {
        return new SplitMix64Rng(seed);
    }

    @Test
    void initialStateSeedsFitnessInRangeDeterministically() {
        PhysicalState a = HealthSystem.initialState(rng(1L));
        PhysicalState b = HealthSystem.initialState(rng(1L));
        assertThat(a).isEqualTo(b);
        assertThat(a.fitness()).isBetween(HealthConstants.FITNESS_MIN, HealthConstants.FITNESS_MAX);
        assertThat(a.fatigue()).isZero();
    }

    @Test
    void participationRaisesFatigue() {
        PhysicalState after = HealthSystem.afterParticipation(PhysicalState.healthy(0.6), 25, rng(2L));
        assertThat(after.fatigue()).isGreaterThan(0.0);
    }

    @Test
    void fitterGolfersTireLessAndRecoverMore() {
        // Fatigue accrual is independent of the injury roll, so compare it directly.
        double fitAdded = HealthSystem.afterParticipation(PhysicalState.healthy(0.9), 25, rng(3L)).fatigue();
        double unfitAdded = HealthSystem.afterParticipation(PhysicalState.healthy(0.3), 25, rng(3L)).fatigue();
        assertThat(fitAdded).isLessThan(unfitAdded);

        double fitLeft = HealthSystem.recoverWeek(PhysicalState.healthy(0.9).withFatigue(0.5), 25).fatigue();
        double unfitLeft = HealthSystem.recoverWeek(PhysicalState.healthy(0.3).withFatigue(0.5), 25).fatigue();
        assertThat(fitLeft).isLessThan(unfitLeft); // fitter recovers more, so less remains
    }

    @Test
    void olderGolfersTireMore() {
        double young = HealthSystem.afterParticipation(PhysicalState.healthy(0.6), 25, rng(4L)).fatigue();
        double old = HealthSystem.afterParticipation(PhysicalState.healthy(0.6), 45, rng(4L)).fatigue();
        assertThat(old).isGreaterThan(young);
    }

    @Test
    void recoveryIsGradual() {
        PhysicalState after = HealthSystem.recoverWeek(PhysicalState.healthy(0.6).withFatigue(0.5), 25);
        assertThat(after.fatigue()).isLessThan(0.5).isGreaterThan(0.0); // moved down, not reset
    }

    @Test
    void injuriesOccurUnderStrainAndBlockCompetition() {
        // A fatigued, unfit, older golfer over many events eventually gets hurt.
        boolean sawInjury = false;
        for (int seed = 0; seed < 400 && !sawInjury; seed++) {
            PhysicalState strained = PhysicalState.healthy(0.3).withFatigue(0.9);
            PhysicalState after = HealthSystem.afterParticipation(strained, 45, rng(seed));
            if (after.injury().isPresent()) {
                sawInjury = true;
                assertThat(after.canCompete()).isFalse();
                assertThat(after.injury().get().rehabWeeksRemaining()).isGreaterThan(0);
            }
        }
        assertThat(sawInjury).isTrue();
    }

    @Test
    void anAlreadyInjuredGolferRollsNoNewInjury() {
        PhysicalState injured = PhysicalState.healthy(0.3).withFatigue(0.9)
                .withInjury(Injury.of(InjuryType.KNEE, InjurySeverity.SEVERE));
        PhysicalState after = HealthSystem.afterParticipation(injured, 45, rng(7L));
        assertThat(after.injury()).contains(Injury.of(InjuryType.KNEE, InjurySeverity.SEVERE));
    }
}
