package com.progolf.app.world;

import static org.assertj.core.api.Assertions.assertThat;

import com.progolf.app.persistence.FilesystemSaveGameStore;
import com.progolf.sim.control.CareerGoal;
import com.progolf.sim.control.GoalType;
import com.progolf.sim.core.Attribute;
import com.progolf.sim.world.WorldConfig;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** save-persistence: WorldService saves/loads sessions through the store and autosaves at checkpoints. */
class WorldPersistenceTest {

    private static final WorldConfig SMALL = new WorldConfig(40, 6, 3, 20, 4);

    @TempDir
    Path dir;
    private WorldService service;

    @BeforeEach
    void setUp() {
        service = new WorldService(new FilesystemSaveGameStore(dir.toString()));
    }

    @Test
    void savingAndLoadingASessionYieldsAnIdenticalWorld() {
        WorldSession original = service.create(123L, SMALL);
        service.advanceSeason(original.id());
        service.save(original.id(), "slot1");

        WorldSession loaded = service.load("slot1");
        assertThat(loaded.id()).isNotEqualTo(original.id()); // a load is a fresh session
        assertThat(loaded.world().snapshot()).isEqualTo(original.world().snapshot());

        // And it continues identically.
        service.advanceSeason(original.id());
        service.advanceSeason(loaded.id());
        assertThat(loaded.world().snapshot()).isEqualTo(original.world().snapshot());
    }

    @Test
    void advancingASeasonRefreshesTheAutosaveSlot() {
        WorldSession session = service.create(7L, SMALL);
        service.advanceSeason(session.id());

        assertThat(service.listSaves()).extracting(m -> m.saveId()).contains(WorldService.AUTOSAVE_ID);
        WorldSession restored = service.load(WorldService.AUTOSAVE_ID);
        assertThat(restored.world().snapshot()).isEqualTo(session.world().snapshot());
    }

    @Test
    void aPlayerCareerSurvivesTheDisk() {
        WorldSession session = service.create(4L, SMALL);
        String golferId = session.world().activeGolferIds().get(0);
        service.assignPlayer(session.id(), golferId);
        service.setDevelopmentFocus(session.id(), List.of(Attribute.DRIVING_DISTANCE));
        service.setCareerGoals(session.id(), List.of(new CareerGoal(GoalType.WIN_A_MAJOR, 1)));
        service.advanceSeason(session.id());

        WorldSession loaded = service.load(WorldService.AUTOSAVE_ID);
        assertThat(loaded.world().playerGolferId()).contains(golferId);
        assertThat(loaded.world().snapshot()).isEqualTo(session.world().snapshot());
    }

    @Test
    void savesAreIndependentAndDeletable() {
        WorldSession a = service.create(1L, SMALL);
        WorldSession b = service.create(2L, SMALL);
        service.save(a.id(), "a");
        service.save(b.id(), "b");

        assertThat(service.listSaves()).extracting(m -> m.saveId()).contains("a", "b");
        assertThat(service.load("a").world().currentSeason()).isEqualTo(1);
        service.deleteSave("a");
        assertThat(service.listSaves()).extracting(m -> m.saveId()).doesNotContain("a").contains("b");
    }
}
