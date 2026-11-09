package com.progolf.app.persistence;

import java.util.List;

/**
 * The port for durable save storage (spec: save-persistence). The application saves and loads worlds through
 * this seam; the storage medium (filesystem now, a database later) is an adapter behind it. Every operation
 * is scoped to an {@code ownerId} (spec: resource-ownership) — a save belongs to the user who wrote it, and
 * one owner never sees or touches another's saves. Implementations MUST reject an unsupported format version
 * and a checksum mismatch on load rather than returning a broken world.
 */
public interface SaveGameStore {

    /** Writes a save unconditionally and returns the revision of the committed file. */
    String save(String ownerId, String saveId, SaveGame game);

    /**
     * Writes only if the slot still has {@code expectedRevision}; a null expectation means create only when
     * absent. The comparison and replacement must be one serialized write operation across store instances.
     * Returns the revision of the committed file, or throws {@link SaveConflictException} without writing.
     */
    String saveIfUnchanged(String ownerId, String saveId, String expectedRevision, SaveGame game);

    /**
     * Reads the save stored under {@code saveId} for {@code ownerId}.
     *
     * @throws SaveNotFoundException if no such save exists for that owner
     * @throws SaveVersionMismatchException if the save's format version is unsupported
     * @throws SaveCorruptedException if the save's payload fails its checksum
     */
    SaveGame load(String ownerId, String saveId);

    /** Loads the game and revision from the same committed file image. */
    VersionedSave loadVersioned(String ownerId, String saveId);

    /** The metadata of every save owned by {@code ownerId}, for listing without loading each world. */
    List<SaveMetadata> list(String ownerId);

    /** True if a save exists under {@code saveId} for {@code ownerId}. */
    boolean exists(String ownerId, String saveId);

    /** Deletes the save under {@code saveId} for {@code ownerId} (a no-op if it does not exist). */
    void delete(String ownerId, String saveId);
}
