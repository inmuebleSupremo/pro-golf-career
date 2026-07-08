package com.progolf.sim.tournament;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.sim.player.ProfessionalGolfer;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Cut-playoff spec: sudden-death resolution yields exactly one winner, reproducibly. */
class TournamentPlayoffTest {

    @Test
    void suddenDeathYieldsExactlyOneWinnerDeterministically() {
        TournamentDefinition def = TournamentFixtures.standardDefinition(21);
        List<ProfessionalGolfer> golfers = TournamentFixtures.field(3);
        List<TournamentEntry> tied = List.of(
                new TournamentEntry(golfers.get(0), 0),
                new TournamentEntry(golfers.get(1), 1),
                new TournamentEntry(golfers.get(2), 2));

        ProfessionalGolfer w1 = Tournament.suddenDeath(def, tied);
        ProfessionalGolfer w2 = Tournament.suddenDeath(def, tied);

        assertThat(w1.player().id()).isEqualTo(w2.player().id()); // reproducible
        assertThat(List.of(golfers.get(0).player().id(), golfers.get(1).player().id(), golfers.get(2).player().id()))
                .contains(w1.player().id()); // one of the tied competitors
    }

    @Test
    void singleContenderIsImmediatelyTheWinner() {
        TournamentDefinition def = TournamentFixtures.standardDefinition(22);
        ProfessionalGolfer only = TournamentFixtures.field(1).get(0);
        assertThat(Tournament.suddenDeath(def, List.of(new TournamentEntry(only, 0))).player().id())
                .isEqualTo(only.player().id());
    }
}
