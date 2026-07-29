package com.progolf.sim.play;

import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.course.Course;
import com.progolf.sim.course.CourseSetup;
import com.progolf.sim.course.EnvironmentClassification;
import com.progolf.sim.course.GeneratedHole;
import com.progolf.sim.course.PinPosition;
import com.progolf.sim.player.DecisionPolicy;
import com.progolf.sim.player.ProfessionalGolfer;
import com.progolf.sim.shot.Environment;
import com.progolf.sim.shot.HoleModel;
import com.progolf.sim.shot.ShotDecision;
import com.progolf.sim.shot.ShotOutcome;
import com.progolf.sim.shot.Strategy;
import com.progolf.sim.tournament.LeaderboardEntry;
import com.progolf.sim.tournament.Tournament;
import com.progolf.sim.tournament.TournamentResult;
import com.progolf.sim.weather.PlayingConditions;
import com.progolf.sim.weather.TournamentWeather;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The player's live tournament played interactively (spec: playable-event). It drives a real
 * {@link Tournament} for the whole field with the player designated as its one interactive competitor:
 * round by round the player plays (or sims) their golfer's round through a {@link PlayableRound} while
 * every other competitor is resolved automatically by the shared engine, and any sudden-death playoff is
 * played interactively through a {@link PlayableHole}. The cut, leaderboard, playoff, and completion are
 * all owned by the Tournament, so nothing about the competition rules is re-implemented here.
 *
 * <p>Fidelity: the player's per-round base seed and the playoff-hole seed match the automatic path exactly,
 * so a fully-simmed event ({@link #simEvent}) reproduces the automatic tournament result bit-for-bit.
 * Always skippable at shot / hole / round / event granularity.
 */
public final class PlayableEvent {

    private enum Phase { ROUND, PLAYOFF, DONE }

    private final Tournament tournament;
    private final ProfessionalGolfer player;
    private final int playerFieldIndex;
    private final Course course;
    private final TournamentWeather weather;
    private final long worldSeed;
    private final int season;
    private final long tournamentId;
    private final Strategy simStrategy;
    private final double exposure;
    private final CourseSetup setup;

    private Phase phase;
    private int currentRoundNo;
    private PlayableRound currentRound;
    private PlayableHole currentPlayoffHole;

    /**
     * Creates an interactive event over a confirmed Tournament in which the player has been designated the
     * interactive competitor at {@code playerFieldIndex}. The seed context and sim strategy must match the
     * automatic path so a simmed event is byte-identical to automatic resolution; the caller (World)
     * supplies the same values it would use to auto-resolve the event.
     */
    public PlayableEvent(Tournament tournament, ProfessionalGolfer player, int playerFieldIndex,
                         Course course, TournamentWeather weather, long worldSeed, int season, long tournamentId) {
        this.tournament = Objects.requireNonNull(tournament, "tournament");
        this.player = Objects.requireNonNull(player, "player");
        this.playerFieldIndex = playerFieldIndex;
        this.course = Objects.requireNonNull(course, "course");
        this.weather = Objects.requireNonNull(weather, "weather");
        this.worldSeed = worldSeed;
        this.season = season;
        this.tournamentId = tournamentId;
        // The exact strategy the automatic path would use for this golfer, so a simmed round matches it.
        this.simStrategy = player.policy().map(DecisionPolicy::defaultStrategy).orElse(Strategy.BALANCED);
        // The event's course setup (from the tournament) scales pins/width via holeModel and wind via exposure,
        // identically to the automatic path, so a simmed event matches the auto result (spec: course-setup).
        this.setup = tournament.setup();
        this.exposure = course.identity().classification().exposure() * setup.windScale();
        beginRound(1);
    }

    // --- Player interaction (delegates to the current interactive unit) ---

    /** The situation for the current shot (round or playoff hole). */
    public ShotSituation situation() {
        requireActive();
        return phase == Phase.PLAYOFF ? currentPlayoffHole.situation() : currentRound.situation();
    }

    // --- Presentation geometry (spec: web-hole-visualization): the hole being played, for rendering ---

    /** The generated geometry of the hole the player is currently on. */
    public GeneratedHole currentHole() {
        return holeGeometry(situation().holeNumber());
    }

    /**
     * The active pin for the hole the player is currently on, under this event's setup — the exact pin the
     * played {@link com.progolf.sim.shot.HoleModel} carries, so a rendered flag matches the resolved shot.
     */
    public PinPosition currentPin() {
        return pinAt(situation().holeNumber());
    }

    /** The generated geometry of hole {@code holeNumber} (1..18) on this event's course. */
    public GeneratedHole holeGeometry(int holeNumber) {
        return course.holes().get(holeNumber - 1);
    }

    /** The active pin for hole {@code holeNumber} in the round currently in progress, under this event's setup. */
    public PinPosition pinAt(int holeNumber) {
        return holeGeometry(holeNumber).pinFor(currentPinRound(), setup);
    }

    /** The host course's environment classification (biome), for presentation styling. */
    public EnvironmentClassification classification() {
        return course.identity().classification();
    }

    /** The round whose pin is live — the playoff round during a playoff, otherwise the round in play. */
    private int currentPinRound() {
        return phase == Phase.PLAYOFF ? (int) tournament.playoffRound() : currentRoundNo;
    }

    /** Plays the current shot with the human's decision. */
    public ShotOutcome playShot(ShotDecision decision) {
        requireActive();
        ShotOutcome outcome = phase == Phase.PLAYOFF
                ? currentPlayoffHole.playShot(decision)
                : currentRound.playShot(decision);
        syncProgress();
        return outcome;
    }

    /** Sims the current shot with the automatic policy. */
    public ShotOutcome simShot() {
        requireActive();
        ShotOutcome outcome = phase == Phase.PLAYOFF ? currentPlayoffHole.simShot() : currentRound.simShot();
        syncProgress();
        return outcome;
    }

    /** Sims the rest of the current hole. */
    public void simHole() {
        requireActive();
        if (phase == Phase.PLAYOFF) {
            currentPlayoffHole.simHole();
        } else {
            currentRound.simHole();
        }
        syncProgress();
    }

    /** Sims the rest of the current round (or, in a playoff, the current playoff hole). */
    public void simRound() {
        requireActive();
        if (phase == Phase.PLAYOFF) {
            currentPlayoffHole.simHole();
        } else {
            currentRound.simRound();
        }
        syncProgress();
    }

    /** Sims the remainder of the event — all remaining rounds and any playoff — to completion. */
    public void simEvent() {
        while (phase != Phase.DONE) {
            if (phase == Phase.PLAYOFF) {
                currentPlayoffHole.simHole();
            } else {
                currentRound.simRound();
            }
            syncProgress();
        }
    }

    // --- Status ---

    /** Whether the whole event (including any playoff) has finished. */
    public boolean isComplete() {
        return phase == Phase.DONE;
    }

    /** The round the player is currently playing (1-4); meaningful only during round play. */
    public int currentRoundNumber() {
        return currentRoundNo;
    }

    /** Whether the player is currently in a sudden-death playoff. */
    public boolean inPlayoff() {
        return phase == Phase.PLAYOFF;
    }

    /**
     * The situational pressure in [0,1] the player currently feels — the identical value the shot model
     * consumes this round (specs: shot-resolution pressure): computed from the pre-round standings for the
     * round in progress, peak during a sudden-death playoff, and zero once the event is done. Exposed so the
     * play surface can show the player WHY the closing rounds bite and that COMPOSURE resists it.
     */
    public double currentPressure() {
        return switch (phase) {
            case ROUND -> tournament.pressureFor(playerFieldIndex, currentRoundNo);
            case PLAYOFF -> tournament.playoffPressure();
            case DONE -> 0.0;
        };
    }

    /** Whether the player made the cut (valid once the second round and cut have been played). */
    public boolean playerMadeCut() {
        return tournament.interactiveCompetitorMadeCut();
    }

    /** The live field leaderboard (the player's standing among the whole field). */
    public List<LeaderboardEntry> leaderboard() {
        return tournament.leaderboard();
    }

    /**
     * The player's current-round scorecard, or {@code null} when no round is in progress (during a playoff
     * or once the event is done — the round-by-round detail only exists while a round is being played).
     */
    public RoundScorecard currentScorecard() {
        if (phase != Phase.ROUND) {
            return null;
        }
        return new RoundScorecard(currentRoundNo, currentRound.currentHole(),
                currentRound.scoreVsPar(), currentRound.totalStrokes(), currentRound.completedHoles());
    }

    /** The completed tournament result (available once the event is complete). */
    public TournamentResult result() {
        if (phase != Phase.DONE) {
            throw new IllegalStateException("the event is not complete");
        }
        return tournament.result();
    }

    // --- Orchestration ---

    /** Called after every resolved shot: when the current interactive unit finishes, advances the event. */
    private void syncProgress() {
        if (phase == Phase.ROUND && currentRound.isComplete()) {
            finishRound();
        } else if (phase == Phase.PLAYOFF && currentPlayoffHole.isComplete()) {
            finishPlayoffHole();
        }
    }

    private void beginRound(int roundNo) {
        this.currentRoundNo = roundNo;
        PlayingConditions conditions = weather.conditionsForRound(roundNo);
        List<HoleToPlay> holes = new ArrayList<>(18);
        for (int hole = 1; hole <= 18; hole++) {
            HoleModel model = course.holeModel(hole, roundNo, setup);
            int par = course.holes().get(hole - 1).par();
            Environment env = conditions.environmentForHole(hole, exposure);
            holes.add(new HoleToPlay(model, par, env));
        }
        SeedCoordinate base = new SeedCoordinate(worldSeed, season, tournamentId, roundNo, playerFieldIndex, 0, 0);
        // The player feels the same situational pressure and scoreboard-bent strategy the auto path would
        // compute for them this round (specs: shot-resolution pressure, tournament-play), read from the
        // pre-round standings — preserving simmed==auto fidelity.
        var playerState = player.player().toGolferState(tournament.pressureFor(playerFieldIndex, roundNo));
        Strategy roundStrategy = tournament.roundStrategyFor(playerFieldIndex, roundNo);
        this.currentRound = new PlayableRound(player.player().attributes(), playerState, holes, base, roundStrategy);
        this.phase = Phase.ROUND;
    }

    /** The player's round is complete: lock its score into the Tournament and advance the field. */
    private void finishRound() {
        int roundNo = currentRoundNo;
        tournament.submitInteractiveRoundScore(roundNo, currentRound.scoreVsPar());
        tournament.addInteractiveRoundStats(currentRound.shotStats()); // capture the player's shot stats too
        tournament.advance(); // plays this round for the AI field, using the submitted player score
        switch (roundNo) {
            case 1 -> beginRound(2);
            case 2 -> {
                tournament.advance(); // evaluate the cut over the combined field
                if (tournament.interactiveCompetitorMadeCut()) {
                    beginRound(3);
                } else {
                    // Player missed the cut: they do not play the final rounds; the field finishes normally.
                    tournament.advance(); // round 3 (player's standing skipped)
                    tournament.advance(); // round 4
                    finishFieldPlayOrPlayoff();
                }
            }
            case 3 -> beginRound(4);
            case 4 -> finishFieldPlayOrPlayoff();
            default -> throw new IllegalStateException("unexpected round " + roundNo);
        }
    }

    /** After the final round: either the event is done, or the player is drawn into an interactive playoff. */
    private void finishFieldPlayOrPlayoff() {
        tournament.advance(); // ROUND_4 -> PLAYOFF or COMPLETED
        if (tournament.isPlayoff()) {
            tournament.beginInteractivePlayoff();
            setupPlayoffHoleOrFinish();
        } else {
            phase = Phase.DONE;
        }
    }

    /** Sets up the next interactive playoff hole for the player, or finishes if the playoff is resolved. */
    private void setupPlayoffHoleOrFinish() {
        if (!tournament.isPlayoff()) {
            phase = Phase.DONE;
            currentPlayoffHole = null;
            return;
        }
        if (playerInPlayoff()) {
            int holeNumber = tournament.playoffHoleNumber();
            long playoffRound = tournament.playoffRound();
            HoleModel model = course.holeModel(holeNumber, (int) playoffRound, setup);
            int par = course.holes().get(holeNumber - 1).par();
            Environment env = weather.conditionsForRound((int) playoffRound).environmentForHole(holeNumber, exposure);
            SeedCoordinate coord =
                    new SeedCoordinate(worldSeed, season, tournamentId, playoffRound, playerFieldIndex, holeNumber, 0);
            // A sudden-death playoff is peak pressure, identical to the auto path's playoff resolution.
            var playoffState = player.player().toGolferState(tournament.playoffPressure());
            currentPlayoffHole = new PlayableHole(holeNumber, par, player.player().attributes(), playoffState,
                    model, env, coord, simStrategy);
            phase = Phase.PLAYOFF;
        } else {
            // Player is not (or no longer) tied for the lead: auto-resolve the rest of the playoff.
            while (tournament.isPlayoff()) {
                tournament.advancePlayoffHole(null);
            }
            phase = Phase.DONE;
            currentPlayoffHole = null;
        }
    }

    /** The player's interactive playoff hole is complete: feed its strokes and advance the playoff. */
    private void finishPlayoffHole() {
        tournament.advancePlayoffHole(currentPlayoffHole.strokes());
        setupPlayoffHoleOrFinish();
    }

    private boolean playerInPlayoff() {
        String playerId = player.player().id();
        return tournament.playoffContenders().stream()
                .anyMatch(g -> g.player().id().equals(playerId));
    }

    private void requireActive() {
        if (phase == Phase.DONE) {
            throw new IllegalStateException("the event is complete");
        }
    }
}
