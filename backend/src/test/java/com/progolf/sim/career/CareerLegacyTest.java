package com.progolf.sim.career;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.player.ProfessionalGolfer;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Career-legacy spec: Hall-of-Fame evaluation runs on retirement, derived from stats, recorded. */
class CareerLegacyTest {

    private static final LocalDate D = LocalDate.of(2044, 1, 1);

    private static Career careerWithWins(int index, int wins) {
        ProfessionalGolfer subject = CareerFixtures.golfer(index);
        ProfessionalGolfer filler = CareerFixtures.golfer(index + 500);
        Career c = new Career(subject.player(), 22);
        for (int i = 0; i < wins; i++) {
            c.recordTournament(CareerFixtures.result("Win " + i, subject, filler, 1, true, false, 1000), D.plusDays(i));
        }
        LocalDate d = D.plusDays(400);
        while (!c.isRetired()) { // advance seasons until mandatory retirement
            c.advanceSeason(d);
            d = d.plusYears(1);
        }
        return c;
    }

    @Test
    void hallOfFameEvaluationRunsOnRetirementAndIsEligibleForAStrongCareer() {
        Career c = careerWithWins(1, CareerConstants.HOF_MIN_WINS); // exactly the win threshold
        assertThat(c.isRetired()).isTrue();
        assertThat(c.hallOfFameResult()).isPresent();
        assertThat(c.hallOfFameResult().get().eligible()).isTrue();
        // Derived from stats.
        assertThat(c.statistics().wins()).isEqualTo(CareerConstants.HOF_MIN_WINS);
    }

    @Test
    void modestCareerIsNotHallOfFameEligible() {
        Career c = careerWithWins(2, 2);
        assertThat(c.hallOfFameResult()).isPresent();
        assertThat(c.hallOfFameResult().get().eligible()).isFalse();
    }
}
