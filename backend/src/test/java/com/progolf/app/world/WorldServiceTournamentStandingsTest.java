package com.progolf.app.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.app.persistence.FilesystemSaveGameStore;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.tournament.TournamentResult;
import com.progolf.sim.world.WorldConfig;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The player's completed event uses the same official rank in the play screen and career result. */
class WorldServiceTournamentStandingsTest {
    private static final String OWNER = "tournament-standings-test";

    @TempDir Path saves;

    @Test
    void fourRoundPlayerEventShowsItsOfficialFinishAndPersistsItsResult() {
        WorldService service = new WorldService(new FilesystemSaveGameStore(saves.toString()));
        WorldSession session = service.create(OWNER, 7L, new WorldConfig(80, 6, 3, 40, 4, 0, 0));
        String playerId = service.createPlayer(OWNER, session.id(), "Test", "Golfer",
                Nationality.GBR, 22, Archetype.POWER_HITTER);
        int weeks = 0;
        while (!service.hasPendingEvent(OWNER, session.id()) && weeks++ < 6) {
            service.advanceWeek(OWNER, session.id());
        }
        assertThat(service.hasPendingEvent(OWNER, session.id())).isTrue();

        service.simEvent(OWNER, session.id());
        var event = session.world().playerEvent();
        TournamentResult official = event.result();
        var board = service.eventLeaderboard(OWNER, session.id());
        assertThat(board).hasSize(official.finishingOrder().size());
        for (int i = 0; i < board.size(); i++) {
            var row = board.get(i);
            var finish = official.finishingOrder().get(i);
            assertThat(row.golfer().player().id()).isEqualTo(finish.golfer().player().id());
            assertThat(row.position()).isEqualTo(finish.position());
            assertThat(row.score()).isEqualTo(finish.score());
        }
        var playerFinish = official.finishingOrder().stream()
                .filter(f -> f.golfer().player().id().equals(playerId)).findFirst().orElseThrow();
        var playerRow = board.stream().filter(r -> r.golfer().player().id().equals(playerId)).findFirst().orElseThrow();
        assertThat(playerRow.position()).isEqualTo(playerFinish.position());
        assertThat(event.playerRounds().stream().mapToInt(r -> r.scoreVsPar()).sum()).isEqualTo(playerFinish.score());
        assertThat(event.playerRounds()).hasSize(playerFinish.madeCut() ? 4 : 2);
        assertThat(board.getFirst().position()).isEqualTo(1);

        long tournamentId = official.tournamentId();
        service.completeEvent(OWNER, session.id());
        assertThat(session.world().resultOf(tournamentId)).isPresent();
    }
}
