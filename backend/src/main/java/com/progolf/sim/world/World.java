package com.progolf.sim.world;

import com.progolf.sim.career.Career;
import com.progolf.sim.career.CareerConstants;
import com.progolf.sim.career.CareerStatistics;
import com.progolf.sim.career.HallOfFame;
import com.progolf.sim.career.HallOfFameCredentials;
import com.progolf.sim.career.HallOfFameInduction;
import com.progolf.sim.control.CareerGoal;
import com.progolf.sim.control.PlayerControl;
import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.Rng;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.core.Seeds;
import com.progolf.sim.core.SplitMix64Rng;
import com.progolf.sim.economy.CommercialReputation;
import com.progolf.sim.economy.EconomyConstants;
import com.progolf.sim.economy.FinancialAccount;
import com.progolf.sim.economy.PerformanceSnapshot;
import com.progolf.sim.economy.SponsorshipAgreement;
import com.progolf.sim.economy.SponsorshipMarket;
import com.progolf.sim.economy.SponsorshipOffer;
import com.progolf.sim.economy.TransactionType;
import com.progolf.sim.equipment.AcquisitionPolicy;
import com.progolf.sim.equipment.EquipmentAcquisition;
import com.progolf.sim.equipment.EquipmentCatalogue;
import com.progolf.sim.equipment.EquipmentCategory;
import com.progolf.sim.equipment.EquipmentConstants;
import com.progolf.sim.equipment.EquipmentInventory;
import com.progolf.sim.equipment.EquipmentItem;
import com.progolf.sim.equipment.GolfBag;
import com.progolf.sim.equipment.TournamentLoadout;
import com.progolf.sim.health.HealthConstants;
import com.progolf.sim.health.HealthEvent;
import com.progolf.sim.health.HealthSystem;
import com.progolf.sim.health.PhysicalState;
import com.progolf.sim.media.CareerNarrative;
import com.progolf.sim.media.MediaConstants;
import com.progolf.sim.media.MediaSystem;
import com.progolf.sim.media.NewsEvent;
import com.progolf.sim.media.NewsFactory;
import com.progolf.sim.statistics.CareerComparison;
import com.progolf.sim.statistics.Championship;
import com.progolf.sim.statistics.EventOutcome;
import com.progolf.sim.statistics.RecordHolder;
import com.progolf.sim.statistics.RecordType;
import com.progolf.sim.statistics.StatLine;
import com.progolf.sim.statistics.StatisticsArchive;
import com.progolf.sim.player.AttributeChange;
import com.progolf.sim.staff.HiringPolicy;
import com.progolf.sim.staff.StaffConstants;
import com.progolf.sim.staff.StaffMarket;
import com.progolf.sim.staff.StaffMember;
import com.progolf.sim.staff.StaffRole;
import com.progolf.sim.staff.SupportTeam;
import com.progolf.sim.course.Course;
import com.progolf.sim.course.CourseGenerator;
import com.progolf.sim.course.EnvironmentClassification;
import com.progolf.sim.player.ProfessionalGolfer;
import com.progolf.sim.play.PlayableEvent;
import com.progolf.sim.player.Archetype;
import com.progolf.sim.player.Nationality;
import com.progolf.sim.population.GolferFactory;
import com.progolf.sim.population.PopulationGenerator;
import com.progolf.sim.progression.ProgressionEngine;
import com.progolf.sim.ranking.RankingHistory;
import com.progolf.sim.ranking.RankingSnapshot;
import com.progolf.sim.ranking.WorldRanking;
import com.progolf.sim.tour.TourSystem;
import com.progolf.sim.tour.TourTier;
import com.progolf.sim.tournament.EntryRequirements;
import com.progolf.sim.tournament.EventPrestige;
import com.progolf.sim.tournament.PrizeStructure;
import com.progolf.sim.tournament.Tier;
import com.progolf.sim.tournament.TournamentConstants;
import com.progolf.sim.tournament.Tournament;
import com.progolf.sim.tournament.TournamentDefinition;
import com.progolf.sim.tournament.TournamentFormat;
import com.progolf.sim.tournament.TournamentResult;
import com.progolf.sim.weather.EnvironmentalRecord;
import com.progolf.sim.weather.TournamentWeather;
import com.progolf.sim.weather.WeatherConstants;
import com.progolf.sim.weather.WeatherSystem;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The persistent World (REQ-101–112): the top-level coordinator that owns the calendar and population and
 * drives the simulation forward. It advances week by week, resolves scheduled events automatically
 * through the shared Tournament engine, feeds results into the World Ranking, Tour Season Standings, and
 * each Career, and runs a seasonal transition that fires every domain seam.
 *
 * <p>Pure coordinator: it composes the owning domains through their public operations. It computes no
 * shot outcomes, ranking points, or finances (REQ-112); it never branches on control type (REQ-104); and
 * it progresses regardless of the player (REQ-102). Everything derives from the master seed, so an entire
 * world is reproducible (REQ-299).
 */
public final class World {

    private final long masterSeed;
    private final WorldConfig config;
    private final WorldCalendar calendar;

    private final List<Course> coursePool = new ArrayList<>();
    private final WeatherSystem weatherSystem;
    private final List<EnvironmentalRecord> environmentalHistory = new ArrayList<>();
    private final SponsorshipMarket sponsorshipMarket = new SponsorshipMarket();
    private final Map<String, FinancialAccount> accounts = new LinkedHashMap<>();
    private final Map<String, PhysicalState> physicalStates = new LinkedHashMap<>();
    private final List<HealthEvent> healthHistory = new ArrayList<>();
    private final Map<String, SupportTeam> supportTeams = new LinkedHashMap<>();
    private final StaffMarket staffMarket = new StaffMarket();
    private final Map<String, EquipmentInventory> equipment = new LinkedHashMap<>();
    private final Map<String, TournamentLoadout> loadouts = new LinkedHashMap<>();
    private final MediaSystem media = new MediaSystem();
    private String previousNumberOne; // for detecting world number-one changes
    private final Set<String> announcedProspects = new LinkedHashSet<>(); // rising prospects reported once
    private final StatisticsArchive statistics = new StatisticsArchive();
    private PlayerControl playerControl; // null = fully autonomous world (unchanged behaviour)
    private final List<SponsorshipOffer> playerPendingOffers = new ArrayList<>();
    // For the player's golfer, staff hiring and equipment purchases defer to the human: the World generates
    // candidates/upgrades each season and holds them as pending offers (spec: player-control).
    private final List<StaffMember> playerPendingStaff = new ArrayList<>();
    private final List<EquipmentItem> playerPendingEquipment = new ArrayList<>();
    private PendingPlayerEvent pendingEvent; // non-null = the week is paused awaiting the player's event
    // Golfers already committed to an event this week — a golfer plays at most one event per week, so a
    // major's cross-tour field excludes them from concurrent tour events (spec: event-prestige).
    private final Set<String> committedThisWeek = new LinkedHashSet<>();
    // Career goals the player has already been congratulated for, so each is announced once (spec: career-goals).
    private final Set<CareerGoal> achievedGoals = new LinkedHashSet<>();
    private final TourSystem tours = new TourSystem();
    private final WorldRanking ranking = new WorldRanking();
    private final Map<String, ProfessionalGolfer> golfers = new LinkedHashMap<>();
    private final Map<String, Career> careers = new LinkedHashMap<>();
    private final Set<String> activeGolfers = new LinkedHashSet<>();

    private final List<SeasonArchive> archives = new ArrayList<>();
    private final List<RankingSnapshot> rankingSnapshots = new ArrayList<>();

    // Hall of Fame (spec: career-legacy): the permanent registry of inductions, the set of inducted ids
    // for O(1) membership, and the season each golfer retired (to derive seasons-since-retirement).
    private final List<HallOfFameInduction> hallOfFameInductions = new ArrayList<>();
    private final Set<String> hallOfFameMembers = new LinkedHashSet<>();
    private final Map<String, Integer> retirementSeason = new LinkedHashMap<>();

