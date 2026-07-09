package com.progolf.sim.world;

import com.progolf.sim.career.Career;
import com.progolf.sim.core.Attribute;
import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.player.AttributeChange;
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
        if (calendar.isSeasonEnd()) {
            seasonalTransition();
        }
        calendar.advance();
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

        Tournament tournament = new Tournament(def);
        tournament.openRegistration();
        for (ProfessionalGolfer g : field) {
            tournament.register(g);
        }
        tournament.confirmField();
        TournamentResult result = tournament.playToCompletion();

        // Feed the one result to every consumer (each owns its own computation).
        ranking.record(result, tier, date, event.tournamentId());
        tours.recordResult(result, event.tier());
        for (ProfessionalGolfer g : field) {
            careers.get(g.player().id()).recordTournament(result, date);
        }
        seasonResults.add(result);
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
        Attributes developed = ProgressionEngine.develop(golfer.player().attributes(), age);
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
