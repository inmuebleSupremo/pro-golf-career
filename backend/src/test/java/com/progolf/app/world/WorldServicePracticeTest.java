package com.progolf.app.world;

import com.progolf.app.persistence.FilesystemSaveGameStore;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.practice.PracticeRange;
import com.progolf.sim.shot.AimPoint;
import com.progolf.sim.shot.BallStrikeIntent;
import com.progolf.sim.shot.ClubId;
import com.progolf.sim.world.WorldConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorldServicePracticeTest {
    @Test
    void practiceNeverChangesWorldOrPendingTournamentAndDoesNotPersist() throws Exception {
        Path saves = Files.createTempDirectory(Path.of("target"), "practice-test-");
        WorldService service = new WorldService(new FilesystemSaveGameStore(saves.toString()));
        WorldSession session = service.create("owner", 73, new WorldConfig(40, 6, 3, 20, 4));
        service.createPlayer("owner", session.id(), "Range", "Player", Nationality.GBR, 19, Archetype.POWER_HITTER);
        var before = session.world().snapshot();
        var intent = new BallStrikeIntent(ClubId.SEVEN_IRON, new AimPoint(18, 150));

        assertThat(service.practiceShot("owner", session.id(), intent, "0").outcome().trace()).isNotNull();
        var second = service.practiceShot("owner", session.id(), intent, 7, "1");
        assertThat(second.ballNumber()).isEqualTo(2);
        assertThat(second.outcome().trace().origin()).isEqualTo(PracticeRange.bays().getLast());
        assertThat(session.world().snapshot()).usingRecursiveComparison().isEqualTo(before);
        assertThatThrownBy(() -> service.practiceRange("other", session.id()))
                .isInstanceOf(WorldSessionNotFoundException.class);

        service.save("owner", session.id(), "range-save");
        WorldSession loaded = service.load("owner", "range-save");
        assertThat(service.practiceRange("owner", loaded.id()).ballsHit()).isZero();

        int weeks = 0;
        while (!service.hasPendingEvent("owner", session.id()) && weeks++ < 12) service.advanceWeek("owner", session.id());
        assertThat(service.hasPendingEvent("owner", session.id())).isTrue();
        var situation = service.currentSituation("owner", session.id());
        int week = session.world().currentWeek();
        service.practiceShot("owner", session.id(), intent, "2");
        assertThat(service.currentSituation("owner", session.id())).isEqualTo(situation);
        assertThat(session.world().currentWeek()).isEqualTo(week);
        assertThat(service.hasPendingEvent("owner", session.id())).isTrue();
    }
}