    private List<ScheduledTournament> schedule = new ArrayList<>();
    private List<TournamentResult> seasonResults = new ArrayList<>();
    private long nextTournamentId = 1;
    private int replenishCounter = 0;

    private World(long masterSeed, WorldConfig config) {
        this.masterSeed = masterSeed;
        this.config = config;
        this.weatherSystem = new WeatherSystem(masterSeed);
        this.calendar = new WorldCalendar(config.weeksPerSeason(), WorldConstants.BASE_YEAR);
    }

    /** Creates and bootstraps a World from a master seed and configuration. */
    public static World create(long masterSeed, WorldConfig config) {
        World world = new World(masterSeed, config);
        world.bootstrap();
        return world;
    }

    /** Creates a default-sized World. */
    public static World create(long masterSeed) {
        return create(masterSeed, WorldConfig.defaults());
    }

    // --- Bootstrap ---

    private void bootstrap() {
        // Course pool.
        EnvironmentClassification[] classes = EnvironmentClassification.values();
        for (int i = 0; i < config.coursePoolSize(); i++) {
            SeedCoordinate coord = new SeedCoordinate(masterSeed, 0, i, 0, 0, 0, 0);
            coursePool.add(CourseGenerator.generate(coord, classes[i % classes.length]));
        }

        // Population, distributed across tiers by initial skill (strongest to the top tiers).
        List<ProfessionalGolfer> population =
                PopulationGenerator.generate(new SeedCoordinate(masterSeed, 1, 0, 0, 0, 0, 0), config.populationSize());
        List<ProfessionalGolfer> ranked = new ArrayList<>(population);
        ranked.sort(Comparator.comparingDouble(World::meanAttribute).reversed()
                .thenComparing(g -> g.player().id()));

        int pop = ranked.size();
        int eliteN = (int) Math.round(pop * WorldConstants.ELITE_FRACTION);
        int primaryN = (int) Math.round(pop * WorldConstants.PRIMARY_FRACTION);
        int secondaryN = (int) Math.round(pop * WorldConstants.SECONDARY_FRACTION);
        for (int i = 0; i < pop; i++) {
            ProfessionalGolfer g = ranked.get(i);
            TourTier tier;
            if (i < eliteN) {
                tier = TourTier.ELITE;
            } else if (i < eliteN + primaryN) {
                tier = TourTier.PRIMARY;
            } else if (i < eliteN + primaryN + secondaryN) {
                tier = TourTier.SECONDARY;
            } else {
                tier = TourTier.DEVELOPMENT;
            }
            admit(g, tier);
        }

        schedule = generateSchedule(calendar.currentSeason());
    }

    /** Registers a golfer into the world: membership, career, active set. */
    private void admit(ProfessionalGolfer golfer, TourTier tier) {
        String id = golfer.player().id();
        golfers.put(id, golfer);
        careers.put(id, new Career(golfer.player(), startAgeOf(golfer)));
        accounts.put(id, new FinancialAccount(id, calendar.currentDate()));
        physicalStates.put(id, HealthSystem.initialState(
                new SplitMix64Rng(Seeds.deriveSeed(Seeds.deriveSeed(masterSeed, HealthConstants.HEALTH_SALT), id.hashCode()))));
        supportTeams.put(id, new SupportTeam());
        // Standard starting kit: one baseline item per category (owned), and a loadout selecting them.
        EquipmentInventory inventory = new EquipmentInventory();
        for (EquipmentCategory category : EquipmentCategory.values()) {
            inventory.add(EquipmentCatalogue.standardItem(category), calendar.currentSeason(),
                    EquipmentAcquisition.Method.INITIAL);
        }
        equipment.put(id, inventory);
        loadouts.put(id, TournamentLoadout.bestFrom(inventory));
        tours.register(id, tier);
        activeGolfers.add(id);
    }

    // --- Weekly progression ---

    /**
     * Advances the world by one week (REQ-105): resolves this week's scheduled events; on the final week
     * of the season runs the seasonal transition. Progresses with or without any player action.
     */
    public void advanceWeek() {
        if (pendingEvent != null) {
            throw new IllegalStateException("A player event is in progress; complete it before advancing");
        }
        int week = calendar.currentWeek();
        List<ScheduledTournament> weekEvents = new ArrayList<>();
        for (ScheduledTournament event : schedule) {
            if (event.week() == week) {
                weekEvents.add(event);
            }
        }
        // Majors resolve first each week: they are the marquee events, and a golfer plays at most one event
        // per week — a major's cross-tour field is committed before the concurrent tour events draw theirs
        // (spec: event-prestige). Ordering is otherwise the stable schedule order.
        weekEvents.sort(Comparator.comparingInt((ScheduledTournament e) -> e.prestige().isMajor() ? 0 : 1));
        committedThisWeek.clear();
        resumeWeek(weekEvents, 0);
    }

    /**
     * Resolves this week's events from index {@code from} onward. Non-player events resolve automatically;
     * when the designated player is entered in an event this yields — building a pending interactive event
     * and returning before recovery/transition/advance (spec: playable-event) — to be resumed by
     * {@link #completePlayerEvent}. When all events are resolved it finishes the week.
     */
    private void resumeWeek(List<ScheduledTournament> weekEvents, int from) {
        for (int i = from; i < weekEvents.size(); i++) {
            ScheduledTournament event = weekEvents.get(i);
            if (isPlayerEntered(event)) {
                BuiltEvent built = buildEvent(event);
                if (built != null && built.playerFieldIndex() >= 0) {
                    built.tournament().designateInteractiveCompetitor(built.playerFieldIndex());
                    PlayableEvent playable = new PlayableEvent(built.tournament(),
                            golfers.get(playerControl.golferId()), built.playerFieldIndex(),
                            built.course(), built.weather(), masterSeed, built.season(), event.tournamentId());
                    pendingEvent = new PendingPlayerEvent(playable, built, weekEvents, i);
                    return; // pause the week until the player's event completes
                }
                if (built != null) {
                    feedConsumers(built, built.tournament().playToCompletion());
                }
                continue;
            }
            resolveEvent(event);
        }
        finishWeek();
    }

    /** Weekly wrap-up after all of this week's events: recovery, any seasonal transition, then the calendar. */
    private void finishWeek() {
        recoverHealth(); // fatigue recovers and rehabilitation advances each week (REQ-218/220)
        if (calendar.isSeasonEnd()) {
            seasonalTransition();
        }
        calendar.advance();
    }

    /**
     * Completes the player's pending interactive event (spec: playable-event): its result feeds every
     * consumer exactly as an automatic resolution would, and the paused week resumes to completion. The
     * event's play (all rounds and any playoff) must be finished first.
     */
    public void completePlayerEvent() {
        if (pendingEvent == null) {
            throw new IllegalStateException("No player event is pending");
        }
        PlayableEvent playable = pendingEvent.event();
        if (!playable.isComplete()) {
            throw new IllegalStateException("The player's event is not finished");
        }
        feedConsumers(pendingEvent.built(), playable.result());
        List<ScheduledTournament> weekEvents = pendingEvent.weekEvents();
        int next = pendingEvent.cursor() + 1;
        pendingEvent = null;
        resumeWeek(weekEvents, next);
    }

    /** Advances every active golfer's recovery by one week, recording comebacks from significant injuries. */
    private void recoverHealth() {
        int season = calendar.currentSeason();
        for (String id : activeGolfers) {
            PhysicalState before = physicalStates.get(id);
            PhysicalState after = HealthSystem.recoverWeek(before, careers.get(id).age());
            // Fitness coach / physiotherapist add extra weekly recovery (REQ-197).
            double recoveryBonus = supportTeams.get(id).effects().recoveryBonus();
            if (recoveryBonus > 0) {
                after = after.withFatigue(Math.max(0.0, after.fatigue() - recoveryBonus));
            }
            physicalStates.put(id, after);
            if (before.injury().isPresent() && after.injury().isEmpty()
                    && before.injury().get().severity().significant()) {
                String description = "Returned from " + before.injury().get().severity() + " " + before.injury().get().type();
                healthHistory.add(new HealthEvent(season, id, HealthEvent.Type.COMEBACK, description));
                media.publish(NewsFactory.comeback(season, id, nameOf(id), description));
            }
        }
    }

