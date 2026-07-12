package com.progolf.sim.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * injury-recovery play-through: a recovering injury may be played through at a severity-scaled impairment,
 * the early injured stage may not, and competing while injured freezes rehabilitation.
 */
class PlayThroughInjuryTest {

    @Test
    void aMinorInjuryIsPlayableFromTheOutsetAtItsImpairment() {
        // MINOR rehab (2 wks) equals the recovering threshold, so a MINOR is recovering immediately.
        PhysicalState s = PhysicalState.healthy(0.6).withInjury(Injury.of(InjuryType.WRIST, InjurySeverity.MINOR));
        assertThat(s.availability()).isEqualTo(Availability.RECOVERING);
        assertThat(s.canCompete()).isFalse(); // not unimpaired-available
        assertThat(s.canPlayThroughInjury()).isTrue();
        assertThat(s.injuryImpairment()).isEqualTo(InjurySeverity.MINOR.impairment());
    }

    @Test
    void aSevereInjuryIsBenchedEarlyThenPlayableInItsTail() {
        PhysicalState s = PhysicalState.healthy(0.6).withInjury(Injury.of(InjuryType.BACK, InjurySeverity.SEVERE));
        // Early stage: injured, not playable, zero impairment exposed.
        assertThat(s.availability()).isEqualTo(Availability.INJURED);
        assertThat(s.canPlayThroughInjury()).isFalse();
        assertThat(s.injuryImpairment()).isZero();

        // Rehab into the recovering tail (12 -> 2 weeks left): now playable at the SEVERE impairment.
        for (int w = 0; w < 10; w++) {
            s = HealthSystem.recoverWeek(s, 30);
        }
        assertThat(s.availability()).isEqualTo(Availability.RECOVERING);
        assertThat(s.canPlayThroughInjury()).isTrue();
        assertThat(s.injuryImpairment()).isEqualTo(InjurySeverity.SEVERE.impairment());
    }

    @Test
    void aHealthyGolferHasNoImpairmentAndIsNotPlayingThrough() {
        PhysicalState s = PhysicalState.healthy(0.7);
        assertThat(s.canPlayThroughInjury()).isFalse();
        assertThat(s.injuryImpairment()).isZero();
    }

    @Test
    void competingFreezesRehabWhileRestingAdvancesIt() {
        PhysicalState injured = PhysicalState.healthy(0.6).withInjury(Injury.of(InjuryType.KNEE, InjurySeverity.MINOR));
        int startWeeks = injured.injury().orElseThrow().rehabWeeksRemaining();

        // Grinding: rehab does not advance, the injury persists at the same weeks remaining.
        PhysicalState grinded = HealthSystem.recoverWeek(injured, 30, true);
        assertThat(grinded.injury()).isPresent();
        assertThat(grinded.injury().orElseThrow().rehabWeeksRemaining()).isEqualTo(startWeeks);

        // Resting: rehab advances one week.
        PhysicalState rested = HealthSystem.recoverWeek(injured, 30, false);
        assertThat(rested.injury().orElseThrow().rehabWeeksRemaining()).isEqualTo(startWeeks - 1);
    }

    @Test
    void impairmentIsOrderedBySeverity() {
        assertThat(InjurySeverity.MINOR.impairment())
                .isLessThan(InjurySeverity.MODERATE.impairment());
        assertThat(InjurySeverity.MODERATE.impairment())
                .isLessThan(InjurySeverity.SEVERE.impairment());
    }
}
