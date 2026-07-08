package com.progolf.sim.tournament;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.player.ProfessionalGolfer;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Completion spec: withdrawal does not invalidate the event and is recorded. */
class TournamentWithdrawalTest {

    @Test
    void midEventWithdrawalKeepsTheTournamentValidAndCompleting() {
        List<ProfessionalGolfer> golfers = TournamentFixtures.field(20);
        Tournament t = TournamentFixtures.openAndRegister(TournamentFixtures.standardDefinition(31), golfers);
        t.advance(); // Round 1
        ProfessionalGolfer quitter = golfers.get(0);
        t.withdraw(quitter);

        TournamentResult result = t.playToCompletion();
        assertThat(t.state()).isEqualTo(TournamentState.COMPLETED);

        TournamentResult.Finish quitterFinish = result.finishingOrder().stream()
                .filter(f -> f.golfer().player().id().equals(quitter.player().id()))
                .findFirst().orElseThrow();
        assertThat(quitterFinish.withdrawn()).isTrue();
        // The winner is not the withdrawn competitor.
        assertThat(result.winner().player().id()).isNotEqualTo(quitter.player().id());
    }

    @Test
    void withdrawnCompetitorsRankBehindActiveOnes() {
        List<ProfessionalGolfer> golfers = TournamentFixtures.field(12);
        Tournament t = TournamentFixtures.openAndRegister(TournamentFixtures.standardDefinition(32), golfers);
        t.withdraw(golfers.get(0));
        TournamentResult result = t.playToCompletion();

        int withdrawnPos = result.finishingOrder().stream()
                .filter(TournamentResult.Finish::withdrawn).mapToInt(TournamentResult.Finish::position).min().orElseThrow();
        int activeWorstPos = result.finishingOrder().stream()
                .filter(f -> !f.withdrawn()).mapToInt(TournamentResult.Finish::position).max().orElseThrow();
        assertThat(withdrawnPos).isGreaterThan(activeWorstPos);
    }
}
