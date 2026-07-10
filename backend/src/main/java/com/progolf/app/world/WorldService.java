package com.progolf.app.world;

import com.progolf.sim.world.World;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

/**
 * The single boundary between the application and the simulation engine (spec: world-session, design D2).
 * It creates, holds, advances, and reads {@link WorldSession}s; controllers, and later GraphQL resolvers,
 * persistence, and player actions, all go through here and never touch {@link World} directly. Sessions
 * are held in memory for now — durable saves attach at this seam in the next change.
 */
@Service
public class WorldService {

    private final ConcurrentMap<String, WorldSession> sessions = new ConcurrentHashMap<>();

    /** Creates a new, independent world session from a master seed and returns it. */
    public WorldSession create(long seed) {
        String id = UUID.randomUUID().toString();
        WorldSession session = new WorldSession(id, seed, World.create(seed));
        sessions.put(id, session);
        return session;
    }

    /** The session with the given id, or a 404-mapped exception if unknown. */
    public WorldSession get(String id) {
        return required(id);
    }

    /** Advances a session by one full season. */
    public void advanceSeason(String id) {
        required(id).world().advanceSeason();
    }

    /** Advances a session by one week. */
    public void advanceWeek(String id) {
        required(id).world().advanceWeek();
    }

    /** A read-only status view of a session's current engine state. */
    public WorldStatus status(String id) {
        WorldSession session = required(id);
        World world = session.world();
        return new WorldStatus(session.id(), world.currentSeason(), world.currentWeek(),
                world.activePopulationSize());
    }

    private WorldSession required(String id) {
        WorldSession session = sessions.get(id);
        if (session == null) {
            throw new WorldSessionNotFoundException(id);
        }
        return session;
    }
}
