package com.progolf.app.persistence;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.progolf.sim.core.SeasonProfile;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A {@link SaveGameStore} backed by the local filesystem (spec: save-persistence): one {@code <id>.json} per
 * save under a configurable directory. Each file is a {@link SaveEnvelope} — a format version and a SHA-256
 * of the payload wrapping the {@link SaveGame} JSON — so an unsupported version or a corrupt/tampered payload
 * is rejected on load. Writes are atomic (temp file + move) so a crash mid-write never leaves a half file.
 * All Jackson/filesystem code lives here; the engine snapshot is a plain record the mapper reflects over.
 */
@Component
public class FilesystemSaveGameStore implements SaveGameStore {

    private static final Logger LOG = LoggerFactory.getLogger(FilesystemSaveGameStore.class);

    /**
     * The current on-disk format version. V4 adds durable career lineage; V2 and V3 remain readable.
     *
     * <p>v2: every golfer carries a {@code potential} (spec: player-development). A v1 save has no ceiling
     * recorded for anyone, and one cannot be inferred after the fact — a golfer's talent is drawn at
     * generation, and their attributes today say nothing about how much headroom they were born with. Such a
     * save is rejected rather than loaded against a guessed ceiling.
     */
    static final int FORMAT_VERSION = 4;
    /** The envelope stores the full snapshot as one string; 256 MiB allows roughly 11x the Season 5 payload. */
    static final int MAX_SAVE_STRING_LENGTH = 256 * 1024 * 1024;
    private static final int MIN_READABLE_FORMAT_VERSION = 2;
    private static final String EXTENSION = ".json";
    private static final ConcurrentMap<Path, ReentrantLock> LOCAL_SLOT_LOCKS = new ConcurrentHashMap<>();

    private final Path directory;
    private final ObjectMapper mapper;

    public FilesystemSaveGameStore(@Value("${progolf.saves.dir:./saves}") String directory) {
        this.directory = Path.of(directory);
        this.mapper = persistenceMapper();
    }

    /**
     * A dedicated ObjectMapper for saves — isolated from the app's HTTP mapper. It has the date/Optional
     * modules plus {@link SimSnapshotModule}, and ignores unknown properties on read so the snapshot records'
     * derived getters (e.g. {@code Injury.isHealed()}) do not break reconstruction via the canonical
     * constructor. Package-private so tests share the exact configuration.
     */
    static ObjectMapper persistenceMapper() {
        JsonFactory factory = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder()
                        .maxStringLength(MAX_SAVE_STRING_LENGTH)
                        .build())
                .build();
        return JsonMapper.builder(factory)
                .findAndAddModules() // jackson-datatype-jsr310 (dates) + jdk8 (Optional)
                .addModule(new SimSnapshotModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();
    }

    @Override
    public String save(String ownerId, String saveId, SaveGame game) {
        return write(ownerId, saveId, null, false, game);
    }

    @Override
    public String saveIfUnchanged(String ownerId, String saveId, String expectedRevision, SaveGame game) {
        return write(ownerId, saveId, expectedRevision, true, game);
    }

