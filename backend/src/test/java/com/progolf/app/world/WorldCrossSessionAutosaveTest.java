package com.progolf.app.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.app.persistence.FilesystemSaveGameStore;
import com.progolf.app.persistence.SaveConflictException;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.progression.PreparationFocus;
import com.progolf.sim.world.WorldConfig;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Reproduces recovery-slot conflicts without running a full season. */
class WorldCrossSessionAutosaveTest {
    private static final String OWNER = "autosave-owner";
    private static final WorldConfig SMALL = new WorldConfig(40, 6, 3, 20, 4);

    @TempDir Path dir;

    @Test
    void staleSameCareerSessionMustNotReplaceNewerRecoveryAutosave() {
        WorldService service = new WorldService(new FilesystemSaveGameStore(dir.toString()));
        String namedId = "career-shared";
        WorldSession source = createNamedCareer(service, 201L, namedId);
        WorldSession newer = service.load(OWNER, namedId);
        WorldSession stale = service.load(OWNER, namedId);
        assertThat(newer.id()).isNotEqualTo(stale.id());
        assertThat(newer.originSaveId()).isEqualTo(stale.originSaveId());
        assertThat(newer.originRevision()).isEqualTo(stale.originRevision());
        assertThat(source.world().playerGolferId()).isEqualTo(newer.world().playerGolferId());

        service.advanceWeek(OWNER, newer.id());
        service.advanceWeek(OWNER, newer.id());
        service.setStandingPreparationFocus(OWNER, newer.id(), PreparationFocus.PUTTING);
        assertThat(service.status(OWNER, newer.id()).week()).isEqualTo(3);
        // The public checkpoint writer is the same path used by automatic autosave, without a season run.
        service.save(OWNER, newer.id(), WorldService.AUTOSAVE_ID);

        service.advanceWeek(OWNER, stale.id());
        assertThat(service.status(OWNER, stale.id()).week()).isEqualTo(2);
        assertThatThrownBy(() -> service.save(OWNER, stale.id(), WorldService.AUTOSAVE_ID))
                .isInstanceOf(SaveConflictException.class);
        assertThat(service.get(OWNER, stale.id()).autosaveGeneration()).isZero();

        WorldService restarted = new WorldService(new FilesystemSaveGameStore(dir.toString()));
        WorldSession recovered = restarted.load(OWNER, WorldService.AUTOSAVE_ID);
        assertThat(recovered.seed()).isEqualTo(newer.seed());
        assertThat(recovered.world().playerGolferId()).isEqualTo(newer.world().playerGolferId());
        assertThat(recovered.world().currentSeason()).isEqualTo(1);
        assertThat(recovered.world().currentWeek())
                .as("a stale same-career session must not roll back the recovery checkpoint")
                .isEqualTo(3);
        assertThat(recovered.world().playerPreparation().standingFocus()).isEqualTo(PreparationFocus.PUTTING);
        assertThat(recovered.world().snapshot()).isEqualTo(newer.world().snapshot());
    }

    @Test
    void anotherCareerMayReplaceTheOwnersSharedAutosave() {
        WorldService service = new WorldService(new FilesystemSaveGameStore(dir.toString()));
        WorldSession first = createNamedCareer(service, 202L, "career-first");
        WorldSession second = createNamedCareer(service, 203L, "career-second");
        assertThat(first.world().currentSeason()).isEqualTo(second.world().currentSeason());
        assertThat(first.world().currentWeek()).isEqualTo(second.world().currentWeek());
        assertThat(first.world().playerGolferId()).isNotEqualTo(second.world().playerGolferId());

        service.save(OWNER, first.id(), WorldService.AUTOSAVE_ID);
        service.save(OWNER, second.id(), WorldService.AUTOSAVE_ID);

        WorldService restarted = new WorldService(new FilesystemSaveGameStore(dir.toString()));
        WorldSession recovered = restarted.load(OWNER, WorldService.AUTOSAVE_ID);
        assertThat(recovered.seed()).isEqualTo(second.seed());
        assertThat(recovered.world().playerGolferId()).isEqualTo(second.world().playerGolferId());
        assertThat(recovered.world().snapshot()).isEqualTo(second.world().snapshot());
        assertThat(restarted.load(OWNER, "career-first").world().snapshot()).isEqualTo(first.world().snapshot());
        assertThat(restarted.load(OWNER, "career-second").world().snapshot()).isEqualTo(second.world().snapshot());
    }

    private static WorldSession createNamedCareer(WorldService service, long seed, String saveId) {
        WorldSession session = service.create(OWNER, seed, SMALL);
        service.createPlayer(OWNER, session.id(), "Same", "Name", Nationality.USA, 18,
                Archetype.ALL_ROUNDER);
        service.setResting(OWNER, session.id(), true); // no pending player event interrupts the short week fixture
        service.save(OWNER, session.id(), saveId);
        return session;
    }
}
