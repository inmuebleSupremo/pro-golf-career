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
    private static final String OWNER = "owner-persist-test";

    @TempDir
    Path dir;
    private WorldService service;

    @BeforeEach
    void setUp() {
        service = new WorldService(new FilesystemSaveGameStore(dir.toString()));
    }

    @Test
    void savingAndLoadingASessionYieldsAnIdenticalWorld() {
        WorldSession original = service.create(OWNER, 123L, SMALL);
        service.advanceSeason(OWNER, original.id());
        service.save(OWNER, original.id(), "slot1");

        WorldSession loaded = service.load(OWNER, "slot1");
        assertThat(loaded.id()).isNotEqualTo(original.id()); // a load is a fresh session
        assertThat(loaded.world().snapshot()).isEqualTo(original.world().snapshot());

        // And it continues identically.
        service.advanceSeason(OWNER, original.id());
        service.advanceSeason(OWNER, loaded.id());
        assertThat(loaded.world().snapshot()).isEqualTo(original.world().snapshot());
    }

    @Test
    void advancingASeasonRefreshesTheAutosaveSlot() {
        WorldSession session = service.create(OWNER, 7L, SMALL);
        service.advanceSeason(OWNER, session.id());

        assertThat(service.listSaves(OWNER)).extracting(m -> m.saveId()).contains(WorldService.AUTOSAVE_ID);
        WorldSession restored = service.load(OWNER, WorldService.AUTOSAVE_ID);
        assertThat(restored.world().snapshot()).isEqualTo(session.world().snapshot());
    }

    @Test
    void aPlayerCareerSurvivesTheDisk() {
        WorldSession session = service.create(OWNER, 4L, SMALL);
        String golferId = session.world().activeGolferIds().get(0);
        service.assignPlayer(OWNER, session.id(), golferId);
        service.setDevelopmentFocus(OWNER, session.id(), List.of(Attribute.DRIVING_DISTANCE));
        service.setCareerGoals(OWNER, session.id(), List.of(new CareerGoal(GoalType.WIN_A_MAJOR, 1)));
        service.advanceSeason(OWNER, session.id());

        WorldSession loaded = service.load(OWNER, WorldService.AUTOSAVE_ID);
        assertThat(loaded.world().playerGolferId()).contains(golferId);
        assertThat(loaded.world().snapshot()).isEqualTo(session.world().snapshot());
    }

    @Test
    void anActiveEquipmentDealSurvivesTheDisk() {
        WorldSession session = service.create(OWNER, 9L, SMALL);
        String golferId = session.world().activeGolferIds().get(0);
        service.assignPlayer(OWNER, session.id(), golferId);
        service.advanceSeason(OWNER, session.id()); // free agent → brand-deal offers
        assertThat(session.world().pendingEquipmentDeals()).isNotEmpty();
        service.acceptEquipmentDeal(OWNER, session.id(), 0); // sign a deal (active + gear)
        service.save(OWNER, session.id(), "deal-slot");

        WorldSession loaded = service.load(OWNER, "deal-slot");
        assertThat(loaded.world().activeEquipmentDeal())
                .as("the active brand deal survives save/load")
                .isEqualTo(session.world().activeEquipmentDeal());
        assertThat(loaded.world().snapshot()).isEqualTo(session.world().snapshot());
    }

    @Test
    void savesAreIndependentAndDeletable() {
        WorldSession a = service.create(OWNER, 1L, SMALL);
        WorldSession b = service.create(OWNER, 2L, SMALL);
        service.save(OWNER, a.id(), "a");
        service.save(OWNER, b.id(), "b");

        assertThat(service.listSaves(OWNER)).extracting(m -> m.saveId()).contains("a", "b");
        assertThat(service.load(OWNER, "a").world().currentSeason()).isEqualTo(1);
        service.deleteSave(OWNER, "a");
        assertThat(service.listSaves(OWNER)).extracting(m -> m.saveId()).doesNotContain("a").contains("b");
    }
}
