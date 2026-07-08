package com.progolf.sim.tournament;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Completion & integrity specs: end-to-end run, exactly one winner, control-free resolution. */
class TournamentEngineTest {

    @Test
    void fullEventRunsToASingleWinnerWithPermanentHistory() {
        Tournament t = TournamentFixtures.openAndRegister(TournamentFixtures.standardDefinition(51), TournamentFixtures.field(48));
        TournamentResult result = t.playToCompletion();

        assertThat(result.winner()).isNotNull();
        assertThat(result.finishingOrder()).hasSize(48);
        long leaders = result.finishingOrder().stream().filter(f -> f.position() == 1).count();
        assertThat(leaders).isEqualTo(1); // exactly one winner
        assertThat(result.finishingOrder().get(0).golfer().player().id()).isEqualTo(result.winner().player().id());
        // History is retrievable and the winner earns the top prize amount.
        assertThat(result.finishingOrder().get(0).prize())
                .isEqualTo(TournamentConstants.TOP_PRIZE);
    }

    @Test
    void winnerHasTheBestScoreAmongMadeCutCompetitors() {
        TournamentResult result = TournamentFixtures.openAndRegister(
                TournamentFixtures.standardDefinition(52), TournamentFixtures.field(30)).playToCompletion();
        int winnerScore = result.finishingOrder().stream()
                .filter(f -> f.golfer().player().id().equals(result.winner().player().id()))
                .mapToInt(TournamentResult.Finish::score).findFirst().orElseThrow();
        int bestMadeCut = result.finishingOrder().stream()
                .filter(TournamentResult.Finish::madeCut).mapToInt(TournamentResult.Finish::score).min().orElseThrow();
        assertThat(winnerScore).isEqualTo(bestMadeCut);
    }

    @Test
    void resolutionDoesNotBranchOnControlType() throws IOException {
        // The tournament resolves shots via the shared engine using the decision policy seam, never by
        // reading control type. Assert the package's source never references control type.
        Path root = Path.of("src", "main", "java", "com", "progolf", "sim", "tournament");
        assertThat(Files.isDirectory(root)).isTrue();
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                try {
                    assertThat(Files.readString(p))
                            .as("%s must not branch on control type", p)
                            .doesNotContain("ControlType")
                            .doesNotContain("controlType");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