    private String write(String ownerId, String saveId, String expectedRevision, boolean conditional, SaveGame game) {
        return withSlotLock(ownerId, saveId, target -> {
            if (conditional) {
                boolean present = Files.isRegularFile(target);
                if (expectedRevision == null ? present
                        : !present || !expectedRevision.equals(sha256(Files.readAllBytes(target)))) {
                    throw new SaveConflictException(saveId);
                }
            }
            SeasonProfile profile = SeasonProfile.current();
            long started = profile == null ? 0 : System.nanoTime();
            String payload;
            try {
                payload = mapper.writeValueAsString(game);
            } finally {
                if (profile != null) profile.record(SeasonProfile.Metric.SERIALIZATION, System.nanoTime() - started);
            }
            started = profile == null ? 0 : System.nanoTime();
            String checksum;
            try {
                checksum = sha256(payload);
            } finally {
                if (profile != null) profile.record(SeasonProfile.Metric.CHECKSUM, System.nanoTime() - started);
            }
            SaveEnvelope envelope = new SaveEnvelope(FORMAT_VERSION, checksum, payload);
            started = profile == null ? 0 : System.nanoTime();
            byte[] bytes;
            try {
                bytes = mapper.writeValueAsBytes(envelope);
            } finally {
                if (profile != null) profile.record(SeasonProfile.Metric.SERIALIZATION, System.nanoTime() - started);
            }
            started = profile == null ? 0 : System.nanoTime();
            try {
                Path temp = Files.createTempFile(ownerDir(ownerId), saveId + "-", ".tmp");
                try {
                    Files.write(temp, bytes);
                    try {
                        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    } catch (AtomicMoveNotSupportedException atomicUnsupported) {
                        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                } finally {
                    Files.deleteIfExists(temp);
                }
            } finally {
                if (profile != null) profile.record(SeasonProfile.Metric.FILE_WRITE, System.nanoTime() - started);
            }
            started = profile == null ? 0 : System.nanoTime();
            try {
                return sha256(bytes);
            } finally {
                if (profile != null) profile.record(SeasonProfile.Metric.CHECKSUM, System.nanoTime() - started);
            }
        });
    }

    @Override
    public SaveGame load(String ownerId, String saveId) {
        return loadVersioned(ownerId, saveId).game();
    }

    @Override
    public VersionedSave loadVersioned(String ownerId, String saveId) {
        Path file = fileFor(ownerId, saveId);
        if (!Files.isRegularFile(file)) {
            throw new SaveNotFoundException(saveId);
        }
        byte[] bytes;
        SaveEnvelope envelope;
        try {
            bytes = Files.readAllBytes(file);
            envelope = mapper.readValue(bytes, SaveEnvelope.class);
        } catch (IOException e) {
            throw new SaveCorruptedException(saveId);
        }
        if (envelope.formatVersion() < MIN_READABLE_FORMAT_VERSION || envelope.formatVersion() > FORMAT_VERSION) {
            throw new SaveVersionMismatchException(saveId, envelope.formatVersion(), FORMAT_VERSION);
        }
        if (!sha256(envelope.payload()).equals(envelope.checksum())) {
            throw new SaveCorruptedException(saveId);
        }
        try {
            return new VersionedSave(mapper.readValue(envelope.payload(), SaveGame.class), sha256(bytes));
        } catch (IOException e) {
            throw new SaveCorruptedException(saveId); // payload no longer deserializes to a SaveGame
        }
    }

    @Override
    public List<SaveMetadata> list(String ownerId) {
        Path ownerDir = ownerDir(ownerId);
        if (!Files.isDirectory(ownerDir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(ownerDir)) {
            List<SaveMetadata> summaries = new ArrayList<>();
            files.filter(p -> p.getFileName().toString().endsWith(EXTENSION)).forEach(p -> {
                metadataOf(p).ifPresent(summaries::add);
            });
            summaries.sort(Comparator.comparing(SaveMetadata::savedAt).reversed());
            return summaries;
        } catch (IOException e) {
            throw new SaveException("Failed to list saves", e);
        }
    }

    @Override
    public boolean exists(String ownerId, String saveId) {
        return Files.isRegularFile(fileFor(ownerId, saveId));
    }

    @Override
    public void delete(String ownerId, String saveId) {
        withSlotLock(ownerId, saveId, target -> { Files.deleteIfExists(target); return null; });
    }

    /** Reads a save's metadata cheaply (parse + pluck) without binding the whole snapshot; skips unreadable files. */
    private java.util.Optional<SaveMetadata> metadataOf(Path file) {
        try {
            SaveEnvelope envelope = mapper.readValue(Files.readString(file, StandardCharsets.UTF_8), SaveEnvelope.class);
            JsonNode metadata = mapper.readTree(envelope.payload()).get("metadata");
            if (metadata == null) {
                LOG.warn("Skipping save {}: missing metadata", file.getFileName());
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(mapper.treeToValue(metadata, SaveMetadata.class));
        } catch (IOException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            LOG.warn("Skipping unreadable save {}: {}", file.getFileName(), cause.getClass().getSimpleName());
            return java.util.Optional.empty();
        }
    }

    @FunctionalInterface
    private interface SlotOperation<T> {
        T run(Path target) throws IOException;
    }

    private <T> T withSlotLock(String ownerId, String saveId, SlotOperation<T> operation) {
        Path target = fileFor(ownerId, saveId);
        Path lockPath = ownerDir(ownerId).resolve(saveId + ".lock").toAbsolutePath().normalize();
        ReentrantLock local = LOCAL_SLOT_LOCKS.computeIfAbsent(lockPath, ignored -> new ReentrantLock());
        local.lock();
        try {
            Files.createDirectories(ownerDir(ownerId));
            try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                    FileLock ignored = channel.lock()) {
                return operation.run(target);
            }
        } catch (IOException e) {
            throw new SaveException("Failed to access save: " + saveId, e);
        } finally {
            local.unlock();
        }
    }

    /** Each owner's saves live under their own subdirectory, so listing and loading never cross owners. */
    private Path ownerDir(String ownerId) {
        return directory.resolve(ownerId);
    }

    private Path fileFor(String ownerId, String saveId) {
        // A save id is one filename within the authenticated owner's directory, never a path supplied by a client.
        if (saveId == null || saveId.isBlank() || saveId.indexOf('/') >= 0 || saveId.indexOf('\\') >= 0
                || saveId.indexOf(':') >= 0) {
            throw new IllegalArgumentException("Invalid save id");
        }
        return ownerDir(ownerId).resolve(saveId + EXTENSION);
    }

    private static String sha256(String value) {
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(byte[] value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value));
        } catch (NoSuchAlgorithmException e) {
            throw new UncheckedIOException(new IOException("SHA-256 unavailable", e));
        }
    }
}
