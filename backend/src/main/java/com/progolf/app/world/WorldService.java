package com.progolf.app.world;

import com.progolf.app.api.dto.AttributeValueDto;
import com.progolf.sim.achievement.Achievement;
import com.progolf.app.api.dto.CalendarEntryDto;
import com.progolf.app.api.dto.CareerInboxDto;
import com.progolf.app.api.dto.CareerInboxItemDto;
import com.progolf.app.api.dto.CareerInboxKind;
import com.progolf.app.api.dto.CareerRecordsDto;
import com.progolf.app.api.dto.CurrentEventDto;
import com.progolf.app.api.dto.DevelopmentDeltaDto;
import com.progolf.app.api.dto.PlayerDevelopmentDto;
import com.progolf.app.api.dto.EventResultDto;
import com.progolf.app.api.dto.FinisherDto;
import com.progolf.app.api.dto.HallOfFameDto;
import com.progolf.app.api.dto.InjuryDto;
import com.progolf.app.api.dto.PlayerFitnessDto;
import com.progolf.app.api.dto.PlayerProfileDto;
import com.progolf.app.api.dto.ActiveSponsorshipDto;
import com.progolf.app.api.dto.EquipmentDealDto;
import com.progolf.app.api.dto.EquipmentItemDto;
import com.progolf.app.api.dto.SponsorshipStatusDto;
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
import com.progolf.sim.career.CareerEventRecord;
import com.progolf.sim.career.CareerStatistics;
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
import com.progolf.app.api.ApiMapper;
import com.progolf.app.api.dto.PlayingHoleDto;
import com.progolf.sim.course.GeneratedHole;
import com.progolf.sim.course.PinPosition;
import com.progolf.sim.play.PlayableEvent;
import com.progolf.sim.play.RoundScorecard;
import com.progolf.sim.play.ShotSituation;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Identity;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.progression.DevelopmentPoints;
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
import com.progolf.sim.world.WorldConstants;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
    /** Application-only completion of the Schedule review, keyed by session and scoped to one season. */
    private final ConcurrentMap<String, Integer> scheduleReviewAcknowledgements = new ConcurrentHashMap<>();
    /** Application-only completion of the optional Staff review, keyed by session and scoped to one season. */
    private final ConcurrentMap<String, Integer> staffReviewAcknowledgements = new ConcurrentHashMap<>();
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
        saveStore.save(ownerId, saveId, new SaveGame(session.seed(), world.config(), world.snapshot(), metadata,
                scheduleReviewAcknowledgements.get(session.id()), staffReviewAcknowledgements.get(session.id())));
    }

    /** Loads one of the owner's saves into a new session they own and returns it (the world continues identically). */
    public WorldSession load(String ownerId, String saveId) {
        SaveGame game = saveStore.load(ownerId, saveId);
        World world = World.restore(game.seed(), game.config(), game.snapshot());
        String sessionId = UUID.randomUUID().toString();
        WorldSession session = new WorldSession(sessionId, ownerId, game.seed(), world);
        sessions.put(sessionId, session);
        if (game.scheduleReviewAcknowledgedSeason() != null) {
            scheduleReviewAcknowledgements.put(sessionId, game.scheduleReviewAcknowledgedSeason());
        }
        if (game.staffReviewAcknowledgedSeason() != null) {
            staffReviewAcknowledgements.put(sessionId, game.staffReviewAcknowledgedSeason());
        }
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
                world.activePopulationSize(), world.hasPendingPlayerEvent(), world.playerEventAwaitingCompletion());
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
     * Records only that the player has completed the current season's Schedule review. This application-owned
     * acknowledgement never changes schedule choices or simulation state and is intentionally not a general
     * Inbox lifecycle model.
     */
    public boolean acknowledgeScheduleReview(String ownerId, String sessionId) {
        World world = required(ownerId, sessionId).world();
        if (world.playerGolferId().isEmpty() || world.currentWeek() != 1 || world.playerSchedule().isEmpty()) {
            return false;
        }
        scheduleReviewAcknowledgements.put(sessionId, world.currentSeason());
        return true;
    }

    /**
     * Records only that the player has reviewed the current season's optional Staff candidates. This
     * application-owned acknowledgement never changes candidates, staffing, funds, or simulation state.
     */
    public boolean acknowledgeStaffReview(String ownerId, String sessionId) {
        World world = required(ownerId, sessionId).world();
        if (world.playerGolferId().isEmpty()) {
            return false;
        }
        String playerId = world.playerGolferId().orElseThrow();
        double funds = world.financialAccountOf(playerId).availableFunds();
        boolean hasAffordableCandidate = world.pendingStaffOffers().stream()
                .anyMatch(candidate -> candidate.hiringCost() <= funds);
        if (!hasAffordableCandidate) {
            return false;
        }
        staffReviewAcknowledgements.put(sessionId, world.currentSeason());
        return true;
    }

    /**
     * The player's compact, current-state attention index (spec: career-inbox). This is deliberately an
     * application projection rather than a simulation subsystem: it aggregates only decisions the owning
     * career screens already govern and stores no Inbox/read state.
     */
    public CareerInboxDto careerInbox(String ownerId, String sessionId) {
        World world = required(ownerId, sessionId).world();
        if (world.playerGolferId().isEmpty()) {
            return new CareerInboxDto(List.of());
        }

        String playerId = world.playerGolferId().orElseThrow();
        double funds = world.financialAccountOf(playerId).availableFunds();
        List<CareerInboxItemDto> items = new ArrayList<>();

        int upcoming = (int) world.playerSchedule().stream()
                .filter(entry -> entry.week() >= world.currentWeek())
                .count();
        boolean scheduleAcknowledged = Integer.valueOf(world.currentSeason())
                .equals(scheduleReviewAcknowledgements.get(sessionId));
        if (world.currentWeek() == 1 && upcoming > 0 && !scheduleAcknowledged) {
            items.add(new CareerInboxItemDto(CareerInboxKind.SCHEDULE, upcoming));
        }

        int developmentPoints = world.playerDevelopmentPoints();
        if (hasAllocatableDevelopment(world, playerId, developmentPoints)) {
            items.add(new CareerInboxItemDto(CareerInboxKind.DEVELOPMENT, developmentPoints));
        }

        int sponsorships = world.pendingSponsorships().size();
        if (sponsorships > 0 && world.activeSponsorships().size() < world.maxConcurrentSponsorships()) {
            items.add(new CareerInboxItemDto(CareerInboxKind.SPONSORSHIP, sponsorships));
        }

        int equipment = (int) world.pendingEquipmentOffers().stream()
                .filter(item -> item.cost() <= funds)
                .count() + world.pendingEquipmentDeals().size();
        if (equipment > 0) {
            items.add(new CareerInboxItemDto(CareerInboxKind.EQUIPMENT, equipment));
        }

        int staff = (int) world.pendingStaffOffers().stream()
                .filter(candidate -> candidate.hiringCost() <= funds)
                .count();
        boolean staffAcknowledged = Integer.valueOf(world.currentSeason())
                .equals(staffReviewAcknowledgements.get(sessionId));
        if (staff > 0 && !staffAcknowledged) {
            items.add(new CareerInboxItemDto(CareerInboxKind.STAFF, staff));
        }

        return new CareerInboxDto(items);
    }

    /** Returns whether the player can make at least one valid Development decision with their banked points. */
    private static boolean hasAllocatableDevelopment(World world, String playerId, int points) {
        if (points <= 0) {
            return false;
        }
        Career career = world.careerOf(playerId);
        Attributes attributes = career.player().attributes();
        Attributes potential = career.player().potential();
        for (Attribute attribute : Attribute.values()) {
            if (attributes.get(attribute) < potential.get(attribute)
                    && points >= DevelopmentPoints.costToRaise(attributes.get(attribute))) {
                return true;
            }
        }
        return false;
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
     * The player's achievements: the full catalogue in display order, each mapped to the season it was
     * unlocked ({@code null} when still locked). Requires a player (callers guard with {@link #hasPlayer}).
     */
    public Map<Achievement, Integer> playerAchievements(String ownerId, String sessionId) {
        return required(ownerId, sessionId).world().playerAchievements();
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

    /**
     * The player's career records (spec: career-records): headline totals + scoring bests, and their full
     * per-event history grouped by event so recurring wins accumulate. Requires a player (callers guard with
     * {@link #hasPlayer}). Counts come from the career statistics; scoring bests come from the rich ledger.
     */
    public CareerRecordsDto careerRecords(String ownerId, String sessionId) {
        World world = required(ownerId, sessionId).world();
        String id = requirePlayerId(world);
        CareerStatistics cs = world.careerOf(id).statistics();
        List<CareerEventRecord> ledger = world.playerCareerRecords();

        Integer bestFinish = null;
        CareerEventRecord lowestRoundEvent = null; // the event holding the single best round
        int lowestRoundToPar = Integer.MAX_VALUE;
        int lowestRoundNo = 0;
        CareerEventRecord lowestTournamentEvent = null; // best full (made-cut) tournament total
        int lowestTournamentToPar = Integer.MAX_VALUE;
        for (CareerEventRecord r : ledger) {
            bestFinish = bestFinish == null ? r.position() : Math.min(bestFinish, r.position());
            if (r.madeCut() && r.scoreToPar() < lowestTournamentToPar) {
                lowestTournamentToPar = r.scoreToPar();
                lowestTournamentEvent = r;
            }
            List<Integer> rounds = r.roundScores();
            for (int i = 0; i < rounds.size(); i++) {
                if (rounds.get(i) < lowestRoundToPar) {
                    lowestRoundToPar = rounds.get(i);
                    lowestRoundEvent = r;
                    lowestRoundNo = i + 1;
                }
            }
        }
        CareerRecordsDto.Summary summary = new CareerRecordsDto.Summary(
                cs.eventsPlayed(), cs.wins(), cs.majorsWon(), cs.runnerUps(), cs.topTens(), cs.cutsMade(),
                bestFinish,
                lowestRoundEvent == null ? null : highlight(lowestRoundEvent, lowestRoundToPar, lowestRoundNo),
                lowestTournamentEvent == null ? null : highlight(lowestTournamentEvent, lowestTournamentToPar, null),
                cs.totalEarnings());

        // Group by the event's stable name (recurring events accumulate). Insertion order = chronological.
        Map<String, List<CareerEventRecord>> byEvent = new LinkedHashMap<>();
        for (CareerEventRecord r : ledger) {
            byEvent.computeIfAbsent(r.eventName(), k -> new ArrayList<>()).add(r);
        }
        List<CareerRecordsDto.EventHistory> events = new ArrayList<>();
        byEvent.forEach((name, group) -> events.add(toEventHistory(name, group)));
        // Prestige first (majors → championships → signatures → regular), then most-decorated within a band:
        // wins, then best finish, then name (stable). The UI splits by tour, preserving this order per section.
        events.sort(Comparator.comparingInt((CareerRecordsDto.EventHistory e) -> prestigeRank(e.prestige()))
                .thenComparing(Comparator.comparingInt(CareerRecordsDto.EventHistory::wins).reversed())
                .thenComparingInt(CareerRecordsDto.EventHistory::bestPosition)
                .thenComparing(CareerRecordsDto.EventHistory::eventName));
        return new CareerRecordsDto(summary, events);
    }

    /** Sort rank for an event prestige — majors first, regular last (spec: event-prestige). */
    private static int prestigeRank(String prestige) {
        return switch (prestige) {
            case "MAJOR" -> 0;
            case "TOUR_CHAMPIONSHIP" -> 1;
            case "SIGNATURE" -> 2;
            default -> 3; // REGULAR (and anything unmapped)
        };
    }

    /** Builds a scoring highlight from the event it was set in, with a realistic calendar date. */
    private static CareerRecordsDto.ScoringHighlight highlight(CareerEventRecord r, int scoreToPar, Integer round) {
        return new CareerRecordsDto.ScoringHighlight(scoreToPar, r.eventName(), r.location(), r.season(),
                displayDate(r.season(), r.week()), round);
    }

    /** Folds one event's grouped records into an {@link CareerRecordsDto.EventHistory} (results best-first). */
    private static CareerRecordsDto.EventHistory toEventHistory(String name, List<CareerEventRecord> group) {
        int wins = 0;
        int bestPosition = Integer.MAX_VALUE;
        for (CareerEventRecord r : group) {
            if (r.won()) {
                wins++;
            }
            bestPosition = Math.min(bestPosition, r.position());
        }
        // The most recent appearance supplies the display location/prestige/tier (group is chronological).
        CareerEventRecord latest = group.get(group.size() - 1);
        List<CareerRecordsDto.Result> results = new ArrayList<>(group.stream().map(WorldService::toResult).toList());
        results.sort(Comparator.comparingInt(CareerRecordsDto.Result::position)
                .thenComparingInt(CareerRecordsDto.Result::scoreToPar));
        return new CareerRecordsDto.EventHistory(name, latest.location(), latest.prestige(), latest.tier(),
                latest.tourTier(), group.size(), wins, bestPosition, results);
    }

    /** Projects one ledger record into its GraphQL result view (with a realistic calendar date). */
    private static CareerRecordsDto.Result toResult(CareerEventRecord r) {
        return new CareerRecordsDto.Result(r.season(), displayDate(r.season(), r.week()), r.position(),
                r.scoreToPar(), r.won(), r.madeCut(), r.location(), r.prize(), r.fairwaysHit(),
                r.fairwaysPossible(), r.greensInRegulation(), r.holesPlayed(), r.putts(), r.roundScores());
    }

    /**
     * A realistic golf-calendar date for a (season, week) turn (spec: career-records realism): each season is
     * a calendar year from {@link WorldConstants#BASE_YEAR}, and week 1 is the first Thursday of April — the
     * same nominal Apr–Oct window the season calendar renders — so records read like real tour dates rather
     * than the engine's continuous week clock. Returned as an ISO date string.
     */
    private static String displayDate(int season, int week) {
        int year = WorldConstants.BASE_YEAR + Math.max(0, season - 1);
        java.time.LocalDate seasonStart = java.time.LocalDate.of(year, 4, 1)
                .with(java.time.temporal.TemporalAdjusters.firstInMonth(java.time.DayOfWeek.THURSDAY));
        return seasonStart.plusWeeks(Math.max(0, week - 1)).toString();
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

    /** Accepts a pending sponsorship offer by index; returns false when the concurrent-agreement cap blocks it. */
    public boolean acceptSponsorship(String ownerId, String sessionId, int index) {
        return required(ownerId, sessionId).world().acceptSponsorship(index);
    }

    /** The player's sponsorship book: the concurrency cap and the currently-active (signed) agreements. */
    public SponsorshipStatusDto sponsorshipStatus(String ownerId, String sessionId) {
        World world = required(ownerId, sessionId).world();
        int season = world.currentSeason();
        List<ActiveSponsorshipDto> active = world.activeSponsorships().stream()
                .map(a -> new ActiveSponsorshipDto(a.sponsor(), a.industry(), a.perSeasonPayment(),
                        a.lastActiveSeason() - season + 1))
                .toList();
        return new SponsorshipStatusDto(world.maxConcurrentSponsorships(), active);
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

    /**
     * Presentation context for the player's pending event — its display name, place, and host course type —
     * or {@code null} when no event is pending. The play surface reads it to title the screen and choose a
     * scene backdrop.
     */
    public CurrentEventDto currentEvent(String ownerId, String sessionId) {
        return required(ownerId, sessionId).world().currentEventScene()
                .map(s -> new CurrentEventDto(s.name(), s.location(), s.courseType()))
                .orElse(null);
    }

    /**
     * The geometry of a hole in the player's pending event for rendering (spec: web-hole-visualization) — the
     * current hole by default, or an explicit {@code holeNumber} (1..18) for pre-fetch, with the pin reflecting
     * the round in progress. {@code null} when the event has been played to the end (no live hole to render).
     */
    public PlayingHoleDto currentPlayingHole(String ownerId, String sessionId, Integer holeNumber) {
        PlayableEvent event = playerEvent(ownerId, sessionId);
        if (event.isComplete()) {
            return null;
        }
        GeneratedHole hole = holeNumber == null ? event.currentHole() : event.holeGeometry(holeNumber);
        PinPosition pin = holeNumber == null ? event.currentPin() : event.pinAt(holeNumber);
        // The hole's biome keys on the event's canonical scene token — the same token the scene backdrop uses —
        // so the illustration agrees with the event's name/place/photo (a links major renders as links, whatever
        // its randomly-assigned host course). Falls back to the raw host classification only if no event is pending.
        String courseType = required(ownerId, sessionId).world().currentEventScene()
                .map(World.PendingEventScene::courseType)
                .orElseGet(() -> event.classification().name());
        var geometry = event.effectiveGeometry(hole.number());
        var ball = holeNumber == null ? event.currentBallState()
                : new com.progolf.sim.shot.BallState(geometry.tee(), com.progolf.sim.spatial.Surface.TEE_BOX);
        return ApiMapper.playingHole(hole, pin, courseType, ball, geometry);
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
