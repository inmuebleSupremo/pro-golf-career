package com.progolf.sim.tournament;

import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.course.CourseSetup;
import com.progolf.sim.player.DecisionPolicy;
import com.progolf.sim.player.ProfessionalGolfer;
import com.progolf.sim.shot.HoleModel;
import com.progolf.sim.shot.HoleStats;
import com.progolf.sim.shot.RoundOutcome;
import com.progolf.sim.shot.RoundResolver;
import com.progolf.sim.shot.ShotStatLine;
import com.progolf.sim.shot.Strategy;
import com.progolf.sim.weather.PlayingConditions;
import com.progolf.sim.weather.TournamentWeather;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The tournament engine (REQ-086–100). It composes lower-level domains — draws a field, plays four
 * rounds through the shared {@link RoundResolver}, applies the cut, resolves playoffs, and completes to
 * a single winner — owning only competition state. It resolves shots for no one; it coordinates.
 *
 * <p>Every competitor is resolved through the same path regardless of control type, and all randomness
 * flows through the {@link SeedCoordinate} (tournament→round→golfer→hole→shot), so an event is
 * reproducible bit-for-bit from its seed.
 */
public final class Tournament {

    private final TournamentDefinition definition;
    private final TournamentWeather weather;
    private final CourseSetup setup;
    private final List<TournamentEntry> entries = new ArrayList<>();
    private final List<CompetitorStanding> standings = new ArrayList<>();

    private TournamentState state = TournamentState.SCHEDULED;
    private CutResult cutResult;
    private ProfessionalGolfer winner;
    private TournamentResult result;

    // Interactive competitor (spec: playable-event): at most one competitor may be played interactively,
    // its per-round score supplied externally instead of computed by the shared resolver. Null = fully
    // automatic (unchanged behaviour). The interactive playoff state is populated only during sudden death.
    private Integer interactiveFieldIndex;
    private final Map<Integer, Integer> interactiveRoundScores = new HashMap<>();
    private List<TournamentEntry> playoffRemaining;
    private int playoffHoleCounter;

    public Tournament(TournamentDefinition definition) {
        this(definition, TournamentWeather.calm());
    }

    public Tournament(TournamentDefinition definition, TournamentWeather weather) {
        this(definition, weather, CourseSetup.standard());
    }

