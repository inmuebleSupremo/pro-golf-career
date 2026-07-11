package com.progolf.sim.career;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.player.ProfessionalGolfer;
import com.progolf.sim.tournament.EventPrestige;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** career-legacy (modified): majors won are tracked and provide a Hall-of-Fame path. */
class CareerMajorsTest {

    private static final LocalDate DATE = LocalDate.of(2001, 6, 1);

    private static Career careerOf(ProfessionalGolfer subject) {
        return new Career(subject.player(), 20);
    }

    @Test
    void winningAMajorIncrementsMajorsWon() {
        ProfessionalGolfer subject = CareerFixtures.golfer(1);
        ProfessionalGolfer filler = CareerFixtures.golfer(2);
        Career career = careerOf(subject);

        career.recordTournament(CareerFixtures.result("Major", subject, filler, 1, true, false, 1000),
                EventPrestige.MAJOR, DATE);

        assertThat(career.statistics().majorsWon()).isEqualTo(1);
        assertThat(career.statistics().wins()).isEqualTo(1);
    }

    @Test
    void aRegularOrSignatureWinIsNotAMajor() {
        ProfessionalGolfer subject = CareerFixtures.golfer(3);
        ProfessionalGolfer filler = CareerFixtures.golfer(4);
        Career career = careerOf(subject);

        career.recordTournament(CareerFixtures.result("Reg", subject, filler, 1, true, false, 500),
                EventPrestige.REGULAR, DATE);
        career.recordTournament(CareerFixtures.result("Sig", subject, filler, 1, true, false, 500),
                EventPrestige.SIGNATURE, DATE);

        assertThat(career.statistics().wins()).isEqualTo(2);
        assertThat(career.statistics().majorsWon()).isZero();
    }

    @Test
    void majorsAreRequiredForTheHallOfFameBaselineButNotSufficientAlone() {
        ProfessionalGolfer subject = CareerFixtures.golfer(5);
        ProfessionalGolfer filler = CareerFixtures.golfer(6);
        Career career = careerOf(subject);

        // A win-rich career (>= the pro-win floor) but with too few majors is NOT baseline eligible.
        for (int i = 0; i < CareerConstants.HOF_MIN_PRO_WINS; i++) {
            career.recordTournament(CareerFixtures.result("Reg" + i, subject, filler, 1, true, false, 500),
                    EventPrestige.REGULAR, DATE);
        }
        assertThat(HallOfFame.meetsBaseline(
                HallOfFameCredentials.of(career.statistics(), 50, 0, false))).isFalse();

        // Adding the majors floor tips it over — with both the pro wins and majors, it is now eligible.
        for (int i = 0; i < CareerConstants.HOF_MIN_MAJORS; i++) {
            career.recordTournament(CareerFixtures.result("Major" + i, subject, filler, 1, true, false, 1000),
                    EventPrestige.MAJOR, DATE);
        }
        assertThat(career.statistics().majorsWon()).isEqualTo(CareerConstants.HOF_MIN_MAJORS);
        assertThat(HallOfFame.meetsBaseline(
                HallOfFameCredentials.of(career.statistics(), 50, 0, false))).isTrue();
    }
}
