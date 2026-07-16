package com.progolf.app.world;

import com.progolf.app.api.dto.AttributeValueDto;
import com.progolf.app.api.dto.PlayerProfileDto;
import com.progolf.app.api.dto.WorldStatusDto;
import com.progolf.app.persistence.SaveGame;
import com.progolf.app.persistence.SaveGameStore;
import com.progolf.app.persistence.SaveMetadata;
import com.progolf.sim.career.Career;
import com.progolf.sim.career.HallOfFameInduction;
import com.progolf.sim.control.CareerGoal;
import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import com.progolf.sim.economy.FinancialAccount;
import com.progolf.sim.economy.SponsorshipOffer;
import com.progolf.sim.equipment.EquipmentCategory;
import com.progolf.sim.equipment.EquipmentItem;
import com.progolf.sim.media.NewsEvent;
import com.progolf.sim.play.PlayableEvent;
import com.progolf.sim.play.RoundScorecard;
import com.progolf.sim.play.ShotSituation;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Identity;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.shot.ShotDecision;
import com.progolf.sim.shot.ShotOutcome;
import com.progolf.sim.staff.StaffMember;
import com.progolf.sim.staff.StaffRole;
import com.progolf.sim.statistics.SeasonStatistics;
import com.progolf.sim.statistics.StatLine;
import com.progolf.sim.tournament.LeaderboardEntry;
import com.progolf.sim.world.CareerGoalProgress;
import com.progolf.sim.world.PlayerScheduleEntry;
import com.progolf.sim.world.World;
import com.progolf.sim.world.WorldConfig;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

/**
 * The single boundary between the application and the simulation engine (spec: world-session, design D2).
 * It creates, holds, advances, and reads {@link WorldSession}s; GraphQL resolvers, persistence, and player
 * actions all go through here and never touch {@link World} directly.
 *
 * <p>Every operation is scoped to an {@code ownerId} (spec: resource-ownership) — the authenticated user id,
 * passed in as a plain string by the resolvers. A session or save is reachable only by its owner; another
 * user's resource is reported as not-found, never revealing its existence. This class receives the owner as
 * a value and carries no security dependency, keeping the engine seam framework-free and reusable.
 */
@Service
public class WorldService {

    /** The reserved save id refreshed by autosave at each checkpoint (per owner). */
    public static final String AUTOSAVE_ID = "autosave";

    private final ConcurrentMap<String, WorldSession> sessions = new ConcurrentHashMap<>();
    private final SaveGameStore saveStore;

    public WorldService(SaveGameStore saveStore) {
        this.saveStore = saveStore;
    }

    /** Creates a new, independent world session owned by {@code ownerId} from a master seed and returns it. */
    public WorldSession create(String ownerId, long seed) {
        return register(ownerId, seed, World.create(seed));
    }

    /** Creates a new owned session from a seed and an explicit world configuration. */
    public WorldSession create(String ownerId, long seed, WorldConfig config) {
        return register(ownerId, seed, World.create(seed, config));
    }

    private WorldSession register(String ownerId, long seed, World world) {
        String id = UUID.randomUUID().toString();
        WorldSession session = new WorldSession(id, ownerId, seed, world);
        sessions.put(id, session);
        return session;
    }

    // --- Persistence (spec: save-persistence): durable saves attach at this seam, scoped to the owner ---

    /** Saves a session's world to the owner's durable storage under {@code saveId}, overwriting any existing save. */
    public void save(String ownerId, String sessionId, String saveId) {
        WorldSession session = required(ownerId, sessionId);
        World world = session.world();
        SaveMetadata metadata = new SaveMetadata(saveId, Instant.now(), world.currentSeason(), world.currentWeek(),
                world.playerGolferId().orElse(null));
        saveStore.save(ownerId, saveId, new SaveGame(session.seed(), world.config(), world.snapshot(), metadata));
    }

    /** Loads one of the owner's saves into a new session they own and returns it (the world continues identically). */
    public WorldSession load(String ownerId, String saveId) {
        SaveGame game = saveStore.load(ownerId, saveId);
        World world = World.restore(game.seed(), game.config(), game.snapshot());
        String sessionId = UUID.randomUUID().toString();
        WorldSession session = new WorldSession(sessionId, ownerId, game.seed(), world);
        sessions.put(sessionId, session);
        return session;
    }

