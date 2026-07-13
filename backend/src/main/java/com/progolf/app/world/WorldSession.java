package com.progolf.app.world;

import com.progolf.sim.world.World;
import java.util.Objects;

/**
 * A running World simulation the application hosts (spec: world-session): its session id, the id of the
 * {@code ownerId} user who created it (spec: resource-ownership), the master seed it was created from, and
 * the live {@link World}. Sessions are independent and addressable, and reachable only by their owner.
 */
public record WorldSession(String id, String ownerId, long seed, World world) {

    public WorldSession {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(world, "world");
    }
}