    /**
     * Creates a Tournament played under supplied Playing Conditions (REQ-231) and course setup (spec:
     * course-setup). Every round resolves under that round's shared conditions and the event's setup; a
     * tournament created without weather defaults to calm, and without a setup to the neutral setup.
     */
    public Tournament(TournamentDefinition definition, TournamentWeather weather, CourseSetup setup) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.weather = Objects.requireNonNull(weather, "weather");
        this.setup = Objects.requireNonNull(setup, "setup");
    }

    /** The course setup this event plays under (spec: course-setup); neutral unless supplied. */
    public CourseSetup setup() {
        return setup;
    }

    public TournamentState state() {
        return state;
    }

    public TournamentDefinition definition() {
        return definition;
    }

    // --- Setup ---

    /** Opens registration (SCHEDULED -> REGISTRATION_OPEN). */
    public void openRegistration() {
        requireState(TournamentState.SCHEDULED);
        state = TournamentState.REGISTRATION_OPEN;
    }

    /** Attempts to register a golfer; returns a typed result (REQ-089). */
    public RegistrationResult register(ProfessionalGolfer golfer) {
        Objects.requireNonNull(golfer, "golfer");
        if (state != TournamentState.REGISTRATION_OPEN) {
            return RegistrationResult.failure(RegistrationResult.Reason.REGISTRATION_CLOSED);
        }
        if (definition.entryRequirements().requiresActiveStatus()
                && golfer.player().status() != com.progolf.sim.player.CareerStatus.ACTIVE) {
            return RegistrationResult.failure(RegistrationResult.Reason.NOT_ELIGIBLE_INACTIVE);
        }
        for (TournamentEntry e : entries) {
            if (e.playerId().equals(golfer.player().id())) {
                return RegistrationResult.failure(RegistrationResult.Reason.DUPLICATE_ENTRY);
            }
        }
        if (entries.size() >= definition.entryRequirements().maxFieldSize()) {
            return RegistrationResult.failure(RegistrationResult.Reason.FIELD_FULL);
        }
        TournamentEntry entry = new TournamentEntry(golfer, entries.size());
        entries.add(entry);
        return RegistrationResult.success(entry);
    }

    /** Confirms the field (REGISTRATION_OPEN -> FIELD_CONFIRMED), fixing entries and field indices. */
    public void confirmField() {
        requireState(TournamentState.REGISTRATION_OPEN);
        if (entries.isEmpty()) {
            throw new IllegalStateException("Cannot confirm an empty field");
        }
        for (TournamentEntry e : entries) {
            standings.add(new CompetitorStanding(e.golfer(), e.fieldIndex()));
        }
        state = TournamentState.FIELD_CONFIRMED;
    }

    /** The confirmed field entries (empty until confirmed). */
    public List<TournamentEntry> field() {
        return List.copyOf(entries);
    }

    // --- Interactive competitor (spec: playable-event) ---

    /**
     * Designates exactly one competitor (by field index) as interactive: its per-round score is supplied
     * externally via {@link #submitInteractiveRoundScore} rather than computed by the shared resolver
     * (REQ: tournament-play, playable-event). Must be called after the field is confirmed and before play
     * begins; every other competitor still resolves through the unchanged shared path.
     */
    public void designateInteractiveCompetitor(int fieldIndex) {
        requireState(TournamentState.FIELD_CONFIRMED);
        if (fieldIndex < 0 || fieldIndex >= standings.size()) {
            throw new IllegalArgumentException("No competitor at field index " + fieldIndex);
        }
        this.interactiveFieldIndex = fieldIndex;
    }

    /** Whether an interactive competitor has been designated. */
    public boolean hasInteractiveCompetitor() {
        return interactiveFieldIndex != null;
    }

    /** Whether the designated interactive competitor made the cut (valid once the cut has been evaluated). */
    public boolean interactiveCompetitorMadeCut() {
        if (interactiveFieldIndex == null) {
            throw new IllegalStateException("No interactive competitor has been designated");
        }
        return standings.get(interactiveFieldIndex).hasMadeCut();
    }

    /**
     * Supplies the interactive competitor's score (relative to par) for a round. Must be submitted before
     * the tournament plays that round; the value is treated identically to a computed score.
     */
    public void submitInteractiveRoundScore(int roundNo, int scoreVsPar) {
        if (interactiveFieldIndex == null) {
            throw new IllegalStateException("No interactive competitor has been designated");
        }
        interactiveRoundScores.put(roundNo, scoreVsPar);
    }

    // --- Lifecycle ---

    /**
     * Advances the tournament by exactly one lifecycle stage (REQ-087). States are never skipped; the
     * PLAYOFF stage is entered only when the final round ends in a tie for the lead.
     */
    public void advance() {
        switch (state) {
            case FIELD_CONFIRMED -> {
                playRound(1);
                state = TournamentState.ROUND_1;
            }
            case ROUND_1 -> {
                playRound(2);
                state = TournamentState.ROUND_2;
            }
            case ROUND_2 -> {
                evaluateCut();
                state = TournamentState.CUT;
            }
            case CUT -> {
                playRound(3);
                state = TournamentState.ROUND_3;
            }
            case ROUND_3 -> {
                playRound(4);
                state = TournamentState.ROUND_4;
            }
            case ROUND_4 -> {
                if (leadIsTied()) {
                    state = TournamentState.PLAYOFF;
                } else {
                    complete();
                }
            }
            case PLAYOFF -> {
                winner = resolvePlayoff();
                complete();
            }
            default -> throw new IllegalStateException("Cannot advance from state " + state);
        }
    }

    /** Convenience: advance repeatedly until the tournament completes. */
    public TournamentResult playToCompletion() {
        while (state != TournamentState.COMPLETED) {
            advance();
        }
        return result;
    }

    // --- Play ---

    private void playRound(int roundNo) {
        // The leader's pre-round score fixes each competitor's "strokes behind" for situational pressure
        // (spec: shot-resolution pressure); computed once so it is not polluted as competitors post scores.
        int leaderBeforeRound = leaderCumulative(roundNo);
        int secondBeforeRound = secondCumulative(roundNo);
        for (CompetitorStanding s : standings) {
            if (s.isWithdrawn()) {
                continue;
            }
            if (roundNo > TournamentConstants.CUT_AFTER_ROUND && !s.hasMadeCut()) {
                continue;
            }
            int strokesBehind = s.cumulative() - leaderBeforeRound;
            double pressure = PressureModel.forRound(roundNo, definition.prestige(), strokesBehind);
            Strategy strategy = ScoreboardStrategy.adjust(dispositionOf(s), roundNo, strokesBehind,
                    strokesBehind == 0 ? secondBeforeRound - leaderBeforeRound : 0);
            s.addRoundScore(playCompetitorRound(s, roundNo, pressure, strategy));
        }
    }

    /** The second-best (2nd lowest) cumulative among the competitors eligible for {@code roundNo}, pre-round. */
    private int secondCumulative(int roundNo) {
        boolean pastCut = roundNo > TournamentConstants.CUT_AFTER_ROUND;
        int best = Integer.MAX_VALUE;
        int second = Integer.MAX_VALUE;
        for (CompetitorStanding s : standings) {
            if (s.isWithdrawn() || (pastCut && !s.hasMadeCut())) {
                continue;
            }
            int c = s.cumulative();
            if (c < best) {
                second = best;
                best = c;
            } else if (c < second) {
                second = c;
            }
        }
        return second == Integer.MAX_VALUE ? best : second;
    }

    /** A competitor's innate strategic disposition (its policy's default), or Balanced for a human. */
    private static Strategy dispositionOf(CompetitorStanding s) {
        return s.golfer().policy().map(DecisionPolicy::defaultStrategy).orElse(Strategy.BALANCED);
    }

    /** The best (lowest) cumulative score among the competitors eligible to play {@code roundNo}, pre-round. */
    private int leaderCumulative(int roundNo) {
        boolean pastCut = roundNo > TournamentConstants.CUT_AFTER_ROUND;
        int best = Integer.MAX_VALUE;
        for (CompetitorStanding s : standings) {
            if (s.isWithdrawn() || (pastCut && !s.hasMadeCut())) {
                continue;
            }
            best = Math.min(best, s.cumulative());
        }
        return best == Integer.MAX_VALUE ? 0 : best;
    }

    /**
     * Situational pressure for the competitor at {@code fieldIndex} about to play {@code roundNo}, from the
     * pre-round standings (spec: shot-resolution pressure). Exposed so the interactive player's round feels
     * the identical pressure the automatic path would compute for the same golfer (playable-event fidelity).
     */
    public double pressureFor(int fieldIndex, int roundNo) {
        return PressureModel.forRound(roundNo, definition.prestige(),
                standings.get(fieldIndex).cumulative() - leaderCumulative(roundNo));
    }

    /** Peak (sudden-death) pressure — all playoff contenders tied on the final round (spec: pressure). */
    public double playoffPressure() {
        return PressureModel.forRound(TournamentConstants.ROUNDS, definition.prestige(), 0);
    }

    /**
     * The scoreboard-adjusted strategy for the competitor at {@code fieldIndex} about to play {@code roundNo},
     * from the pre-round standings (spec: tournament-play). Exposed so the interactive player's round bends
     * to the scoreboard identically to the automatic path (playable-event fidelity).
     */
    public Strategy roundStrategyFor(int fieldIndex, int roundNo) {
        CompetitorStanding s = standings.get(fieldIndex);
        int leader = leaderCumulative(roundNo);
        int strokesBehind = s.cumulative() - leader;
        int margin = strokesBehind == 0 ? secondCumulative(roundNo) - leader : 0;
        return ScoreboardStrategy.adjust(dispositionOf(s), roundNo, strokesBehind, margin);
    }

    /** Resolves one competitor's 18-hole round through the shared engine; returns strokes relative to par. */
    private int playCompetitorRound(CompetitorStanding s, int roundNo, double pressure, Strategy strategy) {
        // The interactive competitor's round score is supplied externally, not computed (spec: playable-event).
        if (interactiveFieldIndex != null && s.fieldIndex() == interactiveFieldIndex) {
            Integer submitted = interactiveRoundScores.get(roundNo);
            if (submitted == null) {
                throw new IllegalStateException("No interactive score submitted for round " + roundNo);
            }
            return submitted;
        }
        ProfessionalGolfer g = s.golfer();
        PlayingConditions conditions = weather.conditionsForRound(roundNo);
        // The event's course setup scales pins/width (via holeModel) and wind (via exposure) — spec: course-setup.
        double exposure = definition.course().identity().classification().exposure() * setup.windScale();
        int strokes = 0;
        for (int hole = 1; hole <= 18; hole++) {
            HoleModel model = definition.course().holeModel(hole, roundNo, setup);
            SeedCoordinate coord = new SeedCoordinate(
                    definition.worldSeed(), definition.seasonId(), definition.tournamentId(),
                    roundNo, s.fieldIndex(), hole, 0);
            RoundOutcome out = RoundResolver.resolveHole(
                    model, g.player().attributes(), g.player().toGolferState(pressure),
                    conditions.environmentForHole(hole, exposure), strategy, coord);
            strokes += out.totalStrokes();
            // Accumulate shot-level stats from the resolved shots (spec: competitive-statistics).
            s.addHole(HoleStats.of(out.shots(), definition.course().holes().get(hole - 1).par()));
        }
        return strokes - definition.course().totalPar();
    }

    /**
     * Submits a whole round's shot statistics for the interactive competitor (spec: competitive-statistics),
     * so the human player's own driving/GIR/putts are captured alongside their externally-supplied score.
     */
    public void addInteractiveRoundStats(ShotStatLine roundStats) {
        if (interactiveFieldIndex == null) {
            throw new IllegalStateException("No interactive competitor has been designated");
        }
        standings.get(interactiveFieldIndex).addRoundStats(roundStats);
    }

    private void evaluateCut() {
        List<CompetitorStanding> active = activeStandings();
        if (!definition.format().hasCut()) {
            active.forEach(s -> s.setMadeCut(true));
            cutResult = new CutResult(false, Integer.MAX_VALUE, active.size());
            return;
        }
        active.sort(Comparator.comparingInt(CompetitorStanding::cumulative));
        int cutSize = definition.format().cutSize();
        int cutLine = active.size() <= cutSize
                ? active.get(active.size() - 1).cumulative()
                : active.get(cutSize - 1).cumulative();
        int made = 0;
        for (CompetitorStanding s : active) {
            boolean makes = s.cumulative() <= cutLine;
            s.setMadeCut(makes);
            if (makes) {
                made++;
            }
        }
        cutResult = new CutResult(true, cutLine, made);
    }

    // --- Leaderboard ---

    /** The live leaderboard over active competitors, ranked by cumulative score with shared tie positions. */
    public List<LeaderboardEntry> leaderboard() {
        List<CompetitorStanding> ranked = activeStandings();
        ranked.sort(Comparator.comparingInt(CompetitorStanding::cumulative)
                .thenComparingInt(CompetitorStanding::fieldIndex));
        List<LeaderboardEntry> board = new ArrayList<>(ranked.size());
        for (CompetitorStanding s : ranked) {
            int position = 1 + (int) ranked.stream().filter(o -> o.cumulative() < s.cumulative()).count();
            board.add(new LeaderboardEntry(position, s.golfer(), s.cumulative(), s.roundsPlayed()));
        }
        return board;
    }

    // --- Playoff & completion ---

    private boolean leadIsTied() {
        return contenders().size() > 1;
    }

    /** Made-cut, non-withdrawn competitors tied at the best (lowest) cumulative score. */
    private List<CompetitorStanding> contenders() {
        List<CompetitorStanding> active = new ArrayList<>();
        for (CompetitorStanding s : standings) {
            if (!s.isWithdrawn() && s.hasMadeCut()) {
                active.add(s);
            }
        }
        if (active.isEmpty()) {
            return active;
        }
        int best = active.stream().mapToInt(CompetitorStanding::cumulative).min().orElseThrow();
        List<CompetitorStanding> tied = new ArrayList<>();
        for (CompetitorStanding s : active) {
            if (s.cumulative() == best) {
                tied.add(s);
            }
        }
        return tied;
    }

    private ProfessionalGolfer resolvePlayoff() {
        List<TournamentEntry> tied = new ArrayList<>();
        for (CompetitorStanding s : contenders()) {
            tied.add(new TournamentEntry(s.golfer(), s.fieldIndex()));
        }
        return suddenDeath(definition, weather, setup, tied);
    }

    /** Calm-conditions, neutral-setup sudden death (backward-compatible entry used by standalone tests). */
    static ProfessionalGolfer suddenDeath(TournamentDefinition definition, List<TournamentEntry> tied) {
        return suddenDeath(definition, TournamentWeather.calm(), CourseSetup.standard(), tied);
    }

    /**
     * Deterministic sudden-death resolution among tied competitors (REQ-095). Package-private and static
     * so it is independently testable: given two or more tied golfers, it always returns exactly one
     * winner, reproducibly. Playoff holes are played under the tournament's final-round conditions and setup.
     */
    static ProfessionalGolfer suddenDeath(TournamentDefinition definition, TournamentWeather weather,
                                          CourseSetup setup, List<TournamentEntry> tied) {
        if (tied.isEmpty()) {
            throw new IllegalArgumentException("Playoff requires at least one competitor");
        }
        List<TournamentEntry> remaining = new ArrayList<>(tied);
        for (int ph = 1; ph <= TournamentConstants.MAX_PLAYOFF_HOLES; ph++) {
            remaining = playSuddenDeathHole(definition, weather, setup, remaining, ph, null, null);
            if (remaining.size() == 1) {
                return remaining.get(0).golfer();
            }
        }
        // Deterministic fallback if still tied after the guard limit.
        return remaining.stream()
                .min(Comparator.comparingInt(TournamentEntry::fieldIndex))
                .orElseThrow()
                .golfer();
    }

    /**
     * Plays one sudden-death hole for the remaining tied competitors, returning the survivors (those tied
     * for the low score on the hole). The interactive competitor's strokes are supplied externally when it
     * is still in the playoff; every other competitor is auto-resolved. Shared verbatim by the automatic
     * {@link #suddenDeath} loop and the interactive {@link #advancePlayoffHole} driver, so an interactive
     * playoff simmed in full reproduces the automatic winner by construction (spec: playable-event).
     */
    private static List<TournamentEntry> playSuddenDeathHole(
            TournamentDefinition definition, TournamentWeather weather, CourseSetup setup,
            List<TournamentEntry> remaining, int ph, Integer interactiveFieldIndex, Integer interactiveStrokes) {
        int holeNumber = ((ph - 1) % 18) + 1;
        int playoffRound = 90 + ph; // distinct from rounds 1-4
        int best = Integer.MAX_VALUE;
        List<TournamentEntry> survivors = new ArrayList<>();
        for (TournamentEntry e : remaining) {
            int strokes = (interactiveFieldIndex != null && e.fieldIndex() == interactiveFieldIndex
                    && interactiveStrokes != null)
                    ? interactiveStrokes
                    : playPlayoffHole(definition, weather, setup, e, holeNumber, playoffRound);
            if (strokes < best) {
                best = strokes;
                survivors.clear();
                survivors.add(e);
            } else if (strokes == best) {
                survivors.add(e);
            }
        }
        return survivors;
    }

    // --- Interactive playoff (spec: playable-event) ---

    /** Whether the tournament is currently in a sudden-death playoff awaiting resolution. */
    public boolean isPlayoff() {
        return state == TournamentState.PLAYOFF;
    }

    /**
     * Begins an interactive sudden-death playoff: captures the competitors tied for the lead as the
     * playoff field so it can be driven hole by hole via {@link #advancePlayoffHole}. Only valid once the
     * final round has left the lead tied (state PLAYOFF).
     */
    public void beginInteractivePlayoff() {
        requireState(TournamentState.PLAYOFF);
        playoffRemaining = new ArrayList<>();
        for (CompetitorStanding s : contenders()) {
            playoffRemaining.add(new TournamentEntry(s.golfer(), s.fieldIndex()));
        }
        playoffHoleCounter = 1;
    }

    /** The competitors still alive in the interactive playoff (empty until {@link #beginInteractivePlayoff}). */
    public List<ProfessionalGolfer> playoffContenders() {
        if (playoffRemaining == null) {
            return List.of();
        }
        return playoffRemaining.stream().map(TournamentEntry::golfer).toList();
    }

    /** The hole number (1-18) of the current interactive playoff hole. */
    public int playoffHoleNumber() {
        return ((playoffHoleCounter - 1) % 18) + 1;
    }

    /** The round identifier of the current interactive playoff hole (distinct from rounds 1-4). */
    public long playoffRound() {
        return 90L + playoffHoleCounter;
    }

    /**
     * Plays one interactive sudden-death hole and advances the playoff. The interactive competitor's
     * strokes are supplied (null when it has been eliminated or is not in the playoff, in which case every
     * remaining competitor is auto-resolved). When a single competitor is left — or the guard limit is
     * reached — the winner is set and the tournament completes.
     */
    public void advancePlayoffHole(Integer interactivePlayerStrokes) {
        requireState(TournamentState.PLAYOFF);
        if (playoffRemaining == null) {
            throw new IllegalStateException("beginInteractivePlayoff has not been called");
        }
        playoffRemaining = playSuddenDeathHole(definition, weather, setup, playoffRemaining, playoffHoleCounter,
                interactiveFieldIndex, interactivePlayerStrokes);
        if (playoffRemaining.size() == 1) {
            winner = playoffRemaining.get(0).golfer();
            complete();
            return;
        }
        if (playoffHoleCounter >= TournamentConstants.MAX_PLAYOFF_HOLES) {
            winner = playoffRemaining.stream()
                    .min(Comparator.comparingInt(TournamentEntry::fieldIndex))
                    .orElseThrow()
                    .golfer();
            complete();
            return;
        }
        playoffHoleCounter++;
    }

    private static int playPlayoffHole(TournamentDefinition definition, TournamentWeather weather,
                                       CourseSetup setup, TournamentEntry e, int holeNumber, int playoffRound) {
        ProfessionalGolfer g = e.golfer();
        Strategy strategy = g.policy().map(DecisionPolicy::defaultStrategy).orElse(Strategy.BALANCED);
        HoleModel model = definition.course().holeModel(holeNumber, playoffRound, setup);
        SeedCoordinate coord = new SeedCoordinate(
                definition.worldSeed(), definition.seasonId(), definition.tournamentId(),
                playoffRound, e.fieldIndex(), holeNumber, 0);
        // Playoff holes are played under the final round's shared conditions (conditionsForRound clamps).
        PlayingConditions conditions = weather.conditionsForRound(playoffRound);
        // The event's course setup scales pins/width (holeModel) and wind (exposure) — spec: course-setup.
        double exposure = definition.course().identity().classification().exposure() * setup.windScale();
        // A sudden-death playoff is peak pressure: all contenders tied on the final round (spec: pressure).
        double pressure = PressureModel.forRound(TournamentConstants.ROUNDS, definition.prestige(), 0);
        RoundOutcome out = RoundResolver.resolveHole(
                model, g.player().attributes(), g.player().toGolferState(pressure),
                conditions.environmentForHole(holeNumber, exposure), strategy, coord);
        return out.totalStrokes();
    }

    private void complete() {
        if (winner == null) {
            winner = contenders().get(0).golfer(); // single outright leader
        }
        CompetitorStanding winnerStanding = standingFor(winner);

        List<TournamentResult.Finish> finishes = new ArrayList<>();
        for (CompetitorStanding s : standings) {
            int position = positionOf(s, winnerStanding);
            double prize = definition.prizeStructure().amountForPosition(position);
            finishes.add(new TournamentResult.Finish(
                    s.golfer(), position, s.cumulative(), s.hasMadeCut(), s.isWithdrawn(), prize, s.shotStats()));
        }
        finishes.sort(Comparator.comparingInt(TournamentResult.Finish::position)
                .thenComparingInt(f -> indexOf(f.golfer())));

        result = new TournamentResult(definition.name(), definition.tournamentId(), finishes, winner, cutResult);
        state = TournamentState.COMPLETED;
    }

    /** 1-based finishing position: 1 + number of competitors strictly ahead of {@code s}. */
    private int positionOf(CompetitorStanding s, CompetitorStanding winnerStanding) {
        int ahead = 0;
        for (CompetitorStanding o : standings) {
            if (o != s && strictlyAhead(o, s, winnerStanding)) {
                ahead++;
            }
        }
        return 1 + ahead;
    }

    /** Ranking order: made-cut before missed-cut before withdrawn; then by score; the winner breaks a tie for the lead. */
    private boolean strictlyAhead(CompetitorStanding a, CompetitorStanding c, CompetitorStanding winnerStanding) {
        int ta = rankTier(a);
        int tc = rankTier(c);
        if (ta != tc) {
            return ta < tc;
        }
        if (a.cumulative() != c.cumulative()) {
            return a.cumulative() < c.cumulative();
        }
        return a == winnerStanding && c != winnerStanding;
    }

    private static int rankTier(CompetitorStanding s) {
        if (s.isWithdrawn()) {
            return 2;
        }
        return s.hasMadeCut() ? 0 : 1;
    }

    // --- Withdrawal ---

    /** Withdraws a competitor (REQ-097). Recorded, removed from the active field; the event continues. */
    public void withdraw(ProfessionalGolfer golfer) {
        Objects.requireNonNull(golfer, "golfer");
        if (state == TournamentState.COMPLETED) {
            throw new IllegalStateException("Cannot withdraw from a completed tournament");
        }
        standingFor(golfer).withdraw();
    }

    // --- Result ---

    /** The completed result (REQ-096/099). Available only once COMPLETED. */
    public TournamentResult result() {
        if (result == null) {
            throw new IllegalStateException("Tournament is not complete");
        }
        return result;
    }

    // --- Helpers ---

    private List<CompetitorStanding> activeStandings() {
        List<CompetitorStanding> active = new ArrayList<>();
        for (CompetitorStanding s : standings) {
            if (!s.isWithdrawn()) {
                active.add(s);
            }
        }
        return active;
    }

    private CompetitorStanding standingFor(ProfessionalGolfer golfer) {
        for (CompetitorStanding s : standings) {
            if (s.golfer().player().id().equals(golfer.player().id())) {
                return s;
            }
        }
        throw new IllegalArgumentException("Golfer is not in this tournament: " + golfer.player().id());
    }

    private int indexOf(ProfessionalGolfer golfer) {
        return standingFor(golfer).fieldIndex();
    }

    private void requireState(TournamentState expected) {
        if (state != expected) {
            throw new IllegalStateException("Expected state " + expected + " but was " + state);
        }
    }
}
