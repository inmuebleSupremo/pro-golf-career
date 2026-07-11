package com.progolf.sim.career;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** career-legacy: the two-phase Hall-of-Fame evaluator — baseline eligibility and the prestige-weighted score. */
class HallOfFameTest {

    private static final int WINS = CareerConstants.HOF_MIN_PRO_WINS;
    private static final int MAJORS = CareerConstants.HOF_MIN_MAJORS;

    /** Credentials with the given majors/wins, at an age/retirement state. Wins are all professional. */
    private static HallOfFameCredentials cred(int majors, int totalProWins, int age, int seasonsRetired,
                                              boolean retired) {
        // totalWins includes the majors; developmentWins = 0 so proWins == totalWins.
        return new HallOfFameCredentials(majors, 0, totalProWins, 0, age, seasonsRetired, retired);
    }

    @Test
    void baselineNeedsBothStatusAndStatistics() {
        // Meets stats (>=15 pro wins AND >=2 majors) but too young and not retired -> fails the status gate.
        assertThat(HallOfFame.meetsBaseline(cred(MAJORS, WINS, 40, 0, false))).isFalse();
        // Old enough -> both gates met.
        assertThat(HallOfFame.meetsBaseline(cred(MAJORS, WINS, CareerConstants.HOF_MIN_AGE, 0, false))).isTrue();
        // Young but long-enough retired -> status met via the retirement clause.
        assertThat(HallOfFame.meetsBaseline(cred(MAJORS, WINS, 38, CareerConstants.HOF_RETIRED_SEASONS, true)))
                .isTrue();
    }

    @Test
    void baselineRequiresBothProWinsAndMajors() {
        int age = CareerConstants.HOF_MIN_AGE;
        assertThat(HallOfFame.meetsBaseline(cred(MAJORS, WINS - 1, age, 0, false))).isFalse(); // one win short
        assertThat(HallOfFame.meetsBaseline(cred(MAJORS - 1, WINS, age, 0, false))).isFalse(); // one major short
        assertThat(HallOfFame.meetsBaseline(cred(MAJORS, WINS, age, 0, false))).isTrue();       // both met
    }

    @Test
    void developmentWinsDoNotCountTowardTheProWinBaseline() {
        // 20 total wins but 10 are development-tier -> only 10 pro wins, below the floor.
        HallOfFameCredentials c = new HallOfFameCredentials(MAJORS, 0, 20, 10, CareerConstants.HOF_MIN_AGE, 0, false);
        assertThat(c.proWins()).isEqualTo(10);
        assertThat(HallOfFame.meetsBaseline(c)).isFalse();
    }

    @Test
    void scoreFavoursPrestige() {
        // A single major outweighs a signature win, which outweighs a regular win, which outweighs a dev win.
        double major = HallOfFame.score(new HallOfFameCredentials(1, 0, 1, 0, 50, 0, true));
        double signature = HallOfFame.score(new HallOfFameCredentials(0, 1, 1, 0, 50, 0, true));
        double regular = HallOfFame.score(new HallOfFameCredentials(0, 0, 1, 0, 50, 0, true));
        double development = HallOfFame.score(new HallOfFameCredentials(0, 0, 1, 1, 50, 0, true));
        assertThat(major).isGreaterThan(signature);
        assertThat(signature).isGreaterThan(regular);
        assertThat(regular).isGreaterThan(development);
    }

    @Test
    void aMajorRichCareerOutscoresAWinRichButMajorlessOne() {
        // 5 majors + 15 wins vs 30 regular wins and no majors: prestige should decide.
        double majorRich = HallOfFame.score(new HallOfFameCredentials(5, 0, 15, 0, 50, 0, true));
        double winRich = HallOfFame.score(new HallOfFameCredentials(0, 0, 30, 0, 50, 0, true));
        assertThat(majorRich).isGreaterThan(winRich);
    }
}
