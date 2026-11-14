package com.progolf.app.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.progolf.app.persistence.FilesystemSaveGameStore;
import com.progolf.app.persistence.SaveConflictException;
import com.progolf.app.persistence.SaveGame;
import com.progolf.sim.world.World;
import com.progolf.sim.world.WorldConfig;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

/** Controlled world seam exercises checkpoint outcomes without simulating a season or a tournament. */
class WorldUnsavedCheckpointOutcomeTest {
    private static final String OWNER = "outcome-owner";
    private static final WorldConfig SMALL = new WorldConfig(40, 6, 3, 20, 4);

    @TempDir Path dir;

    @Test
    void completedSeasonWithRejectedAutosaveIsReconciledAsCompletedUnsaved() {
        WorldService service = new WorldService(new RejectAutosaveStore(dir));
        AtomicInteger season = new AtomicInteger(1);
        World world = controlledWorld(season);
        doAnswer(invocation -> { season.incrementAndGet(); return null; }).when(world).advanceSeason();
        String id = attach(service, world);

        SeasonAdvanceOperation result = service.advanceSeason(OWNER, id, "request-one", 1);
        assertThat(result.state()).isEqualTo(SeasonAdvanceOperation.State.COMPLETED_UNSAVED);
        assertThat(result.completedSeason()).isEqualTo(2);
        assertThat(service.seasonAdvanceOperation(OWNER, id)).isEqualTo(result);
        assertThat(service.advanceSeason(OWNER, id, "request-one", 1)).isEqualTo(result);
        assertThatThrownBy(() -> service.advanceSeason(OWNER, id, "unsafe-retry", 2))
                .isInstanceOf(IllegalStateException.class);
        assertThat(season.get()).isEqualTo(2);
        verify(world).advanceSeason();
        assertThat(new FilesystemSaveGameStore(dir.toString()).exists(OWNER, WorldService.AUTOSAVE_ID)).isFalse();
    }

    @Test
    void completedPlayerEventReportsUnsavedCheckpointWithoutInvitingAReplay() {
        RejectAutosaveStore store = new RejectAutosaveStore(dir);
        WorldService service = new WorldService(store);
        World world = controlledWorld(new AtomicInteger(1));
        AtomicInteger completions = new AtomicInteger();
        doAnswer(invocation -> {
            if (completions.getAndIncrement() > 0) {
                throw new IllegalStateException("No player event is pending");
            }
            return null;
        }).when(world).completePlayerEvent();
        String id = attach(service, world);

        assertThatThrownBy(() -> service.completeEvent(OWNER, id))
                .isInstanceOf(CompletedWithoutAutosaveException.class)
                .hasMessageContaining("Player event completed")
                .hasCauseInstanceOf(SaveConflictException.class);
        int checkpointAttempts = store.autosaveAttempts.get();
        assertThat(checkpointAttempts).isPositive();
        assertThatThrownBy(() -> service.completeEvent(OWNER, id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No player event is pending");
        assertThat(store.autosaveAttempts.get()).isEqualTo(checkpointAttempts);
        assertThat(completions.get()).isEqualTo(2);
        assertThat(new FilesystemSaveGameStore(dir.toString()).exists(OWNER, WorldService.AUTOSAVE_ID)).isFalse();
    }

    private static World controlledWorld(AtomicInteger season) {
        World world = mock(World.class);
        when(world.currentSeason()).thenAnswer(ignored -> season.get());
        when(world.config()).thenReturn(SMALL);
        when(world.snapshot()).thenReturn(World.create(501L, SMALL).snapshot());
        when(world.playerGolferId()).thenReturn(Optional.empty());
        return world;
    }

    @SuppressWarnings("unchecked")
    private static String attach(WorldService service, World world) {
        String id = "controlled-session";
        var sessions = (ConcurrentMap<String, WorldSession>) ReflectionTestUtils.getField(service, "sessions");
        sessions.put(id, new WorldSession(id, OWNER, 501L, world));
        return id;
    }

    private static final class RejectAutosaveStore extends FilesystemSaveGameStore {
        private final AtomicInteger autosaveAttempts = new AtomicInteger();

        RejectAutosaveStore(Path dir) {
            super(dir.toString());
        }

        @Override
        public String saveIfUnchanged(String ownerId, String saveId, String expectedRevision, SaveGame game) {
            if (WorldService.AUTOSAVE_ID.equals(saveId)) {
                autosaveAttempts.incrementAndGet();
                throw new SaveConflictException(saveId);
            }
            return super.saveIfUnchanged(ownerId, saveId, expectedRevision, game);
        }
    }
}
