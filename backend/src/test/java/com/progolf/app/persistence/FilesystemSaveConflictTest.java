package com.progolf.app.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.progolf.sim.world.World;
import com.progolf.sim.world.WorldConfig;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FilesystemSaveConflictTest {

    private static final WorldConfig SMALL = new WorldConfig(40, 6, 3, 20, 4);
    private static final String OWNER = "owner";

    @TempDir
    Path dir;

    private static SaveGame game(World world, long seed, Instant savedAt) {
        return new SaveGame(seed, SMALL, world.snapshot(),
                new SaveMetadata("career-one", savedAt, world.currentSeason(), world.currentWeek(), null), null, null);
    }

    @Test
    void compareAndReplaceRejectsAStaleRevisionWithoutChangingTheDurableSave() {
        var first = new FilesystemSaveGameStore(dir.toString());
        var second = new FilesystemSaveGameStore(dir.toString());
        World world = World.create(31L, SMALL);
        SaveGame original = game(world, 31L, Instant.parse("2026-01-01T00:00:00Z"));
        String revision = first.save(OWNER, "career-one", original);
        assertThat(second.loadVersioned(OWNER, "career-one").revision()).isEqualTo(revision);

        SaveGame newer = game(world, 31L, Instant.parse("2026-01-02T00:00:00Z"));
        String newerRevision = second.saveIfUnchanged(OWNER, "career-one", revision, newer);
        assertThat(newerRevision).isNotEqualTo(revision);
        assertThatThrownBy(() -> first.saveIfUnchanged(OWNER, "career-one", revision, original))
                .isInstanceOf(SaveConflictException.class);
        assertThat(first.loadVersioned(OWNER, "career-one").revision()).isEqualTo(newerRevision);
        assertThat(first.load(OWNER, "career-one").metadata().savedAt()).isEqualTo(newer.metadata().savedAt());
    }

    @Test
    void simultaneousWritersAcrossStoreInstancesAllowOnlyOneReplacement() throws Exception {
        var first = new FilesystemSaveGameStore(dir.toString());
        var second = new FilesystemSaveGameStore(dir.toString());
        World world = World.create(32L, SMALL);
        String revision = first.save(OWNER, "career-one", game(world, 32L, Instant.parse("2026-01-01T00:00:00Z")));
        SaveGame left = game(world, 32L, Instant.parse("2026-01-02T00:00:00Z"));
        SaveGame right = game(world, 32L, Instant.parse("2026-01-03T00:00:00Z"));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<String> leftResult = executor.submit(() -> race(first, revision, left, ready, start));
            Future<String> rightResult = executor.submit(() -> race(second, revision, right, ready, start));
            ready.await();
            start.countDown();
            assertThat(List.of(leftResult.get(), rightResult.get())).containsExactlyInAnyOrder("saved", "conflict");
        }
        assertThat(first.load(OWNER, "career-one").metadata().savedAt())
                .isIn(left.metadata().savedAt(), right.metadata().savedAt());
    }

    private static String race(FilesystemSaveGameStore store, String revision, SaveGame game,
                               CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            store.saveIfUnchanged(OWNER, "career-one", revision, game);
            return "saved";
        } catch (SaveConflictException conflict) {
            return "conflict";
        }
    }

    @Test
    void createOnlyNeverOverwritesAnExistingSlot() {
        var store = new FilesystemSaveGameStore(dir.toString());
        World world = World.create(33L, SMALL);
        SaveGame original = game(world, 33L, Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(store.saveIfUnchanged(OWNER, "career-one", null, original)).isNotBlank();
        assertThatThrownBy(() -> store.saveIfUnchanged(OWNER, "career-one", null, original))
                .isInstanceOf(SaveConflictException.class);
        assertThat(store.load(OWNER, "career-one").metadata().savedAt()).isEqualTo(original.metadata().savedAt());
    }
}
