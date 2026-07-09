package com.progolf.sim.media;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** career-narrative spec: the classifier separates the descriptive narrative cases (REQ-244/248). */
class NarrativeClassifierTest {

    private static CareerNarrative classify(int age, int seasons, int wins, int rank, int sinceWin) {
        return NarrativeClassifier.classify(new CareerSummary(age, seasons, wins, rank, sinceWin));
    }

    @Test
    void recognisesDiverseFormsOfSuccess() {
        // Young, winless, climbing the ranks.
        assertThat(classify(20, 2, 0, 40, Integer.MAX_VALUE)).isEqualTo(CareerNarrative.RISING_PROSPECT);
        // Early-career first titles.
        assertThat(classify(22, 2, 2, 15, 0)).isEqualTo(CareerNarrative.BREAKTHROUGH_SEASON);
        // Top of the world with many wins.
        assertThat(classify(30, 12, 15, 1, 0)).isEqualTo(CareerNarrative.DOMINANT_CHAMPION);
        // Older golfer who has just won again.
        assertThat(classify(38, 15, 6, 10, 0)).isEqualTo(CareerNarrative.VETERAN_RESURGENCE);
        // A past winner long without a title.
        assertThat(classify(33, 8, 3, 20, 5)).isEqualTo(CareerNarrative.CHAMPIONSHIP_DROUGHT);
        // Steady, winless, well-ranked veteran of several seasons.
        assertThat(classify(29, 6, 0, 30, Integer.MAX_VALUE)).isEqualTo(CareerNarrative.CONSISTENT_CONTENDER);
        // Everyone else.
        assertThat(classify(31, 7, 0, 100, Integer.MAX_VALUE)).isEqualTo(CareerNarrative.ESTABLISHED_PROFESSIONAL);
    }
}
