package com.progolf.app.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.app.persistence.FilesystemSaveGameStore;
import com.progolf.app.persistence.SaveMetadata;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.world.WorldConfig;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldSaveProvenanceTest {

    private static final WorldConfig SMALL = new WorldConfig(40, 6, 3, 20, 4);
    private static final String OWNER = "career-owner";

    @TempDir
    Path dir;
    private WorldService service;

    @BeforeEach
    void setUp() {
        service = new WorldService(new FilesystemSaveGameStore(dir.toString()));
    }

    private String createCareer(String owner, long seed, String saveId) {
        WorldSession session = service.create(owner, seed, SMALL);
        service.createPlayer(owner, session.id(), "Career", "Golfer", Nationality.USA, 18,
                Archetype.ALL_ROUNDER);
        service.save(owner, session.id(), saveId);
        return session.id();
    }

    @Test
    void newCareerBindsOnlyAfterItsFirstSuccessfulNamedSave() {
        WorldSession session = service.create(OWNER, 11L, SMALL);
        assertThat(service.get(OWNER, session.id()).originSaveId()).isNull();
        assertThat(service.originalCareerSaveId(OWNER, session.id())).isEmpty();

        service.createPlayer(OWNER, session.id(), "New", "Golfer", Nationality.USA, 18,
                Archetype.ALL_ROUNDER);
        service.save(OWNER, session.id(), "career-new");
        assertThat(service.get(OWNER, session.id()).originSaveId()).isEqualTo("career-new");
        assertThat(service.originalCareerSaveId(OWNER, session.id())).contains("career-new");

        service.save(OWNER, session.id(), "other-slot");
        assertThat(service.get(OWNER, session.id()).originSaveId()).isEqualTo("career-new");
    }

    @Test
    void existingSaveLoadsIntoAProvenanceAwareSessionWithoutWritingAnything() {
        String originalId = createCareer(OWNER, 12L, "career-existing");
        List<SaveMetadata> before = service.listSaves(OWNER);
        WorldService restarted = new WorldService(new FilesystemSaveGameStore(dir.toString()));

        WorldSession resumed = restarted.load(OWNER, "career-existing");
        assertThat(resumed.id()).isNotEqualTo(originalId);
        assertThat(resumed.originSaveId()).isEqualTo("career-existing");
        assertThat(restarted.get(OWNER, resumed.id()).originSaveId()).isEqualTo("career-existing");
        assertThat(restarted.originalCareerSaveId(OWNER, resumed.id())).contains("career-existing");
        assertThat(resumed.world().snapshot()).isEqualTo(service.get(OWNER, originalId).world().snapshot());
        assertThat(restarted.listSaves(OWNER)).isEqualTo(before);
    }

    @Test
    void distinctCareersKeepDistinctOriginsAndAutosaveDoesNotRebindThem() {
        String first = createCareer(OWNER, 13L, "career-first");
        String second = createCareer(OWNER, 14L, "career-second");
        service.advanceSeason(OWNER, first);

        assertThat(service.originalCareerSaveId(OWNER, first)).contains("career-first");
        assertThat(service.originalCareerSaveId(OWNER, second)).contains("career-second");
        assertThat(service.get(OWNER, first).originSaveId()).isEqualTo("career-first");
        assertThat(service.get(OWNER, second).originSaveId()).isEqualTo("career-second");
        assertThat(service.listSaves(OWNER)).extracting(SaveMetadata::saveId)
                .contains("career-first", "career-second", WorldService.AUTOSAVE_ID);
    }

    @Test
    void missingOrReplacedOriginNeverResolvesAsTheOriginalCareer() {
        String first = createCareer(OWNER, 15L, "career-first");
        service.deleteSave(OWNER, "career-first");
        assertThat(service.originalCareerSaveId(OWNER, first)).isEmpty();

        String second = createCareer(OWNER, 16L, "career-second");
        service.save(OWNER, second, "career-first");
        assertThat(service.originalCareerSaveId(OWNER, first)).isEmpty();
        assertThat(service.originalCareerSaveId(OWNER, second)).contains("career-second");
    }

    @Test
    void autosaveOriginIsRecordedButCannotBeTreatedAsANamedCareer() {
        String original = createCareer(OWNER, 17L, "career-original");
        service.advanceSeason(OWNER, original);
        WorldSession resumed = service.load(OWNER, WorldService.AUTOSAVE_ID);

        assertThat(resumed.originSaveId()).isEqualTo(WorldService.AUTOSAVE_ID);
        assertThat(service.originalCareerSaveId(OWNER, resumed.id())).isEmpty();
        assertThat(service.originalCareerSaveId(OWNER, original)).contains("career-original");
    }

    @Test
    void ownerChecksAndSingleFilenameSaveIdsPreventCrossAccountProvenance() {
        String alice = createCareer("alice", 18L, "career-shared");
        String bob = createCareer("bob", 19L, "career-shared");

        assertThat(service.originalCareerSaveId("alice", alice)).contains("career-shared");
        assertThat(service.originalCareerSaveId("bob", bob)).contains("career-shared");
        assertThatThrownBy(() -> service.originalCareerSaveId("bob", alice))
                .isInstanceOf(WorldSessionNotFoundException.class);
        assertThatThrownBy(() -> service.load("alice", "../bob/career-shared"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.save("alice", alice, "../bob/career-shared"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(service.originalCareerSaveId("alice", alice)).contains("career-shared");
        assertThat(service.get("bob", bob).world().snapshot())
                .isEqualTo(service.load("bob", "career-shared").world().snapshot());
    }

    @Test
    void pendingEventStillRejectsSaveWithoutRebindingProvenanceOrOverwritingASlot() {
        WorldSession session = service.create(OWNER, 20L, SMALL);
        service.createPlayer(OWNER, session.id(), "Pending", "Golfer", Nationality.USA, 18,
                Archetype.ALL_ROUNDER);
        service.save(OWNER, session.id(), "career-before-event");
        List<SaveMetadata> before = service.listSaves(OWNER);

        int weeks = 0;
        while (!service.hasPendingEvent(OWNER, session.id()) && weeks++ < 40) {
            service.advanceWeek(OWNER, session.id());
        }
        assertThat(service.hasPendingEvent(OWNER, session.id())).isTrue();
        assertThatThrownBy(() -> service.save(OWNER, session.id(), "career-during-event"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot snapshot while a player event is pending");
        assertThat(service.get(OWNER, session.id()).originSaveId()).isEqualTo("career-before-event");
        assertThat(service.listSaves(OWNER)).isEqualTo(before);
    }
}
