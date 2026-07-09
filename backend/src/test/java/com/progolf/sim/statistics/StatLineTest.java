package com.progolf.sim.statistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

/** competitive-statistics spec: stat-line accumulation and derived reads (REQ-251). */
class StatLineTest {

    private static EventOutcome outcome(int position, int score, boolean madeCut, boolean withdrawn, double prize) {
        return new EventOutcome(1, "g1", position, score, madeCut, withdrawn, prize);
    }

    @Test
    void emptyIsTheAccumulationIdentity() {
        StatLine e = StatLine.empty();
        assertThat(e.events()).isZero();
        assertThat(e.bestFinish()).isEqualTo(Integer.MAX_VALUE);
        assertThat(e.plus(StatLine.empty())).isEqualTo(e);
    }

    @Test
    void aWinIsCountedAcrossEveryTally() {
        StatLine line = StatLine.of(outcome(1, -12, true, false, 1_000_000));
        assertThat(line.events()).isEqualTo(1);
        assertThat(line.cuts()).isEqualTo(1);
        assertThat(line.wins()).isEqualTo(1);
        assertThat(line.topTens()).isEqualTo(1);
        assertThat(line.bestFinish()).isEqualTo(1);
        assertThat(line.totalScoreVsPar()).isEqualTo(-12);
        assertThat(line.earnings()).isEqualTo(1_000_000);
    }

    @Test
    void aWithdrawalCountsNoCompetitiveStatsButKeepsEarnings() {
        StatLine line = StatLine.of(outcome(40, 0, false, true, 5_000));
        assertThat(line.events()).isZero();
        assertThat(line.cuts()).isZero();
        assertThat(line.bestFinish()).isEqualTo(Integer.MAX_VALUE);
        assertThat(line.earnings()).isEqualTo(5_000);
    }

    @Test
    void accumulationSumsAndKeepsTheBestFinish() {
        StatLine line = StatLine.empty()
                .plus(outcome(5, -4, true, false, 100_000))
                .plus(outcome(2, -8, true, false, 300_000))
                .plus(outcome(50, 6, false, false, 0));
        assertThat(line.events()).isEqualTo(3);
        assertThat(line.cuts()).isEqualTo(2);
        assertThat(line.runnerUps()).isEqualTo(1);
        assertThat(line.topTens()).isEqualTo(2);
        assertThat(line.bestFinish()).isEqualTo(2);
        assertThat(line.totalScoreVsPar()).isEqualTo(-6);
        assertThat(line.scoringAverage()).isCloseTo(-2.0, within(1e-9));
        assertThat(line.cutMakeRate()).isCloseTo(2.0 / 3.0, within(1e-9));
    }
}
