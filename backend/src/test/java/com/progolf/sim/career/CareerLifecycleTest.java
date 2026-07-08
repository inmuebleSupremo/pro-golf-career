package com.progolf.sim.career;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.CareerStatus;
import com.progolf.sim.player.Identity;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.player.Player;
import com.progolf.sim.player.ProfessionalGolfer;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Career-lifecycle spec: start validation, age/retirement, read-only completion, runtime states. */
class CareerLifecycleTest {

    private static final LocalDate DATE = LocalDate.of(2001, 10, 1);

    @Test
    void startValidatesAgeAndRequiresAnActivePlayer() {
        ProfessionalGolfer g = CareerFixtures.golfer(1);
        assertThatThrownBy(() -> new Career(g.player(), 15)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Career(g.player(), 23)).isInstanceOf(IllegalArgumentException.class);

        Player created = new Player("created", new Identity("Test", "Golfer", Nationality.USA,
                LocalDate.of(1985, 1, 1), Archetype.TOP_COLLEGE_GRADUATE), Attributes.uniform(60));
        assertThatThrownBy(() -> new Career(created, 20)).isInstanceOf(IllegalStateException.class);

        Career career = new Career(g.player(), 20);
        assertThat(career.age()).isEqualTo(20);
        assertThat(career.isRetired()).isFalse();
        assertThat(career.player()).isSameAs(g.player()); // owned, not transferable
    }

    @Test
    void ageAdvancesByOnePerSeasonAndNeverDecreases() {
        Career career = new Career(CareerFixtures.golfer(2).player(), 20);
        career.advanceSeason(DATE);
        career.advanceSeason(DATE.plusYears(1));
        career.advanceSeason(DATE.plusYears(2));
        assertThat(career.age()).isEqualTo(23);
        assertThat(career.seasons()).hasSize(3);
        assertThat(career.seasons().get(2).endAge()).isEqualTo(23);
    }

    @Test
    void reachingSixtyFiveRetiresAutomaticallyAndCannotBeBypassed() {
        Career career = new Career(CareerFixtures.golfer(3).player(), 22);
        // Advance until mandatory retirement.
        LocalDate d = DATE;
        while (!career.isRetired()) {
            career.advanceSeason(d);
            d = d.plusYears(1);
        }
        assertThat(career.age()).isEqualTo(65);
        assertThat(career.player().status()).isEqualTo(CareerStatus.RETIRED);
        assertThat(career.hallOfFameResult()).isPresent();
        // Cannot advance or compete after retirement.
        final LocalDate after = d;
        assertThatThrownBy(() -> career.advanceSeason(after)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void retiredCareerRejectsCompetitiveUpdates() {
        Career career = new Career(CareerFixtures.golfer(4).player(), 22);
        LocalDate d = DATE;
        while (!career.isRetired()) { // advance seasons until mandatory retirement
            career.advanceSeason(d);
            d = d.plusYears(1);
        }
        assertThat(career.isRetired()).isTrue();
        ProfessionalGolfer subject = CareerFixtures.golfer(4);
        assertThatThrownBy(() -> career.recordTournament(
                CareerFixtures.result("Open", subject, CareerFixtures.golfer(5), 1, true, false, 100), DATE))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void runtimeStateChangesDoNotAdvanceProgression() {
        Career career = new Career(CareerFixtures.golfer(6).player(), 20);
        int ageBefore = career.age();
        career.setRuntimeState(CareerRuntimeState.PAUSED);
        assertThat(career.runtimeState()).isEqualTo(CareerRuntimeState.PAUSED);
        assertThat(career.age()).isEqualTo(ageBefore);
        assertThat(career.seasons()).isEmpty();
        assertThat(career.statistics().eventsPlayed()).isZero();
    }
}
