package com.progolf.app.persistence;

/**
 * The on-disk wrapper around a save (spec: save-persistence): a {@code formatVersion} for compatibility and
 * a {@code checksum} (SHA-256 hex of {@code payload}) for corruption detection. {@code payload} is the JSON
 * of the {@link SaveGame}; hashing the exact stored string means any byte-level corruption is caught on load.
 */
public record SaveEnvelope(int formatVersion, String checksum, String payload) {
}