    /** Advances a whole season (its remaining weeks). Any pending player event is simmed so a bulk advance
     * runs the world unattended (spec: playable-event; the player can still play events week by week). */
    public void advanceSeason() {
        int startSeason = calendar.currentSeason();
        while (calendar.currentSeason() == startSeason) {
            advanceWeek();
            while (pendingEvent != null) {
                pendingEvent.event().simEvent();
                completePlayerEvent();
            }
        }
    }

    /** Automatically resolves an event end to end (unchanged behaviour): build, play, feed every consumer. */
    private void resolveEvent(ScheduledTournament event) {
        BuiltEvent built = buildEvent(event);
        if (built == null) {
            return; // no eligible field this week
        }
        feedConsumers(built, built.tournament().playToCompletion());
    }

    /**
     * Builds an event up to a confirmed Tournament ready to play: draws and gates the field, generates the
     * weather, syncs each competitor's fatigue and equipment into the shot engine, and registers the field.
     * Returns {@code null} when no eligible field exists. Shared by automatic resolution and the player's
     * interactive event (spec: playable-event); it computes no results and feeds no consumer.
     */
    private BuiltEvent buildEvent(ScheduledTournament event) {
        int season = calendar.currentSeason();
        LocalDate date = calendar.dateFor(season, event.week());

        // A major draws the strongest field across all tiers (cross-tour); regular/signature events draw
        // from their own tour's standings (spec: event-prestige).
        List<ProfessionalGolfer> field = event.prestige().isMajor()
                ? majorField(date, event)
                : tours.standings(event.tier()).stream()
                        .filter(activeGolfers::contains)
                        .filter(id -> physicalStates.get(id).canCompete()) // availability gates entry (REQ-221)
                        .filter(id -> !committedThisWeek.contains(id)) // one event per week (spec: event-prestige)
                        .filter(id -> !(isPlayer(id) && playerSitsOut(event))) // player skipped/rested (player-control)
                        .limit(config.fieldSize())
                        .map(golfers::get)
                        .toList();
        if (field.isEmpty()) {
            return null; // no eligible field this week
        }
        // Commit this field: no golfer here may be drawn into another event the same week.
        for (ProfessionalGolfer g : field) {
            committedThisWeek.add(g.player().id());
        }

        Course course = coursePool.get(event.courseIndex());
        Tier tier = mapTier(event.tier());

        // The cut is sized to the actual field so it bites at any scale (~45% make the weekend); making the
        // cut is the pay line — only the made-cut positions earn, on a tier/prestige-scaled purse (spec:
        // add-world-scale / financial-strategy).
        int cutSize = Math.max(1, (int) Math.round(field.size() * TournamentConstants.CUT_FRACTION));
        TournamentFormat format = new TournamentFormat(TournamentConstants.ROUNDS, true, cutSize);
        TournamentDefinition def = new TournamentDefinition(
                eventName(event, tier), course, tier, event.prestige(),
                new EntryRequirements(config.fieldSize(), true),
                PrizeStructure.forEvent(tier, event.prestige(), cutSize), format, date,
                masterSeed, season, event.tournamentId());

        // Weather is generated before play from the course's climate and the point in the season; the
        // whole field plays under the same per-round conditions (REQ-228/231/232).
        int weeksPerSeason = calendar.weeksPerSeason();
        double seasonPhase = weeksPerSeason > 1 ? (double) (event.week() - 1) / (weeksPerSeason - 1) : 0.0;
        TournamentWeather weather = weatherSystem.generate(
                season, event.tournamentId(), seasonPhase,
                course.identity().classification(), def.format().rounds());

        // Before play, sync each competitor's temporary state into the shot engine: accumulated fatigue
        // (REQ-225), the active Golf Bag's characteristics (REQ-206), and the Support Team's shot influence
        // (caddie/psychologist, spec: staff-influence) — all read via toGolferState.
        int playerFieldIndex = -1;
        for (int i = 0; i < field.size(); i++) {
            ProfessionalGolfer g = field.get(i);
            String id = g.player().id();
            g.player().state().setFatigue(physicalStates.get(id).fatigue());
            GolfBag bag = GolfBag.fromLoadout(loadouts.get(id));
            g.player().state().setEquipment(bag.forgivenessBonus(), bag.powerBonus(),
                    bag.workabilityBonus(), bag.feelBonus());
            var effects = supportTeams.get(id).effects();
            g.player().state().setSupport(effects.mentalSupport(), effects.strategicSupport());
            if (isPlayer(id)) {
                playerFieldIndex = i;
            }
        }

        Tournament tournament = new Tournament(def, weather);
        tournament.openRegistration();
        for (ProfessionalGolfer g : field) {
            tournament.register(g);
        }
        tournament.confirmField();
        return new BuiltEvent(event, tournament, field, def, tier, date, weather, course, season, playerFieldIndex);
    }

    /**
     * Feeds a completed event's result to every consumer — ranking, tour standings, careers, media,
     * statistics, economy, and health — each owning its own computation (REQ-112). Identical whether the
     * result came from automatic resolution or the player's interactive event (spec: playable-event).
     */
    private void feedConsumers(BuiltEvent built, TournamentResult result) {
        ScheduledTournament event = built.event();
        TournamentDefinition def = built.def();
        Tier tier = built.tier();
        LocalDate date = built.date();
        TournamentWeather weather = built.weather();
        List<ProfessionalGolfer> field = built.field();
        int season = built.season();

        EventPrestige prestige = def.prestige();
        ProfessionalGolfer winner = result.winner();
        String winnerId = winner.player().id();
        // The winner's ranking BEFORE this event feeds the upset test (a low-ranked winner is an upset).
        int winnerRankBefore = ranking.rankingAsOf(date).positionOf(winnerId).orElse(Integer.MAX_VALUE);

        // Preserve historically significant environmental context (REQ-235) and report it (REQ-241).
        if (weather.severity() >= WeatherConstants.SEVERITY_THRESHOLD) {
            environmentalHistory.add(new EnvironmentalRecord(
                    season, event.tournamentId(), weather.severity(),
                    def.name() + " played in severe conditions"));
            media.publish(NewsFactory.severeWeather(season, def.name()));
        }

        // Feed the one result to every consumer (each owns its own computation), weighted by prestige.
        ranking.record(result, tier, prestige, date, event.tournamentId());
        tours.recordResult(result, event.tier());
        for (ProfessionalGolfer g : field) {
            careers.get(g.player().id()).recordTournament(result, prestige, tier, date);
        }

        // Media: the win (a major victory is the biggest news), any maiden title, and any upset (REQ-241/242).
        String winnerName = winner.player().identity().fullName();
        media.publish(prestige.isMajor()
                ? NewsFactory.majorVictory(season, winnerId, winnerName, def.name())
                : NewsFactory.tournamentVictory(season, winnerId, winnerName, def.name()));
        if (careers.get(winnerId).statistics().wins() == 1) {
            media.publish(NewsFactory.maidenVictory(season, winnerId, winnerName, def.name()));
        }
        if (winnerRankBefore > MediaConstants.UPSET_RANKING_THRESHOLD) {
            media.publish(NewsFactory.majorUpset(season, winnerId, winnerName, def.name(), winnerRankBefore));
        }

        // Statistics: record every finish and the champion (with its prestige) into the archive (REQ-251/254/256).
        for (TournamentResult.Finish finish : result.finishingOrder()) {
            String id = finish.golfer().player().id();
            var shots = finish.shotStats(); // shot-level stats (spec: competitive-statistics)
            EventOutcome outcome = new EventOutcome(season, id, finish.position(), finish.score(),
                    finish.madeCut(), finish.withdrawn(), finish.prize(),
                    shots.fairwaysHit(), shots.fairwaysPossible(), shots.greensInRegulation(),
                    shots.holesPlayed(), shots.putts());
            statistics.observeEvent(outcome, def.name(), tier.name(), prestige.name(), id.equals(winnerId));
        }

        // Economy: competing costs entry + travel; a paying finish awards prize money (REQ-178/182).
        double entryFee = entryFeeFor(event.tier());
        for (ProfessionalGolfer g : field) {
            FinancialAccount account = accounts.get(g.player().id());
            account.charge(TransactionType.ENTRY_FEE, entryFee, date, "Entry: " + def.name());
            account.charge(TransactionType.TRAVEL, EconomyConstants.TRAVEL_COST, date, "Travel: " + def.name());
        }
        for (TournamentResult.Finish finish : result.finishingOrder()) {
            if (finish.prize() > 0) {
                accounts.get(finish.golfer().player().id())
                        .award(TransactionType.PRIZE_MONEY, finish.prize(), date, "Prize: " + def.name());
            }
        }

        // Health: competing accrues fatigue and may cause an injury (REQ-217/219).
        for (ProfessionalGolfer g : field) {
            String id = g.player().id();
            PhysicalState before = physicalStates.get(id);
            SplitMix64Rng rng = new SplitMix64Rng(Seeds.deriveSeed(Seeds.deriveSeed(
                    Seeds.deriveSeed(Seeds.deriveSeed(masterSeed, HealthConstants.HEALTH_SALT), season),
                    event.tournamentId()), id.hashCode()));
            PhysicalState after = HealthSystem.afterParticipation(before, careers.get(id).age(), rng);
            physicalStates.put(id, after);
            if (before.injury().isEmpty() && after.injury().isPresent()
                    && after.injury().get().severity().significant()) {
                String description = after.injury().get().severity() + " " + after.injury().get().type() + " injury";
                healthHistory.add(new HealthEvent(season, id, HealthEvent.Type.INJURY, description));
                media.publish(NewsFactory.injury(season, id, nameOf(id), description));
            }
        }

        seasonResults.add(result);
        checkCareerGoals(); // a win/major/ranking move may complete a player goal (spec: career-goals)
    }