    /** The metadata of every save owned by {@code ownerId}, newest first. */
    public List<SaveMetadata> listSaves(String ownerId) {
        return saveStore.list(ownerId);
    }

    /** Deletes one of the owner's saves. */
    public void deleteSave(String ownerId, String saveId) {
        saveStore.delete(ownerId, saveId);
    }

    /** Autosaves a session to the owner's reserved slot at a clean boundary (skipped while an event is pending). */
    private void autosave(String ownerId, String sessionId) {
        if (!required(ownerId, sessionId).world().hasPendingPlayerEvent()) {
            save(ownerId, sessionId, AUTOSAVE_ID);
        }
    }

    /** The owner's session with the given id, or a 404-mapped exception if unknown or not theirs. */
    public WorldSession get(String ownerId, String id) {
        return required(ownerId, id);
    }

    /** Advances one of the owner's sessions by one full season, then autosaves the checkpoint. */
    public void advanceSeason(String ownerId, String id) {
        required(ownerId, id).world().advanceSeason();
        autosave(ownerId, id);
    }

    /** Advances one of the owner's sessions by one week. */
    public void advanceWeek(String ownerId, String id) {
        required(ownerId, id).world().advanceWeek();
    }

    /** A read-only status view of one of the owner's sessions. */
    public WorldStatusDto status(String ownerId, String id) {
        WorldSession session = required(ownerId, id);
        World world = session.world();
        return new WorldStatusDto(session.id(), world.currentSeason(), world.currentWeek(),
                world.activePopulationSize(), world.hasPendingPlayerEvent());
    }

    // --- Player control (spec: player-control): the human guides one designated golfer ---

    /** Whether a session has a designated (human-controlled) player golfer. */
    public boolean hasPlayer(String ownerId, String sessionId) {
        return required(ownerId, sessionId).world().playerGolferId().isPresent();
    }

    /** Designates a golfer in a session as human-controlled. */
    public void assignPlayer(String ownerId, String sessionId, String golferId) {
        required(ownerId, sessionId).world().assignPlayer(golferId);
    }

    /** Creates a custom golfer (identity + archetype build) as the session's player; returns its id. */
    public String createPlayer(String ownerId, String sessionId, String firstName, String lastName,
                               Nationality nationality, int startAge, Archetype archetype) {
        return required(ownerId, sessionId).world().createPlayer(firstName, lastName, nationality, startAge, archetype);
    }

    /** Sets the player's development focus (attribute priority) in a session. */
    public void setDevelopmentFocus(String ownerId, String sessionId, List<Attribute> focus) {
        required(ownerId, sessionId).world().setDevelopmentFocus(focus);
    }

    /** Sets whether the player's golfer is resting (a blanket sit-out) in a session. */
    public void setResting(String ownerId, String sessionId, boolean resting) {
        required(ownerId, sessionId).world().setResting(resting);
    }

    /** The player's reviewable eligible schedule (each event's prestige and entry status). */
    public List<PlayerScheduleEntry> playerSchedule(String ownerId, String sessionId) {
        return required(ownerId, sessionId).world().playerSchedule();
    }

    /** Skips a specific upcoming event by tournament id for the player. */
    public void skipEvent(String ownerId, String sessionId, long tournamentId) {
        required(ownerId, sessionId).world().skipEvent(tournamentId);
    }

    /** Re-enters a previously skipped event for the player. */
    public void enterEvent(String ownerId, String sessionId, long tournamentId) {
        required(ownerId, sessionId).world().enterEvent(tournamentId);
    }

    /** Sets the player's self-chosen career goals. */
    public void setCareerGoals(String ownerId, String sessionId, List<CareerGoal> goals) {
        required(ownerId, sessionId).world().setCareerGoals(goals);
    }

    /** The player's career goals with live progress toward each. */
    public List<CareerGoalProgress> careerGoals(String ownerId, String sessionId) {
        return required(ownerId, sessionId).world().careerGoals();
    }

