package com.progolf.app.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.progolf.app.persistence.FilesystemSaveGameStore;
import com.progolf.app.persistence.SaveConflictException;
import com.progolf.app.persistence.SaveEnvelope;
import com.progolf.app.persistence.SaveGame;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.progression.PreparationFocus;
import com.progolf.sim.world.WorldConfig;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldAutosaveGenerationTest {
    private static final String OWNER = "checkpoint-owner";
    private static final WorldConfig SMALL = new WorldConfig(40, 6, 3, 20, 4);
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir Path dir;

    @Test
    void successiveCheckpointsAndRecoveryAdvanceOneLineage() {
        WorldService service = service();
        WorldSession original = newCareer(service, OWNER, 401L, "career-a");
        service.save(OWNER, original.id(), WorldService.AUTOSAVE_ID);
        assertThat(service.get(OWNER, original.id()).autosaveGeneration()).isEqualTo(1);
        service.setStandingPreparationFocus(OWNER, original.id(), PreparationFocus.PUTTING);
        service.save(OWNER, original.id(), WorldService.AUTOSAVE_ID);
        assertThat(service.get(OWNER, original.id()).autosaveGeneration()).isEqualTo(2);

        WorldService restarted = service();
        WorldSession recovered = restarted.load(OWNER, WorldService.AUTOSAVE_ID);
        assertThat(recovered.autosaveGeneration()).isEqualTo(2);
        assertThat(recovered.world().playerPreparation().standingFocus()).isEqualTo(PreparationFocus.PUTTING);
        String newNamed = restarted.saveCareer(OWNER, recovered.id()).saveId();
        assertThat(service().load(OWNER, newNamed).careerLineageId()).isEqualTo(original.careerLineageId());
        restarted.save(OWNER, recovered.id(), WorldService.AUTOSAVE_ID);
        assertThat(restarted.get(OWNER, recovered.id()).autosaveGeneration()).isEqualTo(3);
        assertThat(service().load(OWNER, WorldService.AUTOSAVE_ID).autosaveGeneration()).isEqualTo(3);
    }

    @Test
    void sameWeekDivergenceRejectsTheSecondIndependentSession() {
        WorldService service = service();
        newCareer(service, OWNER, 402L, "shared");
        WorldSession first = service.load(OWNER, "shared");
        WorldSession stale = service.load(OWNER, "shared");
        service.setStandingPreparationFocus(OWNER, first.id(), PreparationFocus.PUTTING);
        service.setStandingPreparationFocus(OWNER, stale.id(), PreparationFocus.WEDGES);
        assertThat(service.status(OWNER, first.id()).week()).isEqualTo(service.status(OWNER, stale.id()).week());

        service.save(OWNER, first.id(), WorldService.AUTOSAVE_ID);
        assertThatThrownBy(() -> service.save(OWNER, stale.id(), WorldService.AUTOSAVE_ID))
                .isInstanceOf(SaveConflictException.class);
        assertThat(service.get(OWNER, stale.id()).autosaveGeneration()).isZero();
        assertThat(service().load(OWNER, WorldService.AUTOSAVE_ID).world().snapshot())
                .isEqualTo(first.world().snapshot());
    }

    @Test
    void aSaveAsCopyRetainsTheLineageAndCannotOverwriteItsNewerSourceCheckpoint() {
        WorldService service = service();
        WorldSession source = newCareer(service, OWNER, 408L, "career-original");
        service.save(OWNER, source.id(), "career-copy");
        WorldSession copied = service.load(OWNER, "career-copy");
        assertThat(copied.careerLineageId()).isEqualTo(source.careerLineageId());
        service.save(OWNER, source.id(), WorldService.AUTOSAVE_ID);

        assertThatThrownBy(() -> service.save(OWNER, copied.id(), WorldService.AUTOSAVE_ID))
                .isInstanceOf(SaveConflictException.class);
        assertThat(service().load(OWNER, WorldService.AUTOSAVE_ID).world().snapshot())
                .isEqualTo(source.world().snapshot());
    }

    @Test
    void oldSessionStaysStaleAfterAnotherCareerTakesTheSlot() {
        WorldService service = service();
        WorldSession first = newCareer(service, OWNER, 403L, "career-a");
        WorldSession stale = service.load(OWNER, "career-a");
        WorldSession second = newCareer(service, OWNER, 404L, "career-b");
        service.save(OWNER, first.id(), WorldService.AUTOSAVE_ID);
        service.save(OWNER, second.id(), WorldService.AUTOSAVE_ID);
        assertThat(service().load(OWNER, WorldService.AUTOSAVE_ID).careerLineageId())
                .isEqualTo(second.careerLineageId());

        assertThatThrownBy(() -> service.save(OWNER, stale.id(), WorldService.AUTOSAVE_ID))
                .isInstanceOf(SaveConflictException.class);
        assertThat(service().load(OWNER, WorldService.AUTOSAVE_ID).world().snapshot())
                .isEqualTo(second.world().snapshot());
        service.save(OWNER, first.id(), WorldService.AUTOSAVE_ID); // current A can intentionally retake the slot
        WorldSession afterRestart = service().load(OWNER, WorldService.AUTOSAVE_ID);
        assertThat(afterRestart.careerLineageId()).isEqualTo(first.careerLineageId());
        assertThat(afterRestart.autosaveGeneration()).isEqualTo(2);
        assertThat(new FilesystemSaveGameStore(dir.toString()).load(OWNER, WorldService.AUTOSAVE_ID).autosaveHeads())
                .containsEntry(first.careerLineageId(), 2L).containsEntry(second.careerLineageId(), 1L);
    }

    @Test
    void simultaneousSameLineageWritersProduceOneCheckpointAndOneConflict() throws Exception {
        BarrierStore store = new BarrierStore(dir);
        WorldService service = new WorldService(store);
        newCareer(service, OWNER, 405L, "shared");
        WorldSession first = service.load(OWNER, "shared");
        WorldSession second = service.load(OWNER, "shared");
        service.setStandingPreparationFocus(OWNER, first.id(), PreparationFocus.PUTTING);
        service.setStandingPreparationFocus(OWNER, second.id(), PreparationFocus.WEDGES);

        try (var workers = Executors.newFixedThreadPool(2)) {
            var left = workers.submit(() -> attemptCheckpoint(service, first.id()));
            var right = workers.submit(() -> attemptCheckpoint(service, second.id()));
            assertThat(store.entered.await(10, TimeUnit.SECONDS)).isTrue();
            store.release.countDown();
            assertThat(List.of(left.get(10, TimeUnit.SECONDS), right.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("saved", "conflict");
        } finally {
            store.release.countDown();
        }
        SaveGame persisted = new FilesystemSaveGameStore(dir.toString()).load(OWNER, WorldService.AUTOSAVE_ID);
        assertThat(persisted.autosaveGeneration()).isEqualTo(1);
        assertThat(persisted.snapshot()).isIn(first.world().snapshot(), second.world().snapshot());
    }

    @Test
    void ambiguousLegacyAutosaveRemainsUntilTheOwnerExplicitlyDeletesIt() throws Exception {
        WorldService service = service();
        WorldSession named = newCareer(service, OWNER, 406L, "legacy-named");
        service.save(OWNER, named.id(), WorldService.AUTOSAVE_ID);
        Path legacyFile = makeLegacyAutosave();
        byte[] recoveryBefore = Files.readAllBytes(legacyFile);
        WorldService restarted = service();
        WorldSession namedAgain = restarted.load(OWNER, "legacy-named");
        WorldSession legacyRecovery = restarted.load(OWNER, WorldService.AUTOSAVE_ID);
        assertThat(legacyRecovery.careerLineageId()).startsWith("legacy-autosave-");
        assertThat(legacyRecovery.careerLineageId()).isNotEqualTo(namedAgain.careerLineageId());
        assertThatThrownBy(() -> restarted.save(OWNER, namedAgain.id(), WorldService.AUTOSAVE_ID))
                .isInstanceOf(SaveConflictException.class);
        assertThat(Files.readAllBytes(legacyFile)).isEqualTo(recoveryBefore);
        restarted.saveCareer(OWNER, namedAgain.id()); // named career remains durably saveable
        restarted.save(OWNER, legacyRecovery.id(), WorldService.AUTOSAVE_ID); // recovery can migrate itself
        assertThatThrownBy(() -> restarted.save(OWNER, namedAgain.id(), WorldService.AUTOSAVE_ID))
                .isInstanceOf(SaveConflictException.class); // ambiguity survives that migration
        restarted.deleteSave(OWNER, WorldService.AUTOSAVE_ID); // existing explicit owner action
        restarted.save(OWNER, namedAgain.id(), WorldService.AUTOSAVE_ID);
        assertThat(service().load(OWNER, WorldService.AUTOSAVE_ID).careerLineageId())
                .isEqualTo(namedAgain.careerLineageId());
    }

    @Test
    void eachOwnerHasAnIndependentAutosaveHistory() {
        WorldService service = service();
        WorldSession alice = newCareer(service, "alice", 407L, "career");
        WorldSession bob = newCareer(service, "bob", 407L, "career");
        service.save("alice", alice.id(), WorldService.AUTOSAVE_ID);
        service.save("bob", bob.id(), WorldService.AUTOSAVE_ID);
        assertThat(service().load("alice", WorldService.AUTOSAVE_ID).careerLineageId())
                .isEqualTo(alice.careerLineageId());
        assertThat(service().load("bob", WorldService.AUTOSAVE_ID).careerLineageId())
                .isEqualTo(bob.careerLineageId());
    }

    private String attemptCheckpoint(WorldService service, String id) {
        try {
            service.save(OWNER, id, WorldService.AUTOSAVE_ID);
            return "saved";
        } catch (SaveConflictException conflict) {
            return "conflict";
        }
    }

    private WorldService service() {
        return new WorldService(new FilesystemSaveGameStore(dir.toString()));
    }

    private WorldSession newCareer(WorldService service, String owner, long seed, String saveId) {
        WorldSession session = service.create(owner, seed, SMALL);
        service.createPlayer(owner, session.id(), "Same", "Golfer", Nationality.USA, 18,
                Archetype.ALL_ROUNDER);
        service.setResting(owner, session.id(), true);
        service.save(owner, session.id(), saveId);
        return session;
    }

    private Path makeLegacyAutosave() throws Exception {
        Path file = dir.resolve(OWNER).resolve("autosave.json");
        SaveEnvelope envelope = JSON.readValue(Files.readString(file), SaveEnvelope.class);
        ObjectNode payload = (ObjectNode) JSON.readTree(envelope.payload());
        payload.remove("careerLineageId");
        payload.remove("autosaveGeneration");
        payload.remove("autosaveHeads");
        String oldPayload = JSON.writeValueAsString(payload);
        String checksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(oldPayload.getBytes(StandardCharsets.UTF_8)));
        Files.writeString(file, JSON.writeValueAsString(new SaveEnvelope(3, checksum, oldPayload)));
        return file;
    }

    private static final class BarrierStore extends FilesystemSaveGameStore {
        final CountDownLatch entered = new CountDownLatch(2);
        final CountDownLatch release = new CountDownLatch(1);

        BarrierStore(Path dir) {
            super(dir.toString());
        }

        @Override
        public String saveIfUnchanged(String ownerId, String saveId, String expectedRevision, SaveGame game) {
            if (WorldService.AUTOSAVE_ID.equals(saveId)) {
                entered.countDown();
                try {
                    if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("test release timed out");
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(interrupted);
                }
            }
            return super.saveIfUnchanged(ownerId, saveId, expectedRevision, game);
        }
    }
}
