package com.progolf.app.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.app.persistence.FilesystemSaveGameStore;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.world.WorldConfig;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldRestingReadTest {

    private static final String OWNER = "resting-read-test";
    private static final WorldConfig SMALL = new WorldConfig(40, 6, 3, 20, 4);

    @TempDir Path dir;

    @Test
    void restingDecisionReadsBackFromANamedSave() {
        WorldService service = new WorldService(new FilesystemSaveGameStore(dir.toString()));
        WorldSession session = service.create(OWNER, 901L, SMALL);
        service.createPlayer(OWNER, session.id(), "Rest", "Reader", Nationality.USA, 20,
                Archetype.ALL_ROUNDER);
        assertThat(service.playerResting(OWNER, session.id())).isFalse();

        service.setResting(OWNER, session.id(), true);
        service.save(OWNER, session.id(), "rested-career");

        WorldService reopened = new WorldService(new FilesystemSaveGameStore(dir.toString()));
        WorldSession restored = reopened.load(OWNER, "rested-career");
        assertThat(reopened.playerResting(OWNER, restored.id())).isTrue();
    }
}