    /** Per-event entry fee by tour tier (more prestigious tours cost more to enter). */
    private static double entryFeeFor(TourTier tier) {
        return switch (tier) {
            case ELITE -> EconomyConstants.ENTRY_FEE_ELITE;
            case PRIMARY -> EconomyConstants.ENTRY_FEE_PRIMARY;
            case SECONDARY -> EconomyConstants.ENTRY_FEE_SECONDARY;
            case DEVELOPMENT -> EconomyConstants.ENTRY_FEE_DEVELOPMENT;
        };
    }

    // --- Seasonal transition ---

    private void seasonalTransition() {
        int season = calendar.currentSeason();
        LocalDate date = calendar.dateFor(season, calendar.weeksPerSeason());

        // 1. Season-ending ranking snapshot; report a change at the very top (REQ-241/246).
        RankingSnapshot snapshot = ranking.rankingAsOf(date);
        rankingSnapshots.add(snapshot);
        snapshot.leader().ifPresent(leader -> {
            if (!leader.golferId().equals(previousNumberOne)) {
                media.publish(NewsFactory.worldNumberOne(season, leader.golferId(), nameOf(leader.golferId())));
                previousNumberOne = leader.golferId();
            }
        });

        // 2. Tour promotion/relegation; report promotions (REQ-241).
        for (var movement : tours.reviewSeasonEnd().promotions()) {
            media.publish(NewsFactory.promotion(season, movement.golferId(), nameOf(movement.golferId()),
                    movement.toTier().name()));
        }

        // 3. Advance every active Career one season; collect retirees; evolve survivors' attributes.
        List<String> retirees = new ArrayList<>();
        for (String id : new ArrayList<>(activeGolfers)) {
            Career career = careers.get(id);
            career.advanceSeason(date);
            if (career.isRetired()) {
                retirees.add(id);
                retirementSeason.putIfAbsent(id, season); // for seasons-since-retirement (spec: career-legacy)
                media.publish(NewsFactory.retirement(season, id, nameOf(id), career.statistics().wins()));
            } else {
                evolveGolfer(golfers.get(id), career.age(), season);
            }
        }

        // 3.5. Financial season for every surviving golfer: pay/evaluate sponsors, then sign new offers.
        for (String id : new ArrayList<>(activeGolfers)) {
            if (!careers.get(id).isRetired()) {
                runFinancialSeason(id, season, date);
                runStaffSeason(id, season, date);
                runEquipmentSeason(id, season, date);
            }
        }

        // 3.6. Media: announce rising prospects the first time they crack the top ranks (REQ-248).
        RankingSnapshot standings = ranking.rankingAsOf(date);
        for (String id : activeGolfers) {
            Career career = careers.get(id);
            if (career.isRetired() || announcedProspects.contains(id)) {
                continue;
            }
            int position = standings.positionOf(id).orElse(Integer.MAX_VALUE);
            if (career.age() <= MediaConstants.PROSPECT_MAX_AGE && career.statistics().wins() == 0
                    && position <= MediaConstants.PROSPECT_RANKING) {
                media.publish(NewsFactory.risingProspect(season, id, nameOf(id), position));
                announcedProspects.add(id);
            }
        }

        // 4. Replenish departures so the population stays sufficient.
        for (String id : retirees) {
            activeGolfers.remove(id);
            tours.deregister(id);
            ProfessionalGolfer replacement = PopulationGenerator.generateOne(
                    new SeedCoordinate(masterSeed, 7, 0, 0, 0, 0, 0), config.populationSize() + replenishCounter++);
            admit(replacement, TourTier.DEVELOPMENT);
        }

        // 4.5. Hall of Fame: every election cycle, induct the single highest-scored eligible golfer.
        runHallOfFameElection(season);

        // 5. Announce any player goal reached this season (tour promotion, year-end #1, longevity/HoF).
        checkCareerGoals();

        // 6. Archive the completed season and generate the next season's schedule.
        archives.add(new SeasonArchive(season, schedule, seasonResults));
        seasonResults = new ArrayList<>();
        schedule = generateSchedule(season + 1);
    }

    /**
     * The Hall-of-Fame biennial election (spec: career-legacy). Runs only on an election-cycle season. It
     * gathers every not-yet-inducted golfer — active or retired — that meets the baseline eligibility,
     * ranks them by prestige-weighted score (ties broken by id for determinism), and inducts only the top
     * candidate(s) for the cycle. Non-selected eligibles simply remain candidates next cycle. Pure and
     * deterministic — no randomness — so same-seed worlds induct identically.
     */
    private void runHallOfFameElection(int season) {
        if (season % CareerConstants.HOF_ELECTION_CYCLE_SEASONS != 0) {
            return;
        }
        record Candidate(String id, double score) {
        }
        List<Candidate> ballot = new ArrayList<>();
        for (String id : careers.keySet()) {
            if (hallOfFameMembers.contains(id)) {
                continue;
            }
            HallOfFameCredentials credentials = hallOfFameCredentials(id, season);
            if (HallOfFame.meetsBaseline(credentials)) {
                ballot.add(new Candidate(id, HallOfFame.score(credentials)));
            }
        }
        ballot.sort(Comparator.comparingDouble(Candidate::score).reversed().thenComparing(Candidate::id));
        int inductees = Math.min(CareerConstants.HOF_INDUCTEES_PER_CYCLE, ballot.size());
        for (int i = 0; i < inductees; i++) {
            Candidate c = ballot.get(i);
            hallOfFameMembers.add(c.id());
            hallOfFameInductions.add(new HallOfFameInduction(c.id(), season, c.score()));
            media.publish(NewsFactory.hallOfFameInduction(season, c.id(), nameOf(c.id())));
        }
    }

