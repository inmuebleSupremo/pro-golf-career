package com.progolf.app.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.progolf.app.persistence.FilesystemSaveGameStore;
import com.progolf.app.persistence.SaveGame;
import com.progolf.sim.world.World;
import com.progolf.sim.world.WorldConfig;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

/** Holds one completion inside the world seam so a duplicate request must wait on the session world. */
class WorldConcurrentEventCompletionTest {
    private static final String OWNER = "finish-owner";
    private static final WorldConfig SMALL = new WorldConfig(40, 6, 3, 20, 4);

    @TempDir Path dir;

    @Test
    void concurrentFinishRequestsFeedResultsAndCheckpointOnlyOnce() throws Exception {
        CountingAutosaveStore store = new CountingAutosaveStore(dir);
        WorldService service = new WorldService(store);
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicBoolean pending = new AtomicBoolean(true);
        AtomicInteger completions = new AtomicInteger();
        AtomicInteger careerEvents = new AtomicInteger();
        AtomicInteger earnings = new AtomicInteger();
        AtomicInteger standings = new AtomicInteger();
        World world = controlledWorld();
        doAnswer(invocation -> {
            // Model World's pending-event check followed by its non-idempotent consumer feed.
            boolean observedPending = pending.get();
            int entered = completions.incrementAndGet();
            if (!observedPending) throw new IllegalStateException("No player event is pending");
            if (entered == 1) {
                firstEntered.countDown();
                assertThat(releaseFirst.await(10, TimeUnit.SECONDS)).isTrue();
            }
            careerEvents.incrementAndGet();
            earnings.addAndGet(100);
            standings.addAndGet(10);
            pending.set(false);
            return null;
        }).when(world).completePlayerEvent();
        String id = attach(service, world);

        AtomicReference<Throwable> firstFailure = new AtomicReference<>();
        AtomicReference<Throwable> secondFailure = new AtomicReference<>();
        CountDownLatch secondSubmitted = new CountDownLatch(1);
        Thread first = new Thread(() -> finish(service, id, firstFailure), "first-finish");
        Thread second = new Thread(() -> {
            secondSubmitted.countDown();
            finish(service, id, secondFailure);
        }, "second-finish");
        first.start();
        try {
            assertThat(firstEntered.await(10, TimeUnit.SECONDS)).isTrue();
            second.start();
            assertThat(secondSubmitted.await(10, TimeUnit.SECONDS)).isTrue();
            awaitWorldLock(second, first, world);
            assertThat(completions.get()).isEqualTo(1);
            assertThat(store.autosaveAttempts.get()).isZero();
        } finally {
            releaseFirst.countDown();
            first.join(10_000);
            if (second.isAlive()) second.join(10_000);
        }

        assertThat(first.isAlive()).isFalse();
        assertThat(second.isAlive()).isFalse();
        assertThat(firstFailure.get()).isNull();
        assertThat(secondFailure.get()).isInstanceOf(IllegalStateException.class)
                .hasMessage("No player event is pending");
        assertThat(completions.get()).isEqualTo(2); // the second call only checks the cleared pending event
        assertThat(careerEvents.get()).isEqualTo(1);
        assertThat(earnings.get()).isEqualTo(100);
        assertThat(standings.get()).isEqualTo(10);
        assertThat(store.autosaveAttempts.get()).isEqualTo(1);
        assertThat(store.exists(OWNER, WorldService.AUTOSAVE_ID)).isTrue();
    }

    @Test
    void ordinarySingleFinishStillCheckpoints() {
        CountingAutosaveStore store = new CountingAutosaveStore(dir);
        WorldService service = new WorldService(store);
        AtomicInteger completions = new AtomicInteger();
        World world = controlledWorld();
        doAnswer(invocation -> { completions.incrementAndGet(); return null; })
                .when(world).completePlayerEvent();
        String id = attach(service, world);

        service.completeEvent(OWNER, id);

        assertThat(completions.get()).isEqualTo(1);
        assertThat(store.autosaveAttempts.get()).isEqualTo(1);
        assertThat(store.exists(OWNER, WorldService.AUTOSAVE_ID)).isTrue();
    }

    private static void finish(WorldService service, String id, AtomicReference<Throwable> failure) {
        try {
            service.completeEvent(OWNER, id);
        } catch (Throwable error) {
            failure.set(error);
        }
    }

    private static void awaitWorldLock(Thread waiting, Thread holding, World world) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            var info = ManagementFactory.getThreadMXBean().getThreadInfo(waiting.getId());
            if (info != null && info.getThreadState() == Thread.State.BLOCKED
                    && info.getLockOwnerId() == holding.getId()
                    && info.getLockInfo() != null
                    && info.getLockInfo().getIdentityHashCode() == System.identityHashCode(world)) {
                return;
            }
            if (!waiting.isAlive()) break;
            Thread.onSpinWait();
        }
        throw new AssertionError("The duplicate Finish Event request did not wait on the session world lock");
    }

    private static World controlledWorld() {
        World world = mock(World.class);
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

    private static final class CountingAutosaveStore extends FilesystemSaveGameStore {
        private final AtomicInteger autosaveAttempts = new AtomicInteger();

        CountingAutosaveStore(Path dir) {
            super(dir.toString());
        }

        @Override
        public String saveIfUnchanged(String ownerId, String saveId, String expectedRevision, SaveGame game) {
            if (WorldService.AUTOSAVE_ID.equals(saveId)) autosaveAttempts.incrementAndGet();
            return super.saveIfUnchanged(ownerId, saveId, expectedRevision, game);
        }
    }
}
