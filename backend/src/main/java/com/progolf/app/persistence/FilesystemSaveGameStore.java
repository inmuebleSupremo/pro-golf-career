package com.progolf.app.persistence;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;
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

    /** The current on-disk format version. A save recorded with a different version is rejected on load. */
    static final int FORMAT_VERSION = 1;
    private static final String EXTENSION = ".json";

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
        return JsonMapper.builder()
                .findAndAddModules() // jackson-datatype-jsr310 (dates) + jdk8 (Optional)
                .addModule(new SimSnapshotModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();
    }

    @Override
    public void save(String ownerId, String saveId, SaveGame game) {
        try {
            Path ownerDir = ownerDir(ownerId);
            Files.createDirectories(ownerDir);
            String payload = mapper.writeValueAsString(game);
            SaveEnvelope envelope = new SaveEnvelope(FORMAT_VERSION, sha256(payload), payload);
            byte[] bytes = mapper.writeValueAsBytes(envelope);
            Path target = fileFor(ownerId, saveId);
            Path temp = Files.createTempFile(ownerDir, saveId + "-", ".tmp");
            Files.write(temp, bytes);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicUnsupported) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); // fall back if the FS lacks atomic move
            }
        } catch (IOException e) {
            throw new SaveException("Failed to write save: " + saveId, e);
        }
    }

    @Override
    public SaveGame load(String ownerId, String saveId) {
        Path file = fileFor(ownerId, saveId);
        if (!Files.isRegularFile(file)) {
            throw new SaveNotFoundException(saveId);
        }
        SaveEnvelope envelope = readEnvelope(saveId, file);
        if (envelope.formatVersion() != FORMAT_VERSION) {
            throw new SaveVersionMismatchException(saveId, envelope.formatVersion(), FORMAT_VERSION);
        }
        if (!sha256(envelope.payload()).equals(envelope.checksum())) {
            throw new SaveCorruptedException(saveId);
        }
        try {
            return mapper.readValue(envelope.payload(), SaveGame.class);
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
        try {
            Files.deleteIfExists(fileFor(ownerId, saveId));
        } catch (IOException e) {
            throw new SaveException("Failed to delete save: " + saveId, e);
        }
    }

    /** Reads a save's metadata cheaply (parse + pluck) without binding the whole snapshot; skips unreadable files. */
    private java.util.Optional<SaveMetadata> metadataOf(Path file) {
        try {
            SaveEnvelope envelope = mapper.readValue(Files.readString(file, StandardCharsets.UTF_8), SaveEnvelope.class);
            JsonNode metadata = mapper.readTree(envelope.payload()).get("metadata");
            return metadata == null ? java.util.Optional.empty()
                    : java.util.Optional.of(mapper.treeToValue(metadata, SaveMetadata.class));
        } catch (IOException e) {
            return java.util.Optional.empty();
        }
    }

    private SaveEnvelope readEnvelope(String saveId, Path file) {
        try {
            return mapper.readValue(Files.readString(file, StandardCharsets.UTF_8), SaveEnvelope.class);
        } catch (IOException e) {
            throw new SaveCorruptedException(saveId); // the envelope itself is unreadable
        }
    }

    /** Each owner's saves live under their own subdirectory, so listing and loading never cross owners. */
    private Path ownerDir(String ownerId) {
        return directory.resolve(ownerId);
    }

    private Path fileFor(String ownerId, String saveId) {
        return ownerDir(ownerId).resolve(saveId + EXTENSION);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new UncheckedIOException(new IOException("SHA-256 unavailable", e));
        }
    }
}