    /** Builds a golfer's Hall-of-Fame credentials: career stats + age + seasons since retirement (if retired). */
    private HallOfFameCredentials hallOfFameCredentials(String id, int season) {
        Career career = careers.get(id);
        boolean retired = career.isRetired();
        int seasonsSinceRetirement = retired ? season - retirementSeason.getOrDefault(id, season) : 0;
        // Ranking dominance over the season-ending snapshot history feeds the score (spec: career-legacy).
        int careerHighRanking = RankingHistory.careerHighPosition(id, rankingSnapshots)
                .orElse(CareerConstants.HOF_UNRANKED);
        int seasonsAtNumberOne = (int) RankingHistory.weeksAtNumberOne(id, rankingSnapshots);
        return HallOfFameCredentials.of(career.statistics(), career.age(), seasonsSinceRetirement, retired,
                careerHighRanking, seasonsAtNumberOne);
    }

    /**
     * Runs one golfer's end-of-season financial cycle (REQ-178/183/187/188): pay active sponsorships and
     * evaluate their objectives (bonuses + standing), conclude ended agreements, then generate
     * reputation-gated offers and sign the ones a deterministic policy accepts. The economy random stream
     * is derived from an isolated per-golfer/season seed, so the world stays reproducible.
     */
    private void runFinancialSeason(String id, int season, LocalDate date) {
        FinancialAccount account = accounts.get(id);
        PerformanceSnapshot snapshot = buildPerformanceSnapshot(id, date);

        for (SponsorshipAgreement agreement : account.activeAgreements(season)) {
            account.award(TransactionType.SPONSORSHIP_INCOME, agreement.perSeasonPayment(), date,
                    "Sponsor: " + agreement.sponsor());
            var review = agreement.evaluate(snapshot);
            if (review.bonusEarned() > 0) {
                account.award(TransactionType.SPONSORSHIP_BONUS, review.bonusEarned(), date,
                        "Objectives met: " + agreement.sponsor());
            }
            if (review.objectivesTotal() > 0) {
                account.recordObjectiveOutcome(review.metFraction());
            }
        }
        account.concludeExpiredAgreements(season);

        long seed = Seeds.deriveSeed(
                Seeds.deriveSeed(Seeds.deriveSeed(masterSeed, season), EconomyConstants.ECONOMY_SALT),
                id.hashCode());
        Rng rng = new SplitMix64Rng(seed);
        CommercialReputation reputation = CommercialReputation.fromCompetitive(competitiveReputation(id, date), rng);
        List<SponsorshipOffer> offers = sponsorshipMarket.generateOffers(reputation, season + 1, rng);
        if (isPlayer(id)) {
            // The player reviews and accepts offers between advances (spec: player-control); unaccepted
            // offers from a prior season lapse when this season's are generated.
            playerPendingOffers.clear();
            playerPendingOffers.addAll(offers);
        } else {
            for (SponsorshipOffer offer : com.progolf.sim.economy.AcceptancePolicy.choose(account, offers, season + 1)) {
                account.signSponsorship(offer.agreement(), date);
            }
        }
    }

    /**
     * Runs one golfer's end-of-season staff cycle (REQ-195/196/198): pay employed staff salaries through
     * the Economy, release the costliest member while in the red, then hire one new member per the
     * deterministic policy if affordable. Deterministic from an isolated per-golfer/season seed.
     */
    private void runStaffSeason(String id, int season, LocalDate date) {
        SupportTeam team = supportTeams.get(id);
        FinancialAccount account = accounts.get(id);

        // 1. Salaries are a mandatory ongoing commitment (may drive funds negative).
        for (StaffMember member : team.members()) {
            account.charge(TransactionType.STAFF_SALARY, member.seasonalSalary(), date, "Salary: " + member.role());
        }
        // 2. Under financial pressure, release the costliest member (a recorded departure), one per season.
        if (account.availableFunds() < StaffConstants.RELEASE_THRESHOLD && team.size() > 0) {
            team.release(team.mostExpensiveRole(), season);
        }
        // 3. The discretionary hire: the player decides (candidates held as pending offers); the AI auto-hires.
        if (isPlayer(id)) {
            offerPlayerStaff(id, season, team);
            return;
        }
        int age = careers.get(id).age();
        HiringPolicy.chooseRole(team, age).ifPresent(role -> {
            StaffMember candidate = staffMarket.generate(role, staffRng(id, season, role));
            if (HiringPolicy.canAfford(account.availableFunds(), candidate)
                    && account.spend(TransactionType.STAFF_HIRING, candidate.hiringCost(), date, "Hire: " + role)) {
                team.hire(candidate, season);
            }
        });
    }

    /** The isolated per-golfer/season/role RNG for staff-candidate generation (shared by AI and player paths). */
    private Rng staffRng(String id, int season, StaffRole role) {
        return new SplitMix64Rng(Seeds.deriveSeed(Seeds.deriveSeed(Seeds.deriveSeed(
                Seeds.deriveSeed(masterSeed, StaffConstants.STAFF_SALT), season), id.hashCode()), role.ordinal()));
    }

    /** Generates a candidate for each of the player's unfilled roles, held as pending staff offers. */
    private void offerPlayerStaff(String id, int season, SupportTeam team) {
        playerPendingStaff.clear();
        for (StaffRole role : StaffRole.values()) {
            if (!team.has(role)) {
                playerPendingStaff.add(staffMarket.generate(role, staffRng(id, season, role)));
            }
        }
    }

    /**
     * Runs one golfer's end-of-season equipment acquisition (REQ-210): a deterministic policy proposes an
     * upgrade for the weakest category; if affordable, it is purchased through the Economy, added to the
     * inventory, and selected into the loadout. Deterministic from an isolated per-golfer/season seed.
     */
    private void runEquipmentSeason(String id, int season, LocalDate date) {
        // The player decides purchases (upgrades held as pending offers); the AI auto-buys.
        if (isPlayer(id)) {
            offerPlayerEquipment(id, season);
            return;
        }
        EquipmentInventory inventory = equipment.get(id);
        FinancialAccount account = accounts.get(id);
        Rng rng = new SplitMix64Rng(Seeds.deriveSeed(
                Seeds.deriveSeed(Seeds.deriveSeed(masterSeed, EquipmentConstants.EQUIPMENT_SALT), season),
                id.hashCode()));
        AcquisitionPolicy.chooseUpgrade(inventory, rng).ifPresent(item -> {
            if (AcquisitionPolicy.canAfford(account.availableFunds(), item)
                    && account.spend(TransactionType.EQUIPMENT_PURCHASE, item.cost(), date, "Equipment: " + item.category())) {
                inventory.add(item, season, EquipmentAcquisition.Method.PURCHASE);
                loadouts.put(id, loadouts.get(id).with(item));
            }
        });
    }

    /** Generates one upgrade per category, held as pending equipment offers for the player to buy. */
    private void offerPlayerEquipment(String id, int season) {
        playerPendingEquipment.clear();
        for (EquipmentCategory category : EquipmentCategory.values()) {
            Rng rng = new SplitMix64Rng(Seeds.deriveSeed(Seeds.deriveSeed(Seeds.deriveSeed(
                    Seeds.deriveSeed(masterSeed, EquipmentConstants.EQUIPMENT_SALT), season), id.hashCode()),
                    category.ordinal()));
            playerPendingEquipment.add(EquipmentCatalogue.generateUpgrade(category, rng));
        }
    }

    /** Builds a golfer's season performance from this season's results and the current ranking. */
    private PerformanceSnapshot buildPerformanceSnapshot(String id, LocalDate date) {
        int events = 0;
        int wins = 0;
        int madeCuts = 0;
        int best = Integer.MAX_VALUE;
        for (TournamentResult result : seasonResults) {
            for (TournamentResult.Finish finish : result.finishingOrder()) {
                if (finish.golfer().player().id().equals(id) && !finish.withdrawn()) {
                    events++;
                    if (finish.madeCut()) {
                        madeCuts++;
                    }
                    if (finish.position() == 1) {
                        wins++;
                    }
                    best = Math.min(best, finish.position());
                    break;
                }
            }
        }
        int rankingPosition = ranking.rankingAsOf(date).positionOf(id).orElse(Integer.MAX_VALUE);
        int careerWins = careers.get(id).statistics().wins();
        return new PerformanceSnapshot(events, wins, madeCuts, best, rankingPosition, careerWins);
    }

