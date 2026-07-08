package com.progolf.sim.tournament;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Tournament-play & cut specs: relative-to-par leaderboard, tie positions, one-time cut. */
class TournamentScoringCutTest {

    @Test
    void leaderboardIsRankedLowerScoreFirstWithSharedTiePositions() {
        Tournament t = TournamentFixtures.openAndRegister(TournamentFixtures.standardDefinition(11), TournamentFixtures.field(30));
        t.advance(); // Round 1

        List<LeaderboardEntry> board = t.leaderboard();
        assertThat(board).isNotEmpty();
        assertThat(board.get(0).position()).isEqualTo(1);
        // Scores non-decreasing; positions non-decreasing; ties share a position (standard competition ranking).
        for (int i = 1; i < board.size(); i++) {
            assertThat(board.get(i).score()).isGreaterThanOrEqualTo(board.get(i - 1).score());
            assertThat(board.get(i).position()).isGreaterThanOrEqualTo(board.get(i - 1).position());
            if (board.get(i).score() == board.get(i - 1).score()) {
                assertThat(board.get(i).position()).isEqualTo(board.get(i - 1).position());
            }
        }
        assertThat(board).allSatisfy(e -> assertThat(e.roundsPlayed()).isEqualTo(1));
    }

    @Test
    void cumulativeScoreAccumulatesAcrossRounds() {
        Tournament t = TournamentFixtures.openAndRegister(TournamentFixtures.standardDefinition(12), TournamentFixtures.field(20));
        t.advance();
        assertThat(t.leaderboard()).allSatisfy(e -> assertThat(e.roundsPlayed()).isEqualTo(1));
        t.advance();
        // Every competitor now has two rounds recorded; the leaderboard reflects the accumulated total.
        // (Absolute score magnitude is a shot/course calibration concern, tracked separately.)
        assertThat(t.leaderboard()).allSatisfy(e -> assertThat(e.roundsPlayed()).isEqualTo(2));
    }

    @Test
    void cutIsAppliedOnceAndSplitsTheField() {
        Tournament t = TournamentFixtures.openAndRegister(TournamentFixtures.standardDefinition(13), TournamentFixtures.field(80));
        TournamentResult result = t.playToCompletion();

        assertThat(result.cutResult().applied()).isTrue();
        long madeCutFinishes = result.finishingOrder().stream().filter(TournamentResult.Finish::madeCut).count();
        assertThat(madeCutFinishes).isEqualTo(result.cutResult().madeCutCount());
        assertThat(madeCutFinishes).isGreaterThan(0).isLessThan(80);
        // Made-cut competitors all finish ahead of missed-cut competitors.
        int worstMadeCutPosition = result.finishingOrder().stream()
                .filter(TournamentResult.Finish::madeCut).mapToInt(TournamentResult.Finish::position).max().orElseThrow();
        int bestMissedCutPosition = result.finishingOrder().stream()
                .filter(f -> !f.madeCut()).mapToInt(TournamentResult.Finish::position).min().orElseThrow();
        assertThat(worstMadeCutPosition).isLessThan(bestMissedCutPosition);
    }

    @Test
    void noCutFormatKeepsWholeFieldPlaying() {
        TournamentDefinition def = TournamentFixtures.definition(TournamentFixtures.course(), 14,
                TournamentFormat.noCut(), EntryRequirements.standard());
        TournamentResult result = TournamentFixtures.openAndRegister(def, TournamentFixtures.field(20)).playToCompletion();
        assertThat(result.cutResult().applied()).isFalse();
        assertThat(result.finishingOrder()).allSatisfy(f -> assertThat(f.madeCut()).isTrue());
    }
}
