package com.progolf.app.world;

import com.progolf.sim.world.World;
import java.util.Objects;

/**
 * A running World simulation the application hosts (spec: world-session): its session id, the master seed
 * it was created from, and the live {@link World}. Sessions are independent and addressable — the unit
 * that persistence (a save is a session) and, later, a user's owned worlds build on.
 */
public record WorldSession(String id, long seed, World world) {

    public WorldSession {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(world, "world");
    }
}
