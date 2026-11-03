package com.progolf.app.world;

import com.progolf.app.persistence.FilesystemSaveGameStore;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.putt.PhysicalPuttIntent;
import com.progolf.sim.course.Position2d;
import com.progolf.sim.world.WorldConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorldServicePracticePuttingTest {
    @Test
    void puttingPracticeIsOwnerScopedAndExcludedFromCareerAndSaves() throws Exception {
        Path saves = Files.createTempDirectory(Path.of("target"), "putting-practice-test-");
        WorldService service = new WorldService(new FilesystemSaveGameStore(saves.toString()));
        WorldSession session = service.create("owner", 73, new WorldConfig(40, 6, 3, 20, 4));
        service.createPlayer("owner", session.id(), "Green", "Player", Nationality.GBR, 19, Archetype.PRECISION_PLAYER);
        var before = session.world().snapshot();
        var start = service.practicePuttingGreen("owner", session.id()).state();
        var result = service.practicePhysicalPutt("owner", session.id(), 3,
                new PhysicalPuttIntent(new Position2d(0, 1), 6), start.shotRevision());
        assertThat(result.outcome().trajectory()).hasSizeGreaterThan(1);
        assertThat(result.state().puttsHit()).isEqualTo(1);
        assertThat(session.world().snapshot()).usingRecursiveComparison().isEqualTo(before);
        assertThatThrownBy(() -> service.practicePuttingGreen("other", session.id()))
                .isInstanceOf(WorldSessionNotFoundException.class);

        service.save("owner", session.id(), "putting-save");
        WorldSession loaded = service.load("owner", "putting-save");
        assertThat(service.practicePuttingGreen("owner", loaded.id()).state().puttsHit()).isZero();
        assertThat(service.practicePuttingGreen("owner", loaded.id()).state().restingBalls()).isEmpty();
    }
}
