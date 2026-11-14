package com.progolf.app.world;

import com.progolf.sim.world.World;
import java.util.Objects;
import java.util.UUID;

/**
 * A running World simulation the application hosts (spec: world-session): its session id, the id of the
 * {@code ownerId} user who created it (spec: resource-ownership), the master seed it was created from, and
 * the live {@link World}, and the save slot it originated from (if any). Sessions are independent and
 * addressable, and reachable only by their owner. Save provenance, including the stable career lineage,
 * belongs to the application session, not the deterministic world snapshot; it is recovered from the selected
 * save slot whenever that save is loaded into a new session.
 */
public record WorldSession(String id, String ownerId, long seed, World world, String originSaveId,
                           String originRevision, String careerLineageId, long autosaveGeneration) {

    public WorldSession(String id, String ownerId, long seed, World world) {
        this(id, ownerId, seed, world, null, null, UUID.randomUUID().toString(), 0);
    }

    public WorldSession(String id, String ownerId, long seed, World world, String originSaveId) {
        this(id, ownerId, seed, world, originSaveId, null, UUID.randomUUID().toString(), 0);
    }

    public WorldSession(String id, String ownerId, long seed, World world, String originSaveId,
                        String originRevision) {
        this(id, ownerId, seed, world, originSaveId, originRevision, UUID.randomUUID().toString(), 0);
    }

    public WorldSession(String id, String ownerId, long seed, World world, String originSaveId,
                        String originRevision, String careerLineageId) {
        this(id, ownerId, seed, world, originSaveId, originRevision, careerLineageId, 0);
    }

    public WorldSession {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(world, "world");
        if (careerLineageId == null || careerLineageId.isBlank()) {
            throw new IllegalArgumentException("Career lineage id is required");
        }
        if (autosaveGeneration < 0) {
            throw new IllegalArgumentException("Autosave generation cannot be negative");
        }
        if (originSaveId != null && originSaveId.isBlank()) {
            throw new IllegalArgumentException("Origin save id must not be blank");
        }
        if (originRevision != null && (originSaveId == null || originRevision.isBlank())) {
            throw new IllegalArgumentException("Origin revision requires a save id");
        }
    }
}