    /**
     * A golfer's [0,1] competitive reputation from ranking position and career wins, nudged by their
     * commercial momentum (past objective outcomes). Fed to the sponsorship market (REQ-183/186).
     */
    private double competitiveReputation(String id, LocalDate date) {
        RankingSnapshot snapshot = ranking.rankingAsOf(date);
        int size = Math.max(1, snapshot.size());
        double positionReputation = snapshot.positionOf(id)
                .map(p -> 1.0 - (p - 1.0) / size)
                .orElse(0.0);
        int wins = careers.get(id).statistics().wins();
        double winReputation = Math.min(1.0, wins * EconomyConstants.WIN_REPUTATION_WEIGHT);
        double base = clamp01(EconomyConstants.POSITION_REPUTATION_WEIGHT * positionReputation
                + EconomyConstants.WINS_REPUTATION_WEIGHT * winReputation);
        return clamp01(base + accounts.get(id).commercialMomentum());
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : Math.min(v, 1.0);
    }

    private List<ScheduledTournament> generateSchedule(int season) {
        List<ScheduledTournament> generated = new ArrayList<>();
        int events = config.eventsPerTierPerSeason();
        int signature = config.signatureEventsPerTier();
        for (TourTier tier : TourTier.values()) {
            for (int e = 0; e < events; e++) {
                int week = 1 + (int) ((long) e * config.weeksPerSeason() / events);
                int courseIndex = (int) (nextTournamentId % coursePool.size());
                // The first events of each tour's season are its elevated signature events (spec: event-prestige).
                EventPrestige prestige = e < signature ? EventPrestige.SIGNATURE : EventPrestige.REGULAR;
                generated.add(new ScheduledTournament(week, tier, courseIndex, prestige, nextTournamentId++));
            }
        }
        // Cross-tour majors: the season's marquee events, spread across the calendar, contested by the
        // strongest field across all tiers (spec: event-prestige).
        int majors = config.majorsPerSeason();
        for (int m = 0; m < majors; m++) {
            int week = 1 + (int) ((long) m * config.weeksPerSeason() / Math.max(1, majors));
            int courseIndex = (int) (nextTournamentId % coursePool.size());
            generated.add(new ScheduledTournament(week, TourTier.ELITE, courseIndex, EventPrestige.MAJOR,
                    nextTournamentId++));
        }
        return generated;
    }

    /** A major draws the strongest active golfers across all tiers, deterministically (spec: event-prestige). */
    private List<ProfessionalGolfer> majorField(LocalDate date, ScheduledTournament event) {
        RankingSnapshot snapshot = ranking.rankingAsOf(date);
        return activeGolfers.stream()
                .filter(id -> physicalStates.get(id).canCompete())
                .filter(id -> !committedThisWeek.contains(id)) // one event per week (spec: event-prestige)
                .filter(id -> !(isPlayer(id) && playerSitsOut(event))) // player skipped/rested (player-control)
                .sorted(Comparator
                        .comparingInt((String id) -> snapshot.positionOf(id).orElse(Integer.MAX_VALUE))
                        .thenComparing(Comparator.comparingDouble((String id) -> meanAttribute(golfers.get(id))).reversed())
                        .thenComparing(id -> id))
                .limit(config.fieldSize())
                .map(golfers::get)
                .toList();
    }

    /** The display name for a scheduled event, distinguishing majors and signature events. */
    private static String eventName(ScheduledTournament event, Tier tier) {
        return switch (event.prestige()) {
            case MAJOR -> "Major Championship #" + event.tournamentId();
            case SIGNATURE -> tier + " Signature #" + event.tournamentId();
            case REGULAR -> tier + " Event #" + event.tournamentId();
        };
    }

    // --- Accessors (read-only) ---

    public int currentSeason() {
        return calendar.currentSeason();
    }

    public int currentWeek() {
        return calendar.currentWeek();
    }

    public int activePopulationSize() {
        return activeGolfers.size();
    }

    /** The ids of currently active (non-retired) golfers, in a stable order. */
    public List<String> activeGolferIds() {
        return List.copyOf(activeGolfers);
    }

    public List<ScheduledTournament> currentSchedule() {
        return List.copyOf(schedule);
    }

    public List<SeasonArchive> archives() {
        return List.copyOf(archives);
    }

    public List<RankingSnapshot> rankingSnapshots() {
        return List.copyOf(rankingSnapshots);
    }

    /** The Hall-of-Fame inductions in order (spec: career-legacy). */
    public List<HallOfFameInduction> hallOfFameInductions() {
        return List.copyOf(hallOfFameInductions);
    }

    /** The ids of all golfers inducted into the Hall of Fame. */
    public Set<String> hallOfFameMembers() {
        return Set.copyOf(hallOfFameMembers);
    }

    /** Whether the golfer has been inducted into the Hall of Fame. */
    public boolean isInHallOfFame(String golferId) {
        return hallOfFameMembers.contains(golferId);
    }

    /** Significant environmental history: events played under severe/record conditions (REQ-235). */
    public List<EnvironmentalRecord> environmentalHistory() {
        return List.copyOf(environmentalHistory);
    }

    /** A golfer's financial identity (funds, earnings, expenses, ledger, sponsorships) (REQ-177). */
    public FinancialAccount financialAccountOf(String golferId) {
        return accounts.get(golferId);
    }

    /** A golfer's current Physical State (fitness, fatigue, injury, availability) (REQ-215). */
    public PhysicalState physicalStateOf(String golferId) {
        return physicalStates.get(golferId);
    }

    /** Significant health events (major injuries and comebacks) across the world (REQ-223). */
    public List<HealthEvent> healthHistory() {
        return List.copyOf(healthHistory);
    }

    /** A golfer's Support Team (current staff plus relationship history) (REQ-191). */
    public SupportTeam supportTeamOf(String golferId) {
        return supportTeams.get(golferId);
    }

    /** A golfer's Equipment Inventory (owned items + ownership history) (REQ-203). */
    public EquipmentInventory equipmentInventoryOf(String golferId) {
        return equipment.get(golferId);
    }

    /** A golfer's current Tournament Loadout (REQ-205). */
    public TournamentLoadout tournamentLoadoutOf(String golferId) {
        return loadouts.get(golferId);
    }

    /** The World Narrative: the full media news feed in publication order (REQ-245). */
    public List<NewsEvent> newsFeed() {
        return media.feed();
    }

    /** Historically significant news, which remains discoverable (REQ-246). */
    public List<NewsEvent> significantNews() {
        return media.significantNews();
    }

    /** All news about a particular golfer. */
    public List<NewsEvent> newsForGolfer(String golferId) {
        return media.newsForGolfer(golferId);
    }

    /** A golfer's complete career statistics from the archive, permanent after retirement (REQ-253). */
    public StatLine careerStatisticsOf(String golferId) {
        return statistics.careerStatistics(golferId);
    }

    /** A golfer's preserved statistics for a specific season (REQ-252). */
    public StatLine seasonStatisticsOf(String golferId, int season) {
        return statistics.seasonStatistics(golferId, season);
    }

    /** The champions of a given season from the historical archive (REQ-256/258). */
    public List<Championship> championsOfSeason(int season) {
        return statistics.championsOfSeason(season);
    }

    /** The current holders of every world record (REQ-254). */
    public Map<RecordType, RecordHolder> records() {
        return statistics.records();
    }

    /** The full progression of a record, each successive holder preserved (REQ-255). */
    public List<RecordHolder> recordProgression(RecordType type) {
        return statistics.recordProgression(type);
    }

    /** A read-only comparison of two golfers' careers (REQ-259). */
    public CareerComparison compareCareers(String golferA, String golferB) {
        return statistics.compareCareers(golferA, golferB);
    }