    /**
     * The player's golfer profile (spec: player-profile-api): identity, attributes, world ranking, earnings,
     * tour, and career stats — aggregated read-only over existing engine reads. Assumes a player is assigned
     * (callers guard with {@link #hasPlayer}). Attributes are returned in {@link Attribute} enum order.
     */
    public PlayerProfileDto playerProfile(String ownerId, String sessionId) {
        World world = required(ownerId, sessionId).world();
        String id = requirePlayerId(world);
        Career career = world.careerOf(id);
        Identity identity = career.player().identity();
        Attributes attrs = career.player().attributes();
        StatLine stats = world.careerStatisticsOf(id);
        FinancialAccount.Snapshot finances = world.financialAccountOf(id).snapshot();
        Integer worldRanking = world.currentRanking().positionOf(id).orElse(null);
        String tour = world.tourOf(id).map(Enum::name).orElse(null);

        List<AttributeValueDto> attributes = new java.util.ArrayList<>();
        for (Attribute a : Attribute.values()) {
            attributes.add(new AttributeValueDto(a.name(), attrs.get(a)));
        }

        return new PlayerProfileDto(id, identity.firstName(), identity.lastName(),
                identity.nationality().name(), career.age(), identity.archetype().name(),
                worldRanking, finances.tournamentEarnings(), finances.availableFunds(), tour,
                stats.events(), stats.wins(), stats.topTens(), attributes);
    }

    /** The Hall-of-Fame inductions so far (spec: career-legacy). */
    public List<HallOfFameInduction> hallOfFame(String ownerId, String sessionId) {
        return required(ownerId, sessionId).world().hallOfFameInductions();
    }

    /** The most recent {@code limit} world news items, most recent first (the between-events feedback feed). */
    public List<NewsEvent> recentNews(String ownerId, String sessionId, int limit) {
        List<NewsEvent> feed = required(ownerId, sessionId).world().newsFeed();
        int from = Math.max(0, feed.size() - Math.max(0, limit));
        List<NewsEvent> recent = new ArrayList<>(feed.subList(from, feed.size()));
        Collections.reverse(recent);
        return recent;
    }

    /**
     * The player's per-season statistics, one entry per season they actually competed in (season 1 through
     * the current season, skipping any with no counted events). Requires a player (callers guard with
     * {@link #hasPlayer}); the current, in-progress season is included as it accumulates.
     */
    public List<SeasonStatistics> playerSeasonStats(String ownerId, String sessionId) {
        World world = required(ownerId, sessionId).world();
        String id = requirePlayerId(world);
        List<SeasonStatistics> stats = new ArrayList<>();
        for (int season = 1; season <= world.currentSeason(); season++) {
            StatLine line = world.seasonStatisticsOf(id, season);
            if (line.events() > 0) {
                stats.add(new SeasonStatistics(id, season, line));
            }
        }
        return stats;
    }

    /** The player's pending sponsorship offers awaiting a decision. */
    public List<SponsorshipOffer> pendingSponsorships(String ownerId, String sessionId) {
        return required(ownerId, sessionId).world().pendingSponsorships();
    }

    /** Accepts a pending sponsorship offer by index. */
    public void acceptSponsorship(String ownerId, String sessionId, int index) {
        required(ownerId, sessionId).world().acceptSponsorship(index);
    }

    // --- Player staff & equipment (spec: player-control) ---

    /** The player's staff candidates awaiting a hire decision. */
    public List<StaffMember> pendingStaffOffers(String ownerId, String sessionId) {
        return required(ownerId, sessionId).world().pendingStaffOffers();
    }

    /** Hires a pending staff candidate by index (if affordable). */
    public void hireStaff(String ownerId, String sessionId, int index) {
        required(ownerId, sessionId).world().hireStaff(index);
    }

    /** Releases a current staff member of the player's team by role. */
    public void releaseStaff(String ownerId, String sessionId, StaffRole role) {
        required(ownerId, sessionId).world().releaseStaff(role);
    }

    /** The player's equipment upgrade offers awaiting a purchase decision. */
    public List<EquipmentItem> pendingEquipmentOffers(String ownerId, String sessionId) {
        return required(ownerId, sessionId).world().pendingEquipmentOffers();
    }

    /** Buys a pending equipment upgrade by index (if affordable). */
    public void buyEquipment(String ownerId, String sessionId, int index) {
        required(ownerId, sessionId).world().buyEquipment(index);
    }

    /** Sets the player's loadout for a category to one of their owned items. */
    public void selectLoadoutItem(String ownerId, String sessionId, EquipmentItem item) {
        required(ownerId, sessionId).world().selectLoadoutItem(item);
    }

    /** Every item the player currently owns, across all equipment categories. */
    public List<EquipmentItem> playerEquipment(String ownerId, String sessionId) {
        World world = required(ownerId, sessionId).world();
        return world.equipmentInventoryOf(requirePlayerId(world)).all();
    }

