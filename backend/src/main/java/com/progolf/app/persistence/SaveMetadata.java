package com.progolf.app.persistence;

import java.time.Instant;

/**
 * Display metadata for a save (spec: save-persistence): enough to list saves without inspecting the whole
 * world. {@code playerGolferId} is null for an autonomous world.
 */
public record SaveMetadata(String saveId, Instant savedAt, int season, int week, String playerGolferId) {
}
