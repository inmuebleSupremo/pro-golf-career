package com.progolf.sim.career;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.player.ProfessionalGolfer;
import com.progolf.sim.tournament.EventPrestige;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Career-legacy spec: on retirement a Career records its Hall-of-Fame baseline eligibility, derived from stats. */
class CareerLegacyTest {

    private static final LocalDate D = LocalDate.of(2044, 1, 1);

    /** A career with the given regular pro wins and majors, advanced to mandatory retirement. */
    private static Career retiredCareer(int index, int regularWins, int majors) {
        ProfessionalGolfer subject = CareerFixtures.golfer(index);
        ProfessionalGolfer filler = CareerFixtures.golfer(index + 500);
        Career c = new Career(subject.player(), 22);
        for (int i = 0; i < regularWins; i++) {
            c.recordTournament(CareerFixtures.result("Win " + i, subject, filler, 1, true, false, 1000),
                    EventPrestige.REGULAR, D.plusDays(i));
        }
        for (int i = 0; i < majors; i++) {
            c.recordTournament(CareerFixtures.result("Major " + i, subject, filler, 1, true, false, 2000),
                    EventPrestige.MAJOR, D.plusDays(200 + i));
        }
        LocalDate d = D.plusDays(400);
        while (!c.isRetired()) { // advance seasons until mandatory retirement (age 65 >= the status age)
            c.advanceSeason(d);
            d = d.plusYears(1);
        }
        return c;
    }

    @Test
    void retirementRecordsBaselineEligibilityForAStrongCareer() {
        // Meets both the pro-win floor and the majors floor -> baseline eligible at retirement.
        Career c = retiredCareer(1, CareerConstants.HOF_MIN_PRO_WINS, CareerConstants.HOF_MIN_MAJORS);
        assertThat(c.isRetired()).isTrue();
        assertThat(c.hallOfFameResult()).isPresent();
        assertThat(c.hallOfFameResult().get().eligible()).isTrue();
        assertThat(c.statistics().majorsWon()).isEqualTo(CareerConstants.HOF_MIN_MAJORS);
    }

    @Test
    void aWinRichCareerWithoutTheMajorsFloorIsNotEligible() {
        // Plenty of wins but one major short of the floor -> not baseline eligible (majors are required).
        Career c = retiredCareer(2, CareerConstants.HOF_MIN_PRO_WINS, CareerConstants.HOF_MIN_MAJORS - 1);
        assertThat(c.hallOfFameResult()).isPresent();
        assertThat(c.hallOfFameResult().get().eligible()).isFalse();
    }

    @Test
    void aModestCareerIsNotHallOfFameEligible() {
        Career c = retiredCareer(3, 2, 0);
        assertThat(c.hallOfFameResult()).isPresent();
        assertThat(c.hallOfFameResult().get().eligible()).isFalse();
    }
}
