package com.progolf.app.persistence;

import java.util.List;

/**
 * The port for durable save storage (spec: save-persistence). The application saves and loads worlds through
 * this seam; the storage medium (filesystem now, a database later) is an adapter behind it. Implementations
 * MUST reject an unsupported format version and a checksum mismatch on load rather than returning a broken
 * world.
 */
public interface SaveGameStore {

    /** Writes {@code game} under {@code saveId}, overwriting any existing save with that id. */
    void save(String saveId, SaveGame game);

    /**
     * Reads the save stored under {@code saveId}.
     *
     * @throws SaveNotFoundException if no save exists for the id
     * @throws SaveVersionMismatchException if the save's format version is unsupported
     * @throws SaveCorruptedException if the save's payload fails its checksum
     */
    SaveGame load(String saveId);

    /** The metadata of every stored save, for listing without loading each world. */
    List<SaveMetadata> list();

    /** True if a save exists under {@code saveId}. */
    boolean exists(String saveId);

    /** Deletes the save under {@code saveId} (a no-op if it does not exist). */
    void delete(String saveId);
}
