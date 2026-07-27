package com.progolf.app.world;

import com.progolf.app.api.dto.AttributeValueDto;
import com.progolf.app.api.dto.CalendarEntryDto;
import com.progolf.app.api.dto.DevelopmentDeltaDto;
import com.progolf.app.api.dto.PlayerDevelopmentDto;
import com.progolf.app.api.dto.EventResultDto;
import com.progolf.app.api.dto.FinisherDto;
import com.progolf.app.api.dto.HallOfFameDto;
import com.progolf.app.api.dto.InjuryDto;
import com.progolf.app.api.dto.PlayerFitnessDto;
import com.progolf.app.api.dto.PlayerProfileDto;
import com.progolf.app.api.dto.EquipmentDealDto;
import com.progolf.app.api.dto.EquipmentItemDto;
import com.progolf.app.api.dto.NewsItemDto;
import com.progolf.app.api.dto.RankingRowDto;
import com.progolf.app.api.dto.RecordDto;
import com.progolf.app.api.dto.SeasonReviewDto;
import com.progolf.app.api.dto.SeasonStatDto;
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
import com.progolf.sim.equipment.EquipmentCharacteristics;
import com.progolf.sim.equipment.EquipmentDeal;
import com.progolf.sim.equipment.EquipmentFit;
import com.progolf.sim.equipment.EquipmentItem;
import com.progolf.sim.media.NewsEvent;
import com.progolf.sim.play.PlayableEvent;
import com.progolf.sim.play.RoundScorecard;
import com.progolf.sim.play.ShotSituation;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Identity;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.progression.ProgressionConstants;
import com.progolf.sim.ranking.RankingSnapshot;
import com.progolf.sim.ranking.RankingStanding;
import com.progolf.sim.shot.ShotDecision;
import com.progolf.sim.shot.ShotOutcome;
import com.progolf.sim.staff.StaffMember;
import com.progolf.sim.staff.StaffRole;
import com.progolf.sim.statistics.SeasonStatistics;
import com.progolf.sim.statistics.StatLine;
import com.progolf.sim.tournament.LeaderboardEntry;
import com.progolf.sim.tournament.TournamentResult;
import com.progolf.sim.world.CareerGoalProgress;
import com.progolf.sim.world.PlayerScheduleEntry;
import com.progolf.sim.world.World;
import com.progolf.sim.world.WorldConfig;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    /**
     * The player's season calendar: every eligible event with its name and entry status, marked played once
     * resolved, and — for played events — the result (winner, top finishers, and the player's own finish).
     * Requires a player (callers guard with {@link #hasPlayer}).
     */
    public List<CalendarEntryDto> playerCalendar(String ownerId, String sessionId) {
        World world = required(ownerId, sessionId).world();
        String playerId = requirePlayerId(world);
        List<CalendarEntryDto> calendar = new ArrayList<>();
        for (PlayerScheduleEntry entry : world.playerSchedule()) {
            Optional<TournamentResult> result = world.resultOf(entry.tournamentId());
            EventResultDto resultDto = result.map(r -> eventResult(r, playerId)).orElse(null);
            calendar.add(new CalendarEntryDto(entry.tournamentId(), entry.week(), entry.tier().name(),
                    entry.prestige().name(), entry.entered(), entry.name(), entry.location(),
                    result.isPresent(), resultDto));
        }
        return calendar;
    }

    private static EventResultDto eventResult(TournamentResult result, String playerId) {
        List<TournamentResult.Finish> byPosition = result.finishingOrder().stream()
                .sorted(Comparator.comparingInt(TournamentResult.Finish::position))
                .toList();
        List<FinisherDto> topThree = byPosition.stream().limit(3).map(WorldService::finisher).toList();
        FinisherDto winner = topThree.isEmpty() ? null : topThree.get(0);
        FinisherDto playerFinish = byPosition.stream()
                .filter(f -> f.golfer().player().id().equals(playerId))
                .findFirst().map(WorldService::finisher).orElse(null);
        return new EventResultDto(winner, topThree, playerFinish);
    }

    private static FinisherDto finisher(TournamentResult.Finish f) {
        return new FinisherDto(f.position(), f.golfer().player().identity().fullName(), f.score(),
                f.madeCut(), f.prize());
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
        Attributes potential = career.player().potential();
        StatLine stats = world.careerStatisticsOf(id);
        FinancialAccount.Snapshot finances = world.financialAccountOf(id).snapshot();
        Integer worldRanking = world.currentRanking().positionOf(id).orElse(null);
        String tour = world.tourOf(id).map(Enum::name).orElse(null);

        List<AttributeValueDto> attributes = new java.util.ArrayList<>();
        for (Attribute a : Attribute.values()) {
            attributes.add(new AttributeValueDto(a.name(), attrs.get(a), potential.get(a)));
        }

        return new PlayerProfileDto(id, identity.firstName(), identity.lastName(),
                identity.nationality().name(), career.age(), identity.archetype().name(),
                worldRanking, finances.tournamentEarnings(), finances.availableFunds(), tour,
                stats.events(), stats.wins(), stats.topTens(), career.isRetired(), attributes);
    }

    /** The Hall-of-Fame inductions so far (spec: career-legacy). */
    public List<HallOfFameDto> hallOfFame(String ownerId, String sessionId) {
        World world = required(ownerId, sessionId).world();
        List<HallOfFameDto> inductions = new ArrayList<>();
        for (HallOfFameInduction i : world.hallOfFameInductions()) {
            // Inductees keep their career permanently; fall back to the id if one is somehow absent.
            Career career = world.careerOf(i.golferId());
            String name = career != null ? career.player().identity().fullName() : i.golferId();
            int careerWins = world.careerStatisticsOf(i.golferId()).wins();
            inductions.add(new HallOfFameDto(i.golferId(), name, i.season(), i.score(), careerWins));
        }
        return inductions;
    }

    /**
     * The current World Ranking, top {@code limit} golfers by ranking value, name-enriched. Not player-scoped
     * — the ranking exists regardless of whether a player is assigned. Rows are already ordered by position.
     */
    public List<RankingRowDto> worldRankings(String ownerId, String sessionId, int limit) {
        World world = required(ownerId, sessionId).world();
        int cap = Math.max(0, limit);
        List<RankingRowDto> rows = new ArrayList<>();
        for (RankingStanding s : world.currentRanking().standings()) {
            if (rows.size() >= cap) {
                break;
            }
            // A ranked golfer keeps their career; fall back to the id if one is somehow absent.
            Career career = world.careerOf(s.golferId());
            String name = career != null ? career.player().identity().fullName() : s.golferId();
            rows.add(new RankingRowDto(s.position(), s.golferId(), name, s.rankingValue()));
        }
        return rows;
    }

    /**
     * The player's fitness (spec: physical-state): condition, fatigue, derived availability, and any active
     * injury. Requires a player (callers guard with {@link #hasPlayer}); returns null if the golfer has no
     * tracked physical state (e.g. once retired and removed from the active pool).
     */
    public PlayerFitnessDto playerFitness(String ownerId, String sessionId) {
        World world = required(ownerId, sessionId).world();
        String id = requirePlayerId(world);
        var state = world.physicalStateOf(id);
        if (state == null) {
            return null;
        }
        InjuryDto injury = state.injury()
                .map(i -> new InjuryDto(i.type().name(), i.severity().name(), i.rehabWeeksRemaining()))
                .orElse(null);
        return new PlayerFitnessDto(state.availability().name(), state.fitness(), state.fatigue(),
                state.canCompete(), state.canPlayThroughInjury(), injury);
    }

    /**
     * The world Record Book (spec: records-archive): the current holder of each record, name-enriched. Not
     * player-scoped — records exist regardless of whether a player is assigned. Iterated in RecordType enum
     * order (the underlying map is an EnumMap); only records that have been set are included.
     */
    public List<RecordDto> records(String ownerId, String sessionId) {
        World world = required(ownerId, sessionId).world();
        List<RecordDto> rows = new ArrayList<>();
        for (var entry : world.records().entrySet()) {
            var holder = entry.getValue();
            // A record holder keeps their career; fall back to the id if one is somehow absent.
            Career career = world.careerOf(holder.golferId());
            String name = career != null ? career.player().identity().fullName() : holder.golferId();
            rows.add(new RecordDto(entry.getKey().name(), holder.golferId(), name,
                    holder.value(), holder.season()));
        }
        return rows;
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

    /**
     * The off-season review of a completed season (spec: player-experience — the end-of-season moment): the
     * player's season stat line, their world-ranking movement across it ({@code rankStart} → {@code rankEnd}),
     * that season's development gains, and the season's news (prominence-sorted, the player's own milestones
     * included so the client can split them from the wider tour's). A null {@code season} defaults to the most
     * recently completed one (current season − 1); returns null when no season has completed yet. Requires a
     * player (callers guard with {@link #hasPlayer}).
     */
    public SeasonReviewDto seasonReview(String ownerId, String sessionId, Integer season) {
        World world = required(ownerId, sessionId).world();
        String id = requirePlayerId(world);
        int reviewed = season != null ? season : world.currentSeason() - 1;
        if (reviewed < 1) {
            return null; // no completed season to review yet
        }

        List<RankingSnapshot> snapshots = world.rankingSnapshots();
        Integer rankEnd = snapshotPosition(snapshots, reviewed, id);
        Integer rankStart = snapshotPosition(snapshots, reviewed - 1, id);

        StatLine line = world.seasonStatisticsOf(id, reviewed);
        SeasonStatDto stats = new SeasonStatDto(reviewed, line.events(), line.wins(), line.topTens(),
                line.cuts(), line.bestFinish(), line.earnings());

        List<DevelopmentDeltaDto> development = world.playerDevelopmentReport().stream()
                .filter(c -> c.season() == reviewed)
                .map(c -> new DevelopmentDeltaDto(c.attribute().name(), c.delta(), c.season()))
                .toList();

        List<NewsItemDto> headlines = world.newsFeed().stream()
                .filter(n -> n.season() == reviewed)
                .sorted(Comparator.comparingInt(NewsEvent::prominence).reversed())
                .map(n -> new NewsItemDto(n.season(), n.type().name(), n.headline(), n.prominence(),
                        n.subjectGolferId().orElse(null)))
                .toList();

        return new SeasonReviewDto(reviewed, rankStart, rankEnd, stats, development, headlines, id);
    }

    /** The golfer's world-ranking position from a season's end-of-season snapshot (null if none/unranked). */
    private static Integer snapshotPosition(List<RankingSnapshot> snapshots, int season, String golferId) {
        if (season < 1 || season > snapshots.size()) {
            return null;
        }
        return snapshots.get(season - 1).positionOf(golferId).orElse(null);
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

    /** The player's current support-team roster (spec: support-team). Requires a player (callers guard). */
    public List<StaffMember> playerStaff(String ownerId, String sessionId) {
        World world = required(ownerId, sessionId).world();
        return world.supportTeamOf(requirePlayerId(world)).members();
    }

    /** The player's current development focus as attribute enum names (empty when unset). */
    public List<String> playerDevelopmentFocus(String ownerId, String sessionId) {
        return required(ownerId, sessionId).world().playerDevelopmentFocus().stream()
                .map(Attribute::name).toList();
    }

    /** The player's golfer's development gains from its most recently developed season (for the report). */
    public List<DevelopmentDeltaDto> developmentReport(String ownerId, String sessionId) {
        return required(ownerId, sessionId).world().playerDevelopmentReport().stream()
                .map(c -> new DevelopmentDeltaDto(c.attribute().name(), c.delta(), c.season()))
                .toList();
    }

    /** The player's banked Development Points plus the cost-curve constants for client-side previews. */
    public PlayerDevelopmentDto playerDevelopment(String ownerId, String sessionId) {
        int points = required(ownerId, sessionId).world().playerDevelopmentPoints();
        return new PlayerDevelopmentDto(points, ProgressionConstants.POINTS_PER_RATING,
                ProgressionConstants.COST_GROWTH, ProgressionConstants.COST_REFERENCE);
    }

    /** Spends banked Development Points to raise the player's attributes; returns the new balance. */
    public int spendDevelopmentPoints(String ownerId, String sessionId, Map<Attribute, Integer> raises) {
        World world = required(ownerId, sessionId).world();
        world.spendDevelopmentPoints(raises);
        return world.playerDevelopmentPoints();
    }

    /** Hires a pending staff candidate by index (if affordable). */
    public void hireStaff(String ownerId, String sessionId, int index) {
        required(ownerId, sessionId).world().hireStaff(index);
    }

    /** Releases a current staff member of the player's team by role. */
    public void releaseStaff(String ownerId, String sessionId, StaffRole role) {
        required(ownerId, sessionId).world().releaseStaff(role);
    }

    /** The player's equipment upgrade offers awaiting a purchase decision, fit-scored to their build. */
    public List<EquipmentItemDto> pendingEquipmentOffers(String ownerId, String sessionId) {
        World world = required(ownerId, sessionId).world();
        Attributes build = world.careerOf(requirePlayerId(world)).player().attributes();
        return world.pendingEquipmentOffers().stream().map(i -> equipmentDto(i, build)).toList();
    }

    /** Buys a pending equipment upgrade by index (if affordable). */
    public void buyEquipment(String ownerId, String sessionId, int index) {
        required(ownerId, sessionId).world().buyEquipment(index);
    }

    /** The player's brand-deal offers awaiting a decision, each fit-scored to their build. */
    public List<EquipmentDealDto> pendingEquipmentDeals(String ownerId, String sessionId) {
        World world = required(ownerId, sessionId).world();
        Attributes build = world.careerOf(requirePlayerId(world)).player().attributes();
        return world.pendingEquipmentDeals().stream()
                .map(d -> dealDto(d, build, d.durationSeasons())).toList();
    }

    /** The player's active brand deal (with seasons remaining), or null when they are a free agent. */
    public EquipmentDealDto activeEquipmentDeal(String ownerId, String sessionId) {
        World world = required(ownerId, sessionId).world();
        EquipmentDeal deal = world.activeEquipmentDeal();
        if (deal == null) {
            return null;
        }
        Attributes build = world.careerOf(requirePlayerId(world)).player().attributes();
        return dealDto(deal, build, deal.seasonsRemaining(world.currentSeason()));
    }

    /** Signs a pending brand deal by index (pays the signing bonus, kits + equips the brand's bag, locks in). */
    public void acceptEquipmentDeal(String ownerId, String sessionId, int index) {
        required(ownerId, sessionId).world().acceptEquipmentDeal(index);
    }

    /** Projects a brand deal to its GraphQL view, scoring the brand's bias against the player's build. */
    private static EquipmentDealDto dealDto(EquipmentDeal deal, Attributes build, int seasonsRemaining) {
        double gearFit = EquipmentFit.fit(deal.brand().characteristics(deal.gearTier()), build);
        return new EquipmentDealDto(deal.brand().displayName(), deal.signingBonus(), deal.perSeasonRetainer(),
                deal.durationSeasons(), deal.gearTier(), gearFit, seasonsRemaining);
    }

    /** Sets the player's loadout for a category to one of their owned items. */
    public void selectLoadoutItem(String ownerId, String sessionId, EquipmentItem item) {
        required(ownerId, sessionId).world().selectLoadoutItem(item);
    }

    /** Every item the player currently owns, across all equipment categories, fit-scored to their build. */
    public List<EquipmentItemDto> playerEquipment(String ownerId, String sessionId) {
        World world = required(ownerId, sessionId).world();
        String id = requirePlayerId(world);
        Attributes build = world.careerOf(id).player().attributes();
        return world.equipmentInventoryOf(id).all().stream().map(i -> equipmentDto(i, build)).toList();
    }

    /** The item the player currently has selected in each equipment category, fit-scored to their build. */
    public List<EquipmentItemDto> playerLoadout(String ownerId, String sessionId) {
        World world = required(ownerId, sessionId).world();
        String id = requirePlayerId(world);
        Attributes build = world.careerOf(id).player().attributes();
        return world.tournamentLoadoutOf(id).selection().values().stream()
                .map(i -> equipmentDto(i, build)).toList();
    }

    /** Projects an equipment item to its GraphQL view, scoring its shape-fit against the player's build. */
    private static EquipmentItemDto equipmentDto(EquipmentItem i, Attributes build) {
        EquipmentCharacteristics c = i.characteristics();
        return new EquipmentItemDto(i.name(), i.category().name(), i.brand().displayName(), i.quality(), i.cost(),
                c.forgiveness(), c.power(), c.workability(), c.feel(), EquipmentFit.fit(c, build));
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

    /**
     * The situational pressure [0,1] the player currently feels in their event (spec: shot-resolution
     * pressure) — non-zero only on the closing rounds when in contention, peak in a playoff. Null when no
     * event is pending. The play surface reads it to explain why the final rounds are harder.
     */
    public Double playerPressure(String ownerId, String sessionId) {
        if (!hasPendingEvent(ownerId, sessionId)) {
            return null;
        }
        return playerEvent(ownerId, sessionId).currentPressure();
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
