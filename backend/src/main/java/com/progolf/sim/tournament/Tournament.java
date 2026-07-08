package com.progolf.sim.tournament;

import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.player.DecisionPolicy;
import com.progolf.sim.player.ProfessionalGolfer;
import com.progolf.sim.shot.Environment;
import com.progolf.sim.shot.HoleModel;
import com.progolf.sim.shot.RoundOutcome;
import com.progolf.sim.shot.RoundResolver;
import com.progolf.sim.shot.Strategy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
    private final List<TournamentEntry> entries = new ArrayList<>();
    private final List<CompetitorStanding> standings = new ArrayList<>();

    private TournamentState state = TournamentState.SCHEDULED;
    private CutResult cutResult;
    private ProfessionalGolfer winner;
    private TournamentResult result;

    public Tournament(TournamentDefinition definition) {
        this.definition = Objects.requireNonNull(definition, "definition");
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
        for (CompetitorStanding s : standings) {
            if (s.isWithdrawn()) {
                continue;
            }
            if (roundNo > TournamentConstants.CUT_AFTER_ROUND && !s.hasMadeCut()) {
                continue;
            }
            s.addRoundScore(playCompetitorRound(s, roundNo));
        }
    }

    /** Resolves one competitor's 18-hole round through the shared engine; returns strokes relative to par. */
    private int playCompetitorRound(CompetitorStanding s, int roundNo) {
        ProfessionalGolfer g = s.golfer();
        Strategy strategy = g.policy().map(DecisionPolicy::defaultStrategy).orElse(Strategy.BALANCED);
        int strokes = 0;
        for (int hole = 1; hole <= 18; hole++) {
            HoleModel model = definition.course().holeModel(hole, roundNo);
            SeedCoordinate coord = new SeedCoordinate(
                    definition.worldSeed(), definition.seasonId(), definition.tournamentId(),
                    roundNo, s.fieldIndex(), hole, 0);
            RoundOutcome out = RoundResolver.resolveHole(
                    model, g.player().attributes(), g.player().toGolferState(0.0),
                    Environment.calm(), strategy, coord);
            strokes += out.totalStrokes();
        }
        return strokes - definition.course().totalPar();
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
        return suddenDeath(definition, tied);
    }

    /**
     * Deterministic sudden-death resolution among tied competitors (REQ-095). Package-private and static
     * so it is independently testable: given two or more tied golfers, it always returns exactly one
     * winner, reproducibly.
     */
    static ProfessionalGolfer suddenDeath(TournamentDefinition definition, List<TournamentEntry> tied) {
        if (tied.isEmpty()) {
            throw new IllegalArgumentException("Playoff requires at least one competitor");
        }
        List<TournamentEntry> remaining = new ArrayList<>(tied);
        for (int ph = 1; ph <= TournamentConstants.MAX_PLAYOFF_HOLES; ph++) {
            int holeNumber = ((ph - 1) % 18) + 1;
            int playoffRound = 90 + ph; // distinct from rounds 1-4
            int best = Integer.MAX_VALUE;
            List<TournamentEntry> survivors = new ArrayList<>();
            for (TournamentEntry e : remaining) {
                int strokes = playPlayoffHole(definition, e, holeNumber, playoffRound);
                if (strokes < best) {
                    best = strokes;
                    survivors.clear();
                    survivors.add(e);
                } else if (strokes == best) {
                    survivors.add(e);
                }
            }
            remaining = survivors;
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

    private static int playPlayoffHole(TournamentDefinition definition, TournamentEntry e, int holeNumber, int playoffRound) {
        ProfessionalGolfer g = e.golfer();
        Strategy strategy = g.policy().map(DecisionPolicy::defaultStrategy).orElse(Strategy.BALANCED);
        HoleModel model = definition.course().holeModel(holeNumber, playoffRound);
        SeedCoordinate coord = new SeedCoordinate(
                definition.worldSeed(), definition.seasonId(), definition.tournamentId(),
                playoffRound, e.fieldIndex(), holeNumber, 0);
        RoundOutcome out = RoundResolver.resolveHole(
                model, g.player().attributes(), g.player().toGolferState(0.0),
                Environment.calm(), strategy, coord);
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
                    s.golfer(), position, s.cumulative(), s.hasMadeCut(), s.isWithdrawn(), prize));
        }
        finishes.sort(Comparator.comparingInt(TournamentResult.Finish::position)
                .thenComparingInt(f -> indexOf(f.golfer())));

        result = new TournamentResult(definition.name(), finishes, winner, cutResult);
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
