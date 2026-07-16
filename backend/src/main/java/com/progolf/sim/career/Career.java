package com.progolf.sim.career;

import com.progolf.sim.player.CareerStatus;
import com.progolf.sim.player.Player;
import com.progolf.sim.tournament.EventPrestige;
import com.progolf.sim.tournament.Tier;
import com.progolf.sim.tournament.TournamentResult;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The Career entity (REQ-025–037): the complete playable history of one golfer, owned by a {@link Player}.
 * It tracks the golfer's competitive Age (advancing once per season, never decreasing), enforces
 * mandatory retirement at 65, folds tournament results into cumulative statistics / milestones / a
 * chronological history, and evaluates Hall-of-Fame eligibility on retirement.
 *
 * <p>Analytical and append-only: it never mutates player attributes or tournament results, and its
 * historical records are only appended, so a completed career is reconstructible from them.
 */
public final class Career {

    private final Player player;
    private final int startAge;
    private int age;

    private final CareerStatistics statistics = new CareerStatistics();
    private final EnumSet<CareerMilestone> milestones = EnumSet.noneOf(CareerMilestone.class);
    private final List<CareerHistoryEntry> history = new ArrayList<>();
    private final List<SeasonRecord> seasons = new ArrayList<>();

    private CareerRuntimeState runtimeState = CareerRuntimeState.ACTIVE;
    private HallOfFameResult hallOfFameResult;

    /**
     * Starts a Career for an activated Player at a starting age (REQ-025/026/028). The Player must be
     * ACTIVE; the starting age must be within the permitted range.
     */
    public Career(Player player, int startAge) {
        this.player = Objects.requireNonNull(player, "player");
        if (player.status() != CareerStatus.ACTIVE) {
            throw new IllegalStateException("A Career begins only for an ACTIVE player; status=" + player.status());
        }
        if (startAge < CareerConstants.MIN_START_AGE || startAge > CareerConstants.MAX_START_AGE) {
            throw new IllegalArgumentException("Starting age must be "
                    + CareerConstants.MIN_START_AGE + "-" + CareerConstants.MAX_START_AGE + ": " + startAge);
        }
        this.startAge = startAge;
        this.age = startAge;
    }

    /** Private no-validation constructor used by {@link #restore} (a retired/aged career is reconstructed as-is). */
    private Career(Player player, int startAge, int age) {
        this.player = player;
        this.startAge = startAge;
        this.age = age;
    }

    /** An immutable capture of a Career (spec: world-snapshot). The owning player is captured separately. */
    public record Snapshot(int startAge, int age, CareerStatistics.Snapshot statistics,
                           java.util.Set<CareerMilestone> milestones, List<CareerHistoryEntry> history,
                           List<SeasonRecord> seasons, CareerRuntimeState runtimeState,
                           HallOfFameResult hallOfFameResult) {
        public Snapshot {
            milestones = java.util.Set.copyOf(milestones);
            history = List.copyOf(history);
            seasons = List.copyOf(seasons);
        }
    }

    /** Captures this Career (its owning Player is captured by the golfer registry, not here). */
    public Snapshot snapshot() {
        return new Snapshot(startAge, age, statistics.snapshot(), new java.util.HashSet<>(milestones),
                new ArrayList<>(history), new ArrayList<>(seasons), runtimeState, hallOfFameResult);
    }

    /** Rebuilds a Career bound to an already-restored {@link Player}. */
    public static Career restore(Player player, Snapshot s) {
        Career c = new Career(player, s.startAge(), s.age());
        c.statistics.restoreFrom(s.statistics());
        c.milestones.addAll(s.milestones());
        c.history.addAll(s.history());
        c.seasons.addAll(s.seasons());
        c.runtimeState = s.runtimeState();
        c.hallOfFameResult = s.hallOfFameResult();
        return c;
    }

    /** The golfer this Career belongs to (never reassigned). */
    public Player player() {
        return player;
    }

    public int startAge() {
        return startAge;
    }

    /** Current competitive age in whole years. */
    public int age() {
        return age;
    }

    public boolean isRetired() {
        return player.status() == CareerStatus.RETIRED;
    }

    public CareerStatistics statistics() {
        return statistics;
    }

    public boolean hasMilestone(CareerMilestone milestone) {
        return milestones.contains(milestone);
    }

    /** The chronological (date-ordered) immutable history view. */
    public List<CareerHistoryEntry> history() {
        List<CareerHistoryEntry> ordered = new ArrayList<>(history);
        ordered.sort(Comparator.comparing(CareerHistoryEntry::date));
        return List.copyOf(ordered);
    }

    /** Archived season boundaries, in order. */
    public List<SeasonRecord> seasons() {
        return List.copyOf(seasons);
    }

    public CareerRuntimeState runtimeState() {
        return runtimeState;
    }

    /** Sets the runtime (execution) state; this never advances or alters gameplay progression. */
    public void setRuntimeState(CareerRuntimeState state) {
        this.runtimeState = Objects.requireNonNull(state, "state");
    }