    /** The item the player currently has selected in each equipment category (the tournament loadout). */
    public List<EquipmentItem> playerLoadout(String ownerId, String sessionId) {
        World world = required(ownerId, sessionId).world();
        return List.copyOf(world.tournamentLoadoutOf(requirePlayerId(world)).selection().values());
    }

    /**
     * Sets the player's loadout for a category to one of their owned items, addressed by category and name
     * (the handle the read model exposes). Throws if the player owns no such item in that category.
     */
    public void selectLoadoutItem(String ownerId, String sessionId, EquipmentCategory category, String name) {
        World world = required(ownerId, sessionId).world();
        EquipmentItem item = world.equipmentInventoryOf(requirePlayerId(world)).itemsIn(category).stream()
                .filter(i -> i.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "The player owns no " + category + " item named '" + name + "'"));
        world.selectLoadoutItem(item);
    }

    private String requirePlayerId(World world) {
        return world.playerGolferId()
                .orElseThrow(() -> new IllegalStateException("No player has been assigned to this world"));
    }

    // --- Playable event (spec: playable-event): the player plays their own tournament ---

    /** Whether the session is paused awaiting the player to play (or sim) their scheduled event. */
    public boolean hasPendingEvent(String ownerId, String sessionId) {
        return required(ownerId, sessionId).world().hasPendingPlayerEvent();
    }

    /**
     * The current shot situation in the player's event (the round or playoff hole they are playing), or
     * {@code null} when the event has been played to the end but not yet completed. A pending event can be
     * finished-but-awaiting-completion (all rounds/playoff played, {@link #completeEvent} not yet called);
     * the play surface reads this to decide whether to show the "finish event" step, so report no situation
     * rather than throwing "the event is complete".
     */
    public ShotSituation currentSituation(String ownerId, String sessionId) {
        PlayableEvent event = playerEvent(ownerId, sessionId);
        return event.isComplete() ? null : event.situation();
    }

    /** The live field leaderboard for the player's event. */
    public List<LeaderboardEntry> eventLeaderboard(String ownerId, String sessionId) {
        return playerEvent(ownerId, sessionId).leaderboard();
    }

    /** The player's current-round scorecard, or null when no round is in progress (playoff or event done). */
    public RoundScorecard currentScorecard(String ownerId, String sessionId) {
        return playerEvent(ownerId, sessionId).currentScorecard();
    }

    /** Plays the current shot in the player's event with the human's decision (club / target / risk). */
    public ShotOutcome playShot(String ownerId, String sessionId, ShotDecision decision) {
        return playerEvent(ownerId, sessionId).playShot(decision);
    }

    /** Sims the current shot in the player's event. */
    public ShotOutcome simShot(String ownerId, String sessionId) {
        return playerEvent(ownerId, sessionId).simShot();
    }

    /** Sims the rest of the current hole in the player's event. */
    public void simHole(String ownerId, String sessionId) {
        playerEvent(ownerId, sessionId).simHole();
    }

    /** Sims the rest of the current round in the player's event. */
    public void simRound(String ownerId, String sessionId) {
        playerEvent(ownerId, sessionId).simRound();
    }

    /** Sims the remainder of the player's event (all remaining rounds and any playoff). */
    public void simEvent(String ownerId, String sessionId) {
        playerEvent(ownerId, sessionId).simEvent();
    }

    /** Whether the player made the cut in their event (valid once the second round and cut are played). */
    public boolean playerMadeCut(String ownerId, String sessionId) {
        return playerEvent(ownerId, sessionId).playerMadeCut();
    }

    /** Completes the player's finished event so its result counts and the paused week resumes, then autosaves. */
    public void completeEvent(String ownerId, String sessionId) {
        required(ownerId, sessionId).world().completePlayerEvent();
        autosave(ownerId, sessionId);
    }

    private PlayableEvent playerEvent(String ownerId, String sessionId) {
        return required(ownerId, sessionId).world().playerEvent();
    }

    /**
     * The session with the given id, but only if it belongs to {@code ownerId}. An unknown session and one
     * owned by a different user are indistinguishable — both are not-found — so a session's existence is
     * never revealed to a non-owner (spec: resource-ownership).
     */
    private WorldSession required(String ownerId, String id) {
        WorldSession session = sessions.get(id);
        if (session == null || !session.ownerId().equals(ownerId)) {
            throw new WorldSessionNotFoundException(id);
        }
        return session;
    }
}
