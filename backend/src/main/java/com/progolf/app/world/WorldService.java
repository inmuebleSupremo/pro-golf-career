package com.progolf.app.world;

import com.progolf.sim.core.Attribute;
import com.progolf.sim.economy.SponsorshipOffer;
import com.progolf.sim.play.PlayableEvent;
import com.progolf.sim.play.ShotSituation;
import com.progolf.sim.shot.ShotDecision;
import com.progolf.sim.shot.ShotOutcome;
import com.progolf.sim.tournament.LeaderboardEntry;
import com.progolf.sim.world.World;
import java.util.List;
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

    // --- Player control (spec: player-control): the human guides one designated golfer ---

    /** Designates a golfer in a session as human-controlled. */
    public void assignPlayer(String sessionId, String golferId) {
        required(sessionId).world().assignPlayer(golferId);
    }

    /** Sets the player's development focus (attribute priority) in a session. */
    public void setDevelopmentFocus(String sessionId, List<Attribute> focus) {
        required(sessionId).world().setDevelopmentFocus(focus);
    }

    /** Sets whether the player's golfer is resting in a session. */
    public void setResting(String sessionId, boolean resting) {
        required(sessionId).world().setResting(resting);
    }

    /** The player's pending sponsorship offers awaiting a decision. */
    public List<SponsorshipOffer> pendingSponsorships(String sessionId) {
        return required(sessionId).world().pendingSponsorships();
    }

    /** Accepts a pending sponsorship offer by index. */
    public void acceptSponsorship(String sessionId, int index) {
        required(sessionId).world().acceptSponsorship(index);
    }

    // --- Playable event (spec: playable-event): the player plays their own tournament ---

    /** Whether the session is paused awaiting the player to play (or sim) their scheduled event. */
    public boolean hasPendingEvent(String sessionId) {
        return required(sessionId).world().hasPendingPlayerEvent();
    }

    /** The current shot situation in the player's event (the round or playoff hole they are playing). */
    public ShotSituation currentSituation(String sessionId) {
        return playerEvent(sessionId).situation();
    }

    /** The live field leaderboard for the player's event. */
    public List<LeaderboardEntry> eventLeaderboard(String sessionId) {
        return playerEvent(sessionId).leaderboard();
    }

    /** Plays the current shot in the player's event with the human's decision (club / target / risk). */
    public ShotOutcome playShot(String sessionId, ShotDecision decision) {
        return playerEvent(sessionId).playShot(decision);
    }

    /** Sims the current shot in the player's event. */
    public ShotOutcome simShot(String sessionId) {
        return playerEvent(sessionId).simShot();
    }

    /** Sims the rest of the current hole in the player's event. */
    public void simHole(String sessionId) {
        playerEvent(sessionId).simHole();
    }

    /** Sims the rest of the current round in the player's event. */
    public void simRound(String sessionId) {
        playerEvent(sessionId).simRound();
    }

    /** Sims the remainder of the player's event (all remaining rounds and any playoff). */
    public void simEvent(String sessionId) {
        playerEvent(sessionId).simEvent();
    }

    /** Whether the player made the cut in their event (valid once the second round and cut are played). */
    public boolean playerMadeCut(String sessionId) {
        return playerEvent(sessionId).playerMadeCut();
    }

    /** Completes the player's finished event so its result counts and the paused week resumes. */
    public void completeEvent(String sessionId) {
        required(sessionId).world().completePlayerEvent();
    }

    private PlayableEvent playerEvent(String sessionId) {
        return required(sessionId).world().playerEvent();
    }

    private WorldSession required(String id) {
        WorldSession session = sessions.get(id);
        if (session == null) {
            throw new WorldSessionNotFoundException(id);
        }
        return session;
    }
}