    /** The Hall-of-Fame evaluation recorded at retirement, if the career has retired. */
    public Optional<HallOfFameResult> hallOfFameResult() {
        return Optional.ofNullable(hallOfFameResult);
    }

    /**
     * Advances one completed season (REQ-029): archives the season and increases Age by one. If this
     * reaches the mandatory retirement age, the golfer retires automatically (REQ-028) and cannot be
     * advanced further.
     */
    public void advanceSeason(LocalDate seasonEndDate) {
        Objects.requireNonNull(seasonEndDate, "seasonEndDate");
        if (isRetired()) {
            throw new IllegalStateException("A retired career cannot advance further");
        }
        age += 1; // Age only ever advances (no setter, never decreases)
        seasons.add(new SeasonRecord(seasons.size() + 1, age));
        if (age >= CareerConstants.RETIREMENT_AGE) {
            retire(seasonEndDate);
        }
    }

    /**
     * Records the golfer's participation in a completed tournament (REQ-031/032/033): folds statistics,
     * fires any first-occurrence milestones, and appends history. Rejected once the career is retired.
     */
    public void recordTournament(TournamentResult result, LocalDate date) {
        recordTournament(result, EventPrestige.REGULAR, Tier.STANDARD, date);
    }

    /** Records the golfer's participation, weighting a win by prestige at the default (standard) tour tier. */
    public void recordTournament(TournamentResult result, EventPrestige prestige, LocalDate date) {
        recordTournament(result, prestige, Tier.STANDARD, date);
    }

    /**
     * Records the golfer's participation, classifying a win by the event's prestige AND tour tier
     * (spec: event-prestige, career-legacy): a win is folded into the career's major / signature /
     * development-tier legacy buckets (regular pro wins are the remainder), which drive Hall-of-Fame
     * credentials. Otherwise identical to the regular record.
     */
    public void recordTournament(TournamentResult result, EventPrestige prestige, Tier tier, LocalDate date) {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(prestige, "prestige");
        Objects.requireNonNull(tier, "tier");
        Objects.requireNonNull(date, "date");
        if (isRetired()) {
            throw new IllegalStateException("A retired career is read-only");
        }
        TournamentResult.Finish finish = result.finishingOrder().stream()
                .filter(f -> f.golfer().player().id().equals(player.id()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Golfer did not play in tournament " + result.tournamentName()));

        statistics.recordResult(finish.position(), finish.madeCut(), finish.withdrawn(), finish.prize(),
                classifyWin(finish, prestige, tier));
        history.add(new CareerHistoryEntry(date, CareerHistoryEntry.Type.TOURNAMENT,
                result.tournamentName() + " — position " + finish.position()));

        fireMilestone(CareerMilestone.FIRST_EVENT, date);
        if (!finish.withdrawn()) {
            if (finish.madeCut()) {
                fireMilestone(CareerMilestone.FIRST_MADE_CUT, date);
            }
            if (finish.position() <= CareerConstants.TOP_10) {
                fireMilestone(CareerMilestone.FIRST_TOP_10, date);
            }
            if (finish.position() == 1) {
                fireMilestone(CareerMilestone.FIRST_WIN, date);
            }
        }
    }

    /**
     * Classifies a finish into a legacy win bucket by priority — major, else signature, else
     * development-tier (amateur), else regular professional — or NONE for a non-win (spec: career-legacy).
     */
    private static WinCategory classifyWin(TournamentResult.Finish finish, EventPrestige prestige, Tier tier) {
        if (finish.withdrawn() || finish.position() != 1) {
            return WinCategory.NONE;
        }
        if (prestige.isMajor()) {
            return WinCategory.MAJOR;
        }
        // A Tour Championship is an elevated, non-major win — categorised with Signatures for career stats.
        if (prestige == EventPrestige.SIGNATURE || prestige == EventPrestige.TOUR_CHAMPIONSHIP) {
            return WinCategory.SIGNATURE;
        }
        if (tier == Tier.DEVELOPMENT) {
            return WinCategory.DEVELOPMENT;
        }
        return WinCategory.REGULAR;
    }

    /** Records a milestone the first time it occurs (REQ-031 duplicate prevention). */
    private void fireMilestone(CareerMilestone milestone, LocalDate date) {
        if (milestones.add(milestone)) {
            history.add(new CareerHistoryEntry(date, CareerHistoryEntry.Type.MILESTONE, milestone.name()));
        }
    }

    /** Retires the golfer: coordinates the Player status, records history, and evaluates the Hall of Fame. */
    private void retire(LocalDate date) {
        // An ACTIVE golfer may transition to RETIRED (the career is guarded against being already
        // retired before this is called).
        player.transitionTo(CareerStatus.RETIRED);
        history.add(new CareerHistoryEntry(date, CareerHistoryEntry.Type.RETIREMENT, "Retired at age " + age));
        // Record baseline (ballot) eligibility; actual induction happens via the World's biennial election.
        hallOfFameResult = HallOfFame.baselineResult(
                HallOfFameCredentials.of(statistics, age, 0, true));
    }
}
