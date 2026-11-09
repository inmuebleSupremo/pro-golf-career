package com.progolf.app.world;

import com.progolf.sim.world.World;
import java.util.Objects;

/**
 * A running World simulation the application hosts (spec: world-session): its session id, the id of the
 * {@code ownerId} user who created it (spec: resource-ownership), the master seed it was created from, and
 * the live {@link World}, and the save slot it originated from (if any). Sessions are independent and
 * addressable, and reachable only by their owner. Save provenance belongs to the live session, not the world
 * snapshot; it is recovered from the selected save slot whenever that save is loaded into a new session.
 */
public record WorldSession(String id, String ownerId, long seed, World world, String originSaveId) {

    public WorldSession(String id, String ownerId, long seed, World world) {
        this(id, ownerId, seed, world, null);
    }

    public WorldSession {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(world, "world");
        if (originSaveId != null && originSaveId.isBlank()) {
            throw new IllegalArgumentException("Origin save id must not be blank");
        }
    }
}
