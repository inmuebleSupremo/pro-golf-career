package com.progolf.app.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.app.persistence.FilesystemSaveGameStore;
import com.progolf.app.persistence.SaveConflictException;
import com.progolf.app.persistence.SaveMetadata;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.progression.PreparationFocus;
import com.progolf.sim.world.WorldConfig;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldSafeCareerSaveTest {

    private static final WorldConfig SMALL = new WorldConfig(40, 6, 3, 20, 4);
    private static final String OWNER = "career-owner";

    @TempDir
    Path dir;
    private WorldService service;

    @BeforeEach
    void setUp() {
        service = new WorldService(new FilesystemSaveGameStore(dir.toString()));
    }

    private String newCareer(long seed, String saveId) {
        WorldSession session = service.create(OWNER, seed, SMALL);
        service.createPlayer(OWNER, session.id(), "Safe", "Saver", Nationality.USA, 18,
                Archetype.ALL_ROUNDER);
        service.save(OWNER, session.id(), saveId);
        return session.id();
    }

    @Test
    void firstNamedSaveAndLaterExplicitSaveUpdateTheSameCareer() {
        String id = newCareer(41L, "career-original");
        String initialRevision = service.get(OWNER, id).originRevision();
        service.setStandingPreparationFocus(OWNER, id, PreparationFocus.PUTTING);

        SaveMetadata saved = service.saveCareer(OWNER, id);
        assertThat(saved.saveId()).isEqualTo("career-original");
        assertThat(service.get(OWNER, id).originRevision()).isNotEqualTo(initialRevision);
        assertThat(service.listSaves(OWNER)).extracting(SaveMetadata::saveId).containsExactly("career-original");
        assertThat(service.load(OWNER, "career-original").world().snapshot())
                .isEqualTo(service.get(OWNER, id).world().snapshot());

        service.setStandingPreparationFocus(OWNER, id, PreparationFocus.IRONS);
        assertThat(service.saveCareer(OWNER, id).saveId()).isEqualTo("career-original");
        assertThat(service.listSaves(OWNER)).hasSize(1);
    }

    @Test
    void resumedCareerUpdatesItsOriginalSlotWithoutSavingOnOpenOrRead() {
        newCareer(42L, "career-existing");
        List<SaveMetadata> before = service.listSaves(OWNER);
        WorldService restarted = new WorldService(new FilesystemSaveGameStore(dir.toString()));
        WorldSession resumed = restarted.load(OWNER, "career-existing");
        restarted.status(OWNER, resumed.id()); // ordinary page read / reload
        assertThat(restarted.listSaves(OWNER)).isEqualTo(before);

        restarted.setStandingPreparationFocus(OWNER, resumed.id(), PreparationFocus.WEDGES);
        assertThat(restarted.saveCareer(OWNER, resumed.id()).saveId()).isEqualTo("career-existing");
        assertThat(restarted.listSaves(OWNER)).hasSize(1);
        assertThat(restarted.load(OWNER, "career-existing").world().snapshot())
                .isEqualTo(restarted.get(OWNER, resumed.id()).world().snapshot());
    }

    @Test
    void autosaveRecoveryCreatesOneNewNamedCareerThenUpdatesIt() {
        String original = newCareer(43L, "career-original");
        service.advanceSeason(OWNER, original); // existing autosave checkpoint
        WorldSession recovered = service.load(OWNER, WorldService.AUTOSAVE_ID);
        SaveMetadata autosaveBefore = service.listSaves(OWNER).stream()
                .filter(save -> save.saveId().equals(WorldService.AUTOSAVE_ID)).findFirst().orElseThrow();

        SaveMetadata first = service.saveCareer(OWNER, recovered.id());
        assertThat(first.saveId()).startsWith("career-").isNotEqualTo("career-original");
        assertThat(service.get(OWNER, recovered.id()).originSaveId()).isEqualTo(first.saveId());
        assertThat(service.saveCareer(OWNER, recovered.id()).saveId()).isEqualTo(first.saveId());
        assertThat(service.listSaves(OWNER)).extracting(SaveMetadata::saveId)
                .containsExactlyInAnyOrder("career-original", WorldService.AUTOSAVE_ID, first.saveId());
        assertThat(service.listSaves(OWNER).stream().filter(s -> s.saveId().equals(WorldService.AUTOSAVE_ID))
                .findFirst().orElseThrow()).isEqualTo(autosaveBefore);
    }

    @Test
    void staleSessionAndDeletedSourceFailWithoutReplacingProgressOrAdvancingProvenance() {
        newCareer(44L, "career-shared");
        WorldSession first = service.load(OWNER, "career-shared");
        WorldSession stale = service.load(OWNER, "career-shared");
        String staleRevision = stale.originRevision();

        service.setStandingPreparationFocus(OWNER, first.id(), PreparationFocus.PUTTING);
        service.saveCareer(OWNER, first.id());
        var winner = service.load(OWNER, "career-shared").world().snapshot();
        service.setStandingPreparationFocus(OWNER, stale.id(), PreparationFocus.WEDGES);
        assertThatThrownBy(() -> service.saveCareer(OWNER, stale.id()))
                .isInstanceOf(SaveConflictException.class);
        assertThatThrownBy(() -> service.save(OWNER, stale.id(), "career-shared"))
                .isInstanceOf(SaveConflictException.class);
        assertThat(service.get(OWNER, stale.id()).originRevision()).isEqualTo(staleRevision);
        assertThat(service.load(OWNER, "career-shared").world().snapshot()).isEqualTo(winner);

        service.deleteSave(OWNER, "career-shared");
        assertThatThrownBy(() -> service.saveCareer(OWNER, first.id()))
                .isInstanceOf(SaveConflictException.class);
        assertThat(service.listSaves(OWNER)).isEmpty();
    }

    @Test
    void ownerAndMissingProvenanceCannotInvokeSafeSave() {
        String id = newCareer(45L, "career-owned");
        assertThatThrownBy(() -> service.saveCareer("other-owner", id))
                .isInstanceOf(WorldSessionNotFoundException.class);
        WorldSession unsaved = service.create(OWNER, 46L, SMALL);
        assertThatThrownBy(() -> service.saveCareer(OWNER, unsaved.id()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(service.listSaves(OWNER)).extracting(SaveMetadata::saveId).containsExactly("career-owned");
    }

    @Test
    void pendingEventStillCannotBeSavedAndLeavesTheOriginalDurableSaveIntact() {
        String id = newCareer(47L, "career-pending");
        SaveMetadata before = service.listSaves(OWNER).getFirst();
        String revision = service.get(OWNER, id).originRevision();
        int weeks = 0;
        while (!service.hasPendingEvent(OWNER, id) && weeks++ < 40) {
            service.advanceWeek(OWNER, id);
        }
        assertThat(service.hasPendingEvent(OWNER, id)).isTrue();
        assertThatThrownBy(() -> service.saveCareer(OWNER, id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot snapshot while a player event is pending");
        assertThat(service.get(OWNER, id).originRevision()).isEqualTo(revision);
        assertThat(service.listSaves(OWNER)).containsExactly(before);
    }
}