    // --- Player control (spec: player-control) ---

    /** Designates a golfer as human-controlled; the world otherwise runs autonomously. */
    public void assignPlayer(String golferId) {
        if (playerControl != null) {
            throw new IllegalStateException("A player has already been assigned to this world");
        }
        if (!golfers.containsKey(golferId)) {
            throw new IllegalArgumentException("No such golfer: " + golferId);
        }
        this.playerControl = new PlayerControl(golferId);
        this.playerPendingOffers.clear();
        this.playerPendingStaff.clear();
        this.playerPendingEquipment.clear();
        this.achievedGoals.clear();
    }

    /**
     * Creates a custom golfer for the player (spec: golfer-creation): a chosen identity and playing-style
     * archetype build. The created golfer is human-controlled, enters at the entry (Development) tier to
     * climb from the bottom, and becomes the world's single designated player. Returns its id.
     */
    public String createPlayer(String firstName, String lastName, Nationality nationality, int startAge,
                               Archetype archetype) {
        if (playerControl != null) {
            throw new IllegalStateException("A player has already been assigned to this world");
        }
        String id = "player-" + Long.toUnsignedString(masterSeed, 16);
        ProfessionalGolfer golfer = GolferFactory.createHuman(id, firstName, lastName, nationality, startAge,
                archetype, WorldConstants.BASE_YEAR);
        admit(golfer, TourTier.DEVELOPMENT);
        assignPlayer(id);
        return id;
    }

    /** The designated player-controlled golfer, if any. */
    public java.util.Optional<String> playerGolferId() {
        return playerControl == null ? java.util.Optional.empty() : java.util.Optional.of(playerControl.golferId());
    }

    /** Sets the player's development focus (attribute priority) for their golfer. */
    public void setDevelopmentFocus(List<Attribute> focus) {
        requirePlayer().setDevelopmentFocus(focus);
    }

    /** Sets whether the player's golfer is resting (a blanket sit-out of all events to recover). */
    public void setResting(boolean resting) {
        requirePlayer().setResting(resting);
    }

    /** Skips a specific upcoming event by tournament id (the player is entered in eligible events by default). */
    public void skipEvent(long tournamentId) {
        requirePlayer().skipEvent(tournamentId);
    }

    /** Re-enters a previously skipped event, restoring default entry. */
    public void enterEvent(long tournamentId) {
        requirePlayer().enterEvent(tournamentId);
    }

    // --- Career goals (spec: career-goals) ---

    /** Sets the player's self-chosen career goals (their own framing; never gates play). */
    public void setCareerGoals(List<CareerGoal> goals) {
        requirePlayer().setCareerGoals(goals);
    }

    /** The player's career goals with live progress toward each (current value, target, achieved). */
    public List<CareerGoalProgress> careerGoals() {
        String id = requirePlayer().golferId();
        List<CareerGoalProgress> out = new ArrayList<>();
        for (CareerGoal goal : playerControl.careerGoals()) {
            out.add(evaluateGoal(id, goal));
        }
        return out;
    }

    /** Evaluates one goal against the golfer's live career state (purely observational). */
    private CareerGoalProgress evaluateGoal(String playerId, CareerGoal goal) {
        CareerStatistics stats = careers.get(playerId).statistics();
        long current;
        long target = goal.target();
        switch (goal.type()) {
            case REACH_TOP_TOUR -> {
                current = tours.membershipOf(playerId).map(t -> t == TourTier.ELITE ? 1L : 0L).orElse(0L);
                target = 1;
            }
            case WIN_A_MAJOR -> current = stats.majorsWon();
            case WORLD_NUMBER_ONE -> {
                current = ranking.rankingAsOf(calendar.currentDate()).positionOf(playerId)
                        .map(p -> p == 1 ? 1L : 0L).orElse(0L);
                target = 1;
            }
            case CAREER_WINS -> current = stats.wins();
            case CAREER_EARNINGS -> current = (long) stats.totalEarnings();
            case HALL_OF_FAME -> {
                // Achieved only on actual induction (the marquee lifetime achievement), not mere eligibility.
                current = hallOfFameMembers.contains(playerId) ? 1L : 0L;
                target = 1;
            }
            default -> throw new IllegalStateException("unhandled goal type " + goal.type());
        }
        return new CareerGoalProgress(goal, current, target, current >= target);
    }

    /** Announces any of the player's goals that have newly become achieved (once each) into the narrative. */
    private void checkCareerGoals() {
        if (playerControl == null || playerControl.careerGoals().isEmpty()) {
            return;
        }
        String id = playerControl.golferId();
        int season = calendar.currentSeason();
        for (CareerGoal goal : playerControl.careerGoals()) {
            if (achievedGoals.contains(goal)) {
                continue;
            }
            if (evaluateGoal(id, goal).achieved()) {
                achievedGoals.add(goal);
                media.publish(NewsFactory.goalAchieved(season, id, nameOf(id), describeGoal(goal)));
            }
        }
    }

    /** A human phrase for a goal, for the achievement headline. */
    private static String describeGoal(CareerGoal goal) {
        return switch (goal.type()) {
            case REACH_TOP_TOUR -> "reached the Elite tour";
            case WIN_A_MAJOR -> goal.target() > 1 ? "won " + goal.target() + " majors" : "won a major";
            case WORLD_NUMBER_ONE -> "became world number one";
            case CAREER_WINS -> "won " + goal.target() + " tournaments";
            case CAREER_EARNINGS -> "surpassed " + goal.target() + " in career earnings";
            case HALL_OF_FAME -> "reached Hall-of-Fame standard";
        };
    }

    /**
     * The player's reviewable schedule (spec: player-control): every upcoming event they are eligible for —
     * their tour's events plus all majors (which are cross-tour) — with its prestige and whether the player
     * is currently entered (not resting and not skipped). The reward side of the fatigue/travel trade-off is
     * the prestige; the cost side is read from the player's physical state and the standard travel/entry costs.
     */
    public List<PlayerScheduleEntry> playerSchedule() {
        String playerId = requirePlayer().golferId();
        var tier = tours.membershipOf(playerId);
        List<PlayerScheduleEntry> out = new ArrayList<>();
        for (ScheduledTournament event : schedule) {
            boolean eligibleByTour = event.prestige().isMajor()
                    || tier.map(t -> t == event.tier()).orElse(false);
            if (eligibleByTour) {
                out.add(new PlayerScheduleEntry(event.tournamentId(), event.week(), event.tier(),
                        event.prestige(), !playerSitsOut(event)));
            }
        }
        return out;
    }

    /** The player's golfer's sponsorship offers awaiting an accept/decline decision. */
    public List<SponsorshipOffer> pendingSponsorships() {
        return List.copyOf(playerPendingOffers);
    }

    /** Accepts a pending sponsorship offer by index, signing it within the concurrent-agreement limit. */
    public void acceptSponsorship(int index) {
        requirePlayer();
        if (index < 0 || index >= playerPendingOffers.size()) {
            throw new IndexOutOfBoundsException("No pending offer at index " + index);
        }
        SponsorshipOffer offer = playerPendingOffers.get(index);
        FinancialAccount account = accounts.get(playerControl.golferId());
        int startSeason = offer.agreement().startSeason();
        if (account.activeAgreementCount(startSeason) < EconomyConstants.MAX_CONCURRENT_AGREEMENTS) {
            account.signSponsorship(offer.agreement(), calendar.currentDate());
            playerPendingOffers.remove(index);
        }
    }

    // --- Staff (spec: player-control) ---

    /** The player's golfer's staff candidates awaiting a hire decision. */
    public List<StaffMember> pendingStaffOffers() {
        return List.copyOf(playerPendingStaff);
    }

