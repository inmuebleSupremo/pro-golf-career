package com.progolf.app.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.progolf.sim.world.World;
import com.progolf.sim.world.WorldConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * save-persistence / resource-ownership: the filesystem store round-trips a world through disk, detects
 * corruption, isolates saves, and scopes every operation to its owner so users never see each other's saves.
 */
class FilesystemSaveGameStoreTest {

    private static final WorldConfig SMALL = new WorldConfig(40, 6, 3, 20, 4);
    private static final String OWNER = "owner-1";
    private final ObjectMapper mapper = FilesystemSaveGameStore.persistenceMapper();

    @TempDir
    Path dir;
    private FilesystemSaveGameStore store;

    @BeforeEach
    void setUp() {
        store = new FilesystemSaveGameStore(dir.toString());
    }

    private SaveGame gameFrom(World world, long seed, String id) {
        SaveMetadata meta = new SaveMetadata(id, Instant.now(), world.currentSeason(), world.currentWeek(),
                world.playerGolferId().orElse(null));
        return new SaveGame(seed, world.config(), world.snapshot(), meta);
    }

    @Test
    void savingThenLoadingPreservesTheSnapshot() {
        World world = World.create(123L, SMALL);
        world.advanceSeason();
        store.save(OWNER, "s1", gameFrom(world, 123L, "s1"));

        SaveGame loaded = store.load(OWNER, "s1");
        assertThat(loaded.seed()).isEqualTo(123L);
        assertThat(loaded.config()).isEqualTo(SMALL);
        assertThat(loaded.snapshot()).isEqualTo(world.snapshot()); // full structural equality through JSON
    }

    @Test
    void aWorldLoadedFromDiskContinuesIdentically() {
        World original = World.create(0xABCDEL, SMALL);
        original.advanceSeason();
        store.save(OWNER, "s1", gameFrom(original, 0xABCDEL, "s1"));

        SaveGame loaded = store.load(OWNER, "s1");
        World restored = World.restore(loaded.seed(), loaded.config(), loaded.snapshot());
        for (int i = 0; i < 3; i++) {
            original.advanceSeason();
            restored.advanceSeason();
        }
        assertThat(restored.snapshot()).as("save -> load -> advance == advance").isEqualTo(original.snapshot());
    }

    @Test
    void aCorruptPayloadIsRejected() throws IOException {
        store.save(OWNER, "s1", gameFrom(World.create(1L, SMALL), 1L, "s1"));
        Path file = dir.resolve(OWNER).resolve("s1.json");
        SaveEnvelope env = mapper.readValue(Files.readString(file), SaveEnvelope.class);
        // Keep the version valid but corrupt the checksum so the payload no longer matches.
        Files.writeString(file, mapper.writeValueAsString(
                new SaveEnvelope(env.formatVersion(), "deadbeef", env.payload())));
        assertThatThrownBy(() -> store.load(OWNER, "s1")).isInstanceOf(SaveCorruptedException.class);
    }

    @Test
    void anUnsupportedFormatVersionIsRejected() throws IOException {
        store.save(OWNER, "s1", gameFrom(World.create(1L, SMALL), 1L, "s1"));
        Path file = dir.resolve(OWNER).resolve("s1.json");
        SaveEnvelope env = mapper.readValue(Files.readString(file), SaveEnvelope.class);
        Files.writeString(file, mapper.writeValueAsString(
                new SaveEnvelope(FilesystemSaveGameStore.FORMAT_VERSION + 1, env.checksum(), env.payload())));
        assertThatThrownBy(() -> store.load(OWNER, "s1")).isInstanceOf(SaveVersionMismatchException.class);
    }

    @Test
    void loadingAMissingSaveReportsNotFound() {
        assertThatThrownBy(() -> store.load(OWNER, "nope")).isInstanceOf(SaveNotFoundException.class);
    }

    @Test
    void multipleSavesAreIsolatedListedAndDeletable() {
        store.save(OWNER, "alpha", gameFrom(World.create(1L, SMALL), 1L, "alpha"));
        World b = World.create(2L, SMALL);
        b.advanceSeason();
        store.save(OWNER, "beta", gameFrom(b, 2L, "beta"));

        assertThat(store.list(OWNER)).extracting(SaveMetadata::saveId).containsExactlyInAnyOrder("alpha", "beta");
        assertThat(store.load(OWNER, "alpha").seed()).isEqualTo(1L);
        assertThat(store.load(OWNER, "beta").seed()).isEqualTo(2L);

        store.delete(OWNER, "alpha");
        assertThat(store.exists(OWNER, "alpha")).isFalse();
        assertThat(store.list(OWNER)).extracting(SaveMetadata::saveId).containsExactly("beta");
        assertThatThrownBy(() -> store.load(OWNER, "alpha")).isInstanceOf(SaveNotFoundException.class);
    }

    @Test
    void savesAreIsolatedPerOwner() {
        // Two owners save under the SAME id; each keeps their own world and cannot see the other's.
        store.save("alice", "game", gameFrom(World.create(1L, SMALL), 1L, "game"));
        store.save("bob", "game", gameFrom(World.create(2L, SMALL), 2L, "game"));

        assertThat(store.load("alice", "game").seed()).isEqualTo(1L);
        assertThat(store.load("bob", "game").seed()).isEqualTo(2L);
        assertThat(store.list("alice")).extracting(SaveMetadata::saveId).containsExactly("game");

        // Bob deleting his own save leaves Alice's intact.
        store.delete("bob", "game");
        assertThat(store.exists("bob", "game")).isFalse();
        assertThat(store.exists("alice", "game")).isTrue();

        // An owner with nothing saved sees an empty list, not another owner's saves.
        assertThat(store.list("carol")).isEmpty();
    }
}
