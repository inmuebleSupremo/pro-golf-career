package com.progolf.app.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.app.api.dto.WorldStatusDto;
import com.progolf.app.persistence.FilesystemSaveGameStore;
import com.progolf.app.persistence.SaveGame;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.world.WorldConfig;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldSeasonAdvanceOperationTest {
    private static final String OWNER = "season-owner";
    private static final WorldConfig SMALL = new WorldConfig(40, 6, 3, 20, 4);

    @TempDir Path dir;

    @Test
    void successfulAttemptCanBeReconciledAndReplayedWithoutAdvancingAnotherSeason() {
        WorldService service = new WorldService(new FilesystemSaveGameStore(dir.toString()));
        String id = service.create(OWNER, 31L, SMALL).id();
        assertThat(service.seasonAdvanceOperation(OWNER, id)).isNull();

        SeasonAdvanceOperation result = service.advanceSeason(OWNER, id, "request-one", 1);
        assertThat(result.state()).isEqualTo(SeasonAdvanceOperation.State.COMPLETED);
        assertThat(result.completedSeason()).isEqualTo(2);
        assertThat(service.seasonAdvanceOperation(OWNER, id)).isEqualTo(result);
        assertThat(service.advanceSeason(OWNER, id, "request-one", 1)).isEqualTo(result);
        assertThat(service.status(OWNER, id).season()).isEqualTo(2);
        assertThatThrownBy(() -> service.advanceSeason(OWNER, id, "request-two", 1))
                .isInstanceOf(IllegalStateException.class);
        assertThat(service.status(OWNER, id).season()).isEqualTo(2);

        assertThat(service.advanceSeason(OWNER, id, "intentional-next-season", 2).completedSeason()).isEqualTo(3);
        assertThatThrownBy(() -> service.advanceSeason(OWNER, id, "request-one", 1))
                .isInstanceOf(IllegalStateException.class);
        assertThat(service.status(OWNER, id).season()).isEqualTo(3);
    }

    @Test
    void runningAttemptIsVisibleAndBlocksAnotherRequestWithoutWaitingForIt() throws Exception {
        ControlledStore store = new ControlledStore(dir);
        WorldService service = new WorldService(store);
        String id = service.create(OWNER, 32L, SMALL).id();
        try (var pool = Executors.newSingleThreadExecutor()) {
            var first = pool.submit(() -> service.advanceSeason(OWNER, id, "running-request", 1));
            assertThat(store.entered.await(30, TimeUnit.SECONDS)).isTrue();
            assertThat(service.seasonAdvanceOperation(OWNER, id).state())
                    .isEqualTo(SeasonAdvanceOperation.State.RUNNING);
            assertThat(service.advanceSeason(OWNER, id, "running-request", 1).state())
                    .isEqualTo(SeasonAdvanceOperation.State.RUNNING);
            assertThatThrownBy(() -> service.advanceSeason(OWNER, id, "overlap", 1))
                    .isInstanceOf(IllegalStateException.class);
            store.release.countDown();
            assertThat(first.get(30, TimeUnit.SECONDS).state()).isEqualTo(SeasonAdvanceOperation.State.COMPLETED);
            assertThat(service.seasonAdvanceOperation(OWNER, id).completedSeason()).isEqualTo(2);
        } finally {
            store.release.countDown();
        }
    }

    @Test
    void advanceWeekSubmittedDuringSeasonSimulationIsRejectedBeforeTheSeasonCompletes() throws Exception {
        ControlledStore store = new ControlledStore(dir);
        WorldService service = new WorldService(store);
        String id = service.create(OWNER, 35L, SMALL).id();
        service.createPlayer(OWNER, id, "Week", "Guard", Nationality.USA, 18, Archetype.ALL_ROUNDER);
        service.save(OWNER, id, "career-before-season");
        SaveGame namedBefore = store.load(OWNER, "career-before-season");
        String otherId = service.create(OWNER, 36L, SMALL).id();

        try (var pool = Executors.newFixedThreadPool(2)) {
            var season = pool.submit(() -> service.advanceSeason(OWNER, id, "guarded-season", 1));
            try {
                // The world has completed its season but still holds its lock while the autosave is paused.
                assertThat(store.entered.await(30, TimeUnit.SECONDS)).isTrue();
                WorldStatusDto paused = service.status(OWNER, id);
                assertThat(paused.season()).isEqualTo(2);
                assertThat(paused.week()).isEqualTo(1);
                assertThat(paused.hasPendingEvent()).isFalse();
                assertThat(service.seasonAdvanceOperation(OWNER, id).state())
                        .isEqualTo(SeasonAdvanceOperation.State.RUNNING);

                var week = pool.submit(() -> {
                    try {
                        service.advanceWeek(OWNER, id);
                        return (Throwable) null;
                    } catch (Throwable failure) {
                        return failure;
                    }
                });
                // Resolve the week attempt while the season is still held. A queued request cannot pass.
                assertThat(week.get(10, TimeUnit.SECONDS))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("season simulation");
                assertThat(service.status(OWNER, id)).isEqualTo(paused);
                assertThat(store.load(OWNER, "career-before-season")).isEqualTo(namedBefore);
                assertThat(service.listSaves(OWNER)).extracting(save -> save.saveId())
                        .containsExactly("career-before-season");

                // The operation guard is scoped to the session, not the owner's other worlds.
                service.advanceWeek(OWNER, otherId);
                assertThat(service.status(OWNER, otherId).week()).isEqualTo(2);
            } finally {
                store.release.countDown();
            }

            SeasonAdvanceOperation completed = season.get(30, TimeUnit.SECONDS);
            assertThat(completed.state()).isEqualTo(SeasonAdvanceOperation.State.COMPLETED);
            assertThat(completed.completedSeason()).isEqualTo(2);
            assertThat(service.seasonAdvanceOperation(OWNER, id)).isEqualTo(completed);
            assertThat(service.status(OWNER, id).week()).isEqualTo(1);
            assertThat(service.status(OWNER, id).hasPendingEvent()).isFalse();
            assertThat(service.load(OWNER, WorldService.AUTOSAVE_ID).world().snapshot())
                    .isEqualTo(service.get(OWNER, id).world().snapshot());
            assertThat(store.load(OWNER, "career-before-season")).isEqualTo(namedBefore);

            // A fresh request for the same session after completion remains ordinary week progression.
            service.advanceWeek(OWNER, id);
            WorldStatusDto afterWeek = service.status(OWNER, id);
            assertThat(afterWeek.season()).isEqualTo(2);
            assertThat(afterWeek.week() > 1 || afterWeek.hasPendingEvent()).isTrue();
        }
    }

    @Test
    void unsavedCompletionAfterProgressIsRecordedAndBlocksUnsafeReplay() {
        ControlledStore store = new ControlledStore(dir);
        store.failAutosave = true;
        WorldService service = new WorldService(store);
        String id = service.create(OWNER, 33L, SMALL).id();

        SeasonAdvanceOperation unsaved = service.advanceSeason(OWNER, id, "failed-request", 1);
        assertThat(unsaved.completedSeason()).isEqualTo(2);
        assertThat(service.status(OWNER, id).season()).isEqualTo(2); // in-memory progress was not rolled back
        assertThat(service.seasonAdvanceOperation(OWNER, id).state())
                .isEqualTo(SeasonAdvanceOperation.State.COMPLETED_UNSAVED);
        assertThat(service.advanceSeason(OWNER, id, "failed-request", 1).state())
                .isEqualTo(SeasonAdvanceOperation.State.COMPLETED_UNSAVED);
        assertThatThrownBy(() -> service.advanceSeason(OWNER, id, "unsafe-retry", 2))
                .isInstanceOf(IllegalStateException.class);
        assertThat(service.status(OWNER, id).season()).isEqualTo(2);
    }

    @Test
    void operationLookupRespectsOwnershipAndMissingSessionsStayUnknown() {
        WorldService service = new WorldService(new FilesystemSaveGameStore(dir.toString()));
        String id = service.create(OWNER, 34L, SMALL).id();
        assertThatThrownBy(() -> service.seasonAdvanceOperation("other-owner", id))
                .isInstanceOf(WorldSessionNotFoundException.class);
        WorldService restarted = new WorldService(new FilesystemSaveGameStore(dir.toString()));
        assertThatThrownBy(() -> restarted.seasonAdvanceOperation(OWNER, id))
                .isInstanceOf(WorldSessionNotFoundException.class);
    }

    private static final class ControlledStore extends FilesystemSaveGameStore {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        volatile boolean failAutosave;

        ControlledStore(Path directory) {
            super(directory.toString());
        }

        @Override
        public String saveIfUnchanged(String ownerId, String saveId, String expectedRevision, SaveGame game) {
            if (WorldService.AUTOSAVE_ID.equals(saveId)) {
                entered.countDown();
                if (failAutosave) throw new IllegalStateException("autosave failed after simulation");
                try {
                    if (!release.await(30, TimeUnit.SECONDS)) throw new IllegalStateException("test release timed out");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }
            return super.saveIfUnchanged(ownerId, saveId, expectedRevision, game);
        }
    }
}