    /** Hires a pending staff candidate by index, if the player can afford it (charged to their account). */
    public void hireStaff(int index) {
        requirePlayer();
        if (index < 0 || index >= playerPendingStaff.size()) {
            throw new IndexOutOfBoundsException("No pending staff candidate at index " + index);
        }
        StaffMember candidate = playerPendingStaff.get(index);
        String id = playerControl.golferId();
        FinancialAccount account = accounts.get(id);
        if (HiringPolicy.canAfford(account.availableFunds(), candidate)
                && account.spend(TransactionType.STAFF_HIRING, candidate.hiringCost(),
                        calendar.currentDate(), "Hire: " + candidate.role())) {
            supportTeams.get(id).hire(candidate, calendar.currentSeason());
            playerPendingStaff.remove(index);
        }
    }

    /** Releases a current member of the player's support team by role (a recorded, voluntary departure). */
    public void releaseStaff(StaffRole role) {
        requirePlayer();
        SupportTeam team = supportTeams.get(playerControl.golferId());
        if (team.has(role)) {
            team.release(role, calendar.currentSeason());
        }
    }

    // --- Equipment (spec: player-control) ---

    /** The player's golfer's equipment upgrade offers awaiting a purchase decision. */
    public List<EquipmentItem> pendingEquipmentOffers() {
        return List.copyOf(playerPendingEquipment);
    }

    /** Buys a pending equipment upgrade by index, if affordable; adds it and selects it into the loadout. */
    public void buyEquipment(int index) {
        requirePlayer();
        if (index < 0 || index >= playerPendingEquipment.size()) {
            throw new IndexOutOfBoundsException("No pending equipment upgrade at index " + index);
        }
        EquipmentItem item = playerPendingEquipment.get(index);
        String id = playerControl.golferId();
        FinancialAccount account = accounts.get(id);
        if (AcquisitionPolicy.canAfford(account.availableFunds(), item)
                && account.spend(TransactionType.EQUIPMENT_PURCHASE, item.cost(),
                        calendar.currentDate(), "Equipment: " + item.category())) {
            equipment.get(id).add(item, calendar.currentSeason(), EquipmentAcquisition.Method.PURCHASE);
            loadouts.put(id, loadouts.get(id).with(item));
            playerPendingEquipment.remove(index);
        }
    }

    /** Sets the player's tournament loadout for a category to one of their owned items. */
    public void selectLoadoutItem(EquipmentItem item) {
        requirePlayer();
        String id = playerControl.golferId();
        if (!equipment.get(id).owns(item)) {
            throw new IllegalArgumentException("The player does not own " + item.category() + " item " + item.name());
        }
        loadouts.put(id, loadouts.get(id).with(item));
    }

    private PlayerControl requirePlayer() {
        if (playerControl == null) {
            throw new IllegalStateException("No player has been assigned to this world");
        }
        return playerControl;
    }

    /** Whether the given golfer is the world's designated player. */
    private boolean isPlayer(String golferId) {
        return playerControl != null && playerControl.golferId().equals(golferId);
    }

    // --- Playable event (spec: playable-event): the player plays their own tournament ---

    /**
     * Whether the designated player would be entered in an event: assigned, active, not resting, able to
     * compete, and a member of the event's tour. Whether they actually make the field (within its size) is
     * confirmed when the event is built.
     */
    private boolean isPlayerEntered(ScheduledTournament event) {
        return isPlayerEligible(event) && !playerSitsOut(event);
    }

    /** Whether the player is eligible for an event (the hard gates they cannot override). */
    private boolean isPlayerEligible(ScheduledTournament event) {
        if (playerControl == null) {
            return false;
        }
        String id = playerControl.golferId();
        if (!activeGolfers.contains(id) || !physicalStates.get(id).canCompete()) {
            return false;
        }
        if (event.prestige().isMajor()) {
            return true; // majors are cross-tour; buildEvent's field/playerFieldIndex decides if they qualify
        }
        return tours.membershipOf(id).map(t -> t == event.tier()).orElse(false);
    }

    /** Whether the player has chosen to sit an event out — a blanket rest or a per-event skip (spec: player-control). */
    private boolean playerSitsOut(ScheduledTournament event) {
        return playerControl != null
                && (playerControl.isResting() || playerControl.isSkipped(event.tournamentId()));
    }

    /** Whether the world is paused awaiting the player to complete their interactive event. */
    public boolean hasPendingPlayerEvent() {
        return pendingEvent != null;
    }

    /** The player's pending interactive event handle (play or sim it, then {@link #completePlayerEvent}). */
    public PlayableEvent playerEvent() {
        if (pendingEvent == null) {
            throw new IllegalStateException("No player event is pending");
        }
        return pendingEvent.event();
    }

    /** The event built for the player, plus the paused week's position, held while the event is in progress. */
    private record PendingPlayerEvent(PlayableEvent event, BuiltEvent built,
                                      List<ScheduledTournament> weekEvents, int cursor) {
    }

    /** An event built up to a confirmed Tournament, carrying everything {@link #feedConsumers} needs. */
    private record BuiltEvent(ScheduledTournament event, Tournament tournament, List<ProfessionalGolfer> field,
                              TournamentDefinition def, Tier tier, LocalDate date, TournamentWeather weather,
                              Course course, int season, int playerFieldIndex) {
    }

    /** A golfer's current descriptive Career Narrative (REQ-244). */
    public CareerNarrative careerNarrativeOf(String golferId) {
        Career career = careers.get(golferId);
        int position = ranking.rankingAsOf(calendar.currentDate()).positionOf(golferId).orElse(Integer.MAX_VALUE);
        return media.careerNarrative(golferId, career.age(), career.seasons().size(),
                career.statistics().wins(), position, calendar.currentSeason());
    }

    /** A golfer's full display name, for news headlines. */
    private String nameOf(String golferId) {
        return golfers.get(golferId).player().identity().fullName();
    }

    /** The current World Ranking as of the calendar date. */
    public RankingSnapshot currentRanking() {
        return ranking.rankingAsOf(calendar.currentDate());
    }

    /** A golfer's current tour tier, if they are an active member. */
    public java.util.Optional<TourTier> tourOf(String golferId) {
        return tours.membershipOf(golferId);
    }

    /** A golfer's career, if present (retired golfers' careers remain retrievable). */
    public Career careerOf(String golferId) {
        return careers.get(golferId);
    }

    // --- Helpers ---

    /** Applies one season of development then aging to a golfer's attributes (REQ-152/159). */
    private void evolveGolfer(ProfessionalGolfer golfer, int age, int season) {
        // A coach enhances development by scaling the season's Development Points (REQ-197).
        double developmentFactor = 1.0 + supportTeams.get(golfer.player().id()).effects().developmentBonus();
        // The player directs their golfer's development to a chosen focus (spec: player-control); everyone
        // else (and every unassigned world) uses the automatic allocation.
        Attributes developed = isPlayer(golfer.player().id()) && !playerControl.developmentFocus().isEmpty()
                ? ProgressionEngine.develop(golfer.player().attributes(), age, developmentFactor,
                        playerControl.developmentFocus())
                : ProgressionEngine.develop(golfer.player().attributes(), age, developmentFactor);
        golfer.player().evolveAttributes(developed, AttributeChange.Reason.DEVELOPMENT, season);
        Attributes aged = ProgressionEngine.age(golfer.player().attributes(), age);
        golfer.player().evolveAttributes(aged, AttributeChange.Reason.AGING, season);
    }

    private static Tier mapTier(TourTier tier) {
        return switch (tier) {
            case DEVELOPMENT -> Tier.DEVELOPMENT;
            case SECONDARY -> Tier.STANDARD;
            case PRIMARY -> Tier.PREMIER;
            case ELITE -> Tier.ELITE;
        };
    }

    private static int startAgeOf(ProfessionalGolfer golfer) {
        return WorldConstants.BASE_YEAR - golfer.player().identity().dateOfBirth().getYear();
    }

    private static double meanAttribute(ProfessionalGolfer golfer) {
        double sum = 0;
        for (Attribute a : Attribute.values()) {
            sum += golfer.player().attributes().get(a);
        }
        return sum / Attribute.values().length;
    }
}
