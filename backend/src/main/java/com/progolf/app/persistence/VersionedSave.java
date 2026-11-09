package com.progolf.app.persistence;

/** A saved game and the revision of the exact durable file it was read from. */
public record VersionedSave(SaveGame game, String revision) {
}
