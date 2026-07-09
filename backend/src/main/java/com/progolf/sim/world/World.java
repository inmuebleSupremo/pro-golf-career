package com.progolf.sim.world;

import com.progolf.sim.career.Career;
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
import com.progolf.sim.population.PopulationGenerator;
import com.progolf.sim.progression.ProgressionEngine;
import com.progolf.sim.ranking.RankingSnapshot;
import com.progolf.sim.ranking.WorldRanking;
import com.progolf.sim.tour.TourSystem;
import com.progolf.sim.tour.TourTier;
import com.progolf.sim.tournament.EntryRequirements;
import com.progolf.sim.tournament.PrizeStructure;
import com.progolf.sim.tournament.Tier;
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
    private final TourSystem tours = new TourSystem();
    private final WorldRanking ranking = new WorldRanking();
    private final Map<String, ProfessionalGolfer> golfers = new LinkedHashMap<>();
    private final Map<String, Career> careers = new LinkedHashMap<>();
    private final Set<String> activeGolfers = new LinkedHashSet<>();

    private final List<SeasonArchive> archives = new ArrayList<>();
    private final List<RankingSnapshot> rankingSnapshots = new ArrayList<>();

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
        int week = calendar.currentWeek();
        for (ScheduledTournament event : schedule) {
            if (event.week() == week) {
                resolveEvent(event);
            }
        }
        recoverHealth(); // fatigue recovers and rehabilitation advances each week (REQ-218/220)
        if (calendar.isSeasonEnd()) {
            seasonalTransition();
        }
        calendar.advance();
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
                healthHistory.add(new HealthEvent(season, id, HealthEvent.Type.COMEBACK,
                        "Returned from " + before.injury().get().severity() + " " + before.injury().get().type()));
            }
        }
    }

    /** Advances a whole season (its remaining weeks). */
    public void advanceSeason() {
        int startSeason = calendar.currentSeason();
        while (calendar.currentSeason() == startSeason) {
            advanceWeek();
        }
    }

    private void resolveEvent(ScheduledTournament event) {
        List<ProfessionalGolfer> field = tours.standings(event.tier()).stream()
                .filter(activeGolfers::contains)
                .filter(id -> physicalStates.get(id).canCompete()) // availability gates entry (REQ-221)
                .limit(config.fieldSize())
                .map(golfers::get)
                .toList();
        if (field.isEmpty()) {
            return; // no eligible field this week
        }

        Course course = coursePool.get(event.courseIndex());
        Tier tier = mapTier(event.tier());
        LocalDate date = calendar.dateFor(calendar.currentSeason(), event.week());

        TournamentDefinition def = new TournamentDefinition(
                tier + " Event " + event.tournamentId(), course, tier,
                new EntryRequirements(config.fieldSize(), true),
                PrizeStructure.standard(), TournamentFormat.standard(), date,
                masterSeed, calendar.currentSeason(), event.tournamentId());

        // Weather is generated before play from the course's climate and the point in the season; the
        // whole field plays under the same per-round conditions (REQ-228/231/232).
        int weeksPerSeason = calendar.weeksPerSeason();
        double seasonPhase = weeksPerSeason > 1 ? (double) (event.week() - 1) / (weeksPerSeason - 1) : 0.0;
        TournamentWeather weather = weatherSystem.generate(
                calendar.currentSeason(), event.tournamentId(), seasonPhase,
                course.identity().classification(), def.format().rounds());

        // Before play, sync each competitor's temporary state into the shot engine: accumulated fatigue
        // (REQ-225) and the active Golf Bag's characteristics (REQ-206), both read via toGolferState.
        for (ProfessionalGolfer g : field) {
            String id = g.player().id();
            g.player().state().setFatigue(physicalStates.get(id).fatigue());
            GolfBag bag = GolfBag.fromLoadout(loadouts.get(id));
            g.player().state().setEquipment(bag.forgivenessBonus(), bag.powerBonus());
        }

        Tournament tournament = new Tournament(def, weather);
        tournament.openRegistration();
        for (ProfessionalGolfer g : field) {
            tournament.register(g);
        }
        tournament.confirmField();
        TournamentResult result = tournament.playToCompletion();

        // Preserve historically significant environmental context (REQ-235).
        if (weather.severity() >= WeatherConstants.SEVERITY_THRESHOLD) {
            environmentalHistory.add(new EnvironmentalRecord(
                    calendar.currentSeason(), event.tournamentId(), weather.severity(),
                    tier + " Event " + event.tournamentId() + " played in severe conditions"));
        }

        // Feed the one result to every consumer (each owns its own computation).
        ranking.record(result, tier, date, event.tournamentId());
        tours.recordResult(result, event.tier());
        for (ProfessionalGolfer g : field) {
            careers.get(g.player().id()).recordTournament(result, date);
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
        int season = calendar.currentSeason();
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
                healthHistory.add(new HealthEvent(season, id, HealthEvent.Type.INJURY,
                        after.injury().get().severity() + " " + after.injury().get().type() + " injury"));
            }
        }

        seasonResults.add(result);
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

        // 1. Season-ending ranking snapshot.
        rankingSnapshots.add(ranking.rankingAsOf(date));

        // 2. Tour promotion/relegation (golfers who competed this season are still members).
        tours.reviewSeasonEnd();

        // 3. Advance every active Career one season; collect retirees; evolve survivors' attributes.
        List<String> retirees = new ArrayList<>();
        for (String id : new ArrayList<>(activeGolfers)) {
            Career career = careers.get(id);
            career.advanceSeason(date);
            if (career.isRetired()) {
                retirees.add(id);
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

        // 4. Replenish departures so the population stays sufficient.
        for (String id : retirees) {
            activeGolfers.remove(id);
            tours.deregister(id);
            ProfessionalGolfer replacement = PopulationGenerator.generateOne(
                    new SeedCoordinate(masterSeed, 7, 0, 0, 0, 0, 0), config.populationSize() + replenishCounter++);
            admit(replacement, TourTier.DEVELOPMENT);
        }

        // 5. Archive the completed season and generate the next season's schedule.
        archives.add(new SeasonArchive(season, schedule, seasonResults));
        seasonResults = new ArrayList<>();
        schedule = generateSchedule(season + 1);
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
        for (SponsorshipOffer offer : com.progolf.sim.economy.AcceptancePolicy.choose(account, offers, season + 1)) {
            account.signSponsorship(offer.agreement(), date);
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
        // 3. Hire one new member per the deterministic policy if it is affordable (discretionary spend).
        int age = careers.get(id).age();
        HiringPolicy.chooseRole(team, age).ifPresent(role -> {
            Rng rng = new SplitMix64Rng(Seeds.deriveSeed(Seeds.deriveSeed(Seeds.deriveSeed(
                    Seeds.deriveSeed(masterSeed, StaffConstants.STAFF_SALT), season), id.hashCode()), role.ordinal()));
            StaffMember candidate = staffMarket.generate(role, rng);
            if (HiringPolicy.canAfford(account.availableFunds(), candidate)
                    && account.spend(TransactionType.STAFF_HIRING, candidate.hiringCost(), date, "Hire: " + role)) {
                team.hire(candidate, season);
            }
        });
    }

    /**
     * Runs one golfer's end-of-season equipment acquisition (REQ-210): a deterministic policy proposes an
     * upgrade for the weakest category; if affordable, it is purchased through the Economy, added to the
     * inventory, and selected into the loadout. Deterministic from an isolated per-golfer/season seed.
     */
    private void runEquipmentSeason(String id, int season, LocalDate date) {
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
        for (TourTier tier : TourTier.values()) {
            for (int e = 0; e < events; e++) {
                int week = 1 + (int) ((long) e * config.weeksPerSeason() / events);
                int courseIndex = (int) (nextTournamentId % coursePool.size());
                generated.add(new ScheduledTournament(week, tier, courseIndex, nextTournamentId++));
            }
        }
        return generated;
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
        Attributes developed = ProgressionEngine.develop(golfer.player().attributes(), age, developmentFactor);
        golfer.player().evolveAttributes(developed, AttributeChange.Reason.DEVELOPMENT, season);
        Attributes aged = ProgressionEngine.age(golfer.player().attributes(), age);
        golfer.player().evolveAttributes(aged, AttributeChange.Reason.AGING, season);
    }

    private static Tier mapTier(TourTier tier) {
        return switch (tier) {
            case DEVELOPMENT -> Tier.DEVELOPMENT;
            case SECONDARY -> Tier.STANDARD;
            case PRIMARY -> Tier.PREMIER;
            case ELITE -> Tier.MAJOR;
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
