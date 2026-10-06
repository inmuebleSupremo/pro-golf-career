package com.progolf.sim.play;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.shot.GolferState;
import com.progolf.sim.shot.BallState;
import com.progolf.sim.shot.HoleStats;
import com.progolf.sim.shot.HoleModel;
import com.progolf.sim.shot.ShotContext;
import com.progolf.sim.shot.ShotDecision;
import com.progolf.sim.shot.ShotFrame;
import com.progolf.sim.shot.ShotOutcome;
import com.progolf.sim.shot.ShotResolver;
import com.progolf.sim.shot.ShotStatLine;
import com.progolf.sim.shot.SimConstants;
import com.progolf.sim.shot.Strategy;
import com.progolf.sim.shot.StrategyPolicy;
import com.progolf.sim.spatial.Surface;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An interactive, stateful 18-hole round a human plays shot-by-shot through the shot engine (spec:
 * playable-round). Each shot the player sees the {@link #situation()} and submits a {@link ShotDecision}
 * (club / target / risk) to {@link #playShot}; the round is always skippable via {@link #simShot} /
 * {@link #simHole} / {@link #simRound}, which use the same {@link StrategyPolicy} the AI does.
 *
 * <p>The per-shot loop mirrors {@code RoundResolver.resolveHole} exactly — the same seed coordinates,
 * penalty-hazard recovery (water-drop vs out-of-bounds stroke-and-distance), holed threshold, and shot
 * cap — so a fully-simmed round is identical to the
 * automatic round resolution for the same inputs and seed, and playing by hand differs only by the human's
 * decisions. Framework-free and deterministic given the decisions and seed.
 */
public final class PlayableRound {

    private static final int HOLES = 18;

    private final Attributes attributes;
    private final GolferState state;
    private final List<HoleToPlay> holes;
    private final SeedCoordinate base;
    private final StrategyPolicy simPolicy;

    private int holeIndex;        // 0-based; == HOLES when the round is complete
    private int shotNumber = 1;   // 1-based within the current hole
    private double remaining;     // distance to the pin for the current shot
    private int strokesThisHole;
    private int totalStrokes;
    private Surface lie = Surface.TEE_BOX;
    private BallState ball;
    private final List<Integer> holeScores = new ArrayList<>();
    private final List<ShotOutcome> currentHoleShots = new ArrayList<>();
    private final List<PlayedHole> playedHoles = new ArrayList<>();
    private ShotStatLine shotStats = ShotStatLine.empty();

    public PlayableRound(Attributes attributes, GolferState state, List<HoleToPlay> holes,
                         SeedCoordinate base, Strategy simStrategy) {
        this.attributes = Objects.requireNonNull(attributes, "attributes");
        this.state = Objects.requireNonNull(state, "state");
        Objects.requireNonNull(holes, "holes");
        if (holes.size() != HOLES) {
            throw new IllegalArgumentException("a round is " + HOLES + " holes: " + holes.size());
        }
        this.holes = List.copyOf(holes);
        this.base = Objects.requireNonNull(base, "base");
        this.simPolicy = new StrategyPolicy(Objects.requireNonNull(simStrategy, "simStrategy"));
        this.remaining = this.holes.get(0).model().startDistance();
        this.ball = initialBall(this.holes.get(0));
    }

    /** Whether all eighteen holes have been played. */
    public boolean isComplete() {
        return holeIndex >= HOLES;
    }

    /** The current hole number (1-based). */
    public int currentHole() {
        return holeIndex + 1;
    }

    /** The exact setup-specific model currently used to resolve the active hole. */
    public HoleModel currentHoleModel() {
        requireNotComplete();
        return holes.get(holeIndex).model();
    }

    /** The situation for the current shot. */
    public ShotSituation situation() {
        requireNotComplete();
        HoleToPlay hole = holes.get(holeIndex);
        return new ShotSituation(holeIndex + 1, hole.par(), shotNumber, strokesThisHole,
                remaining, lie, hole.model().pinLateral(), hole.model().zoneProfileFor(remaining));
    }

    /** Plays the current shot with the human's decision (club / target / risk). */
    public ShotOutcome playShot(ShotDecision decision) {
        requireNotComplete();
        return resolveOne(Objects.requireNonNull(decision, "decision"));
    }

    /** Sims the current shot with the automatic policy. */
    public ShotOutcome simShot() {
        requireNotComplete();
        return resolveOne(simDecision());
    }

    /** Sims the rest of the current hole with the automatic policy. */
    public void simHole() {
        requireNotComplete();
        int hole = holeIndex;
        while (!isComplete() && holeIndex == hole) {
            resolveOne(simDecision());
        }
    }

    /** Sims the rest of the round with the automatic policy. */
    public void simRound() {
        while (!isComplete()) {
            resolveOne(simDecision());
        }
    }

    /** The automatic policy's decision for the current situation (lie + pin + attributes), matching the AI path. */
    private ShotDecision simDecision() {
        HoleToPlay hole = holes.get(holeIndex);
        if (ball != null && hole.model().geometry() != null) {
            return simPolicy.decide(remaining, lie, localPinLateral(hole), attributes, hole.par());
        }
        return simPolicy.decide(remaining, lie, hole.model().pinLateral(), attributes, hole.par());
    }

    private ShotOutcome resolveOne(ShotDecision decision) {
        HoleToPlay hole = holes.get(holeIndex);
        double preShotRemaining = remaining;
        SeedCoordinate coord = base.withHole(holeIndex + 1).withShot(shotNumber);
        ShotContext context = contextFor(hole, decision, coord);
        ShotOutcome outcome = ShotResolver.resolveShot(context);

        totalStrokes += outcome.strokes();
        strokesThisHole += outcome.strokes();
        currentHoleShots.add(outcome);
        lie = outcome.finalSurface();

        boolean holed;
        if (outcome.settlement() != null) {
            ball = outcome.settlement().ball();
            lie = ball.lie();
            remaining = ball.position().distanceTo(hole.model().cupPosition());
            holed = !outcome.hazardEntered() && remaining <= SimConstants.HOLED_THRESHOLD;
        } else if (outcome.hazardEntered()) {
            // Penalty-hazard recovery, identical to RoundResolver so simmed == auto (spec: shot-resolution).
            if (outcome.finalSurface() == Surface.WATER) {
                remaining = Math.min(preShotRemaining, outcome.distanceRemaining() + SimConstants.WATER_DROP_SETBACK);
                lie = Surface.PRIMARY_ROUGH; // dropped in rough near the hazard
            } else {
                remaining = preShotRemaining; // out of bounds: stroke-and-distance
            }
            holed = false;
        } else {
            remaining = outcome.distanceRemaining();
            holed = remaining <= SimConstants.HOLED_THRESHOLD;
        }
        shotNumber++;
        if (holed || shotNumber > SimConstants.MAX_SHOTS_PER_HOLE) {
            completeHole();
        }
        return outcome;
    }

    private void completeHole() {
        holeScores.add(strokesThisHole);
        int par = holes.get(holeIndex).par();
        shotStats = shotStats.plus(HoleStats.of(currentHoleShots, par));
        playedHoles.add(new PlayedHole(holeIndex + 1, par, currentHoleShots));
        currentHoleShots.clear();
        holeIndex++;
        if (!isComplete()) {
            remaining = holes.get(holeIndex).model().startDistance();
            shotNumber = 1;
            strokesThisHole = 0;
            lie = Surface.TEE_BOX;
            ball = initialBall(holes.get(holeIndex));
        }
    }

    /** The round's accumulated shot-level statistics (spec: competitive-statistics). */
    public ShotStatLine shotStats() {
        return shotStats;
    }

    /** Total strokes taken so far (the round total once complete). */
    public int totalStrokes() {
        return totalStrokes;
    }

    /** The score on each completed hole, in order. */
    public List<Integer> holeScores() {
        return List.copyOf(holeScores);
    }

    /** The completed holes with their full shot-by-shot detail, in order (spec: career-achievements). */
    public List<PlayedHole> playedHoles() {
        return List.copyOf(playedHoles);
    }

    /** The completed holes of this round as (hole number, par, strokes), in order. */
    public List<RoundScorecard.HoleScore> completedHoles() {
        List<RoundScorecard.HoleScore> result = new ArrayList<>(holeScores.size());
        for (int i = 0; i < holeScores.size(); i++) {
            result.add(new RoundScorecard.HoleScore(i + 1, holes.get(i).par(), holeScores.get(i)));
        }
        return result;
    }

    /** Strokes relative to par over the completed holes (the round's score vs par once complete). */
    public int scoreVsPar() {
        int strokes = 0;
        int par = 0;
        for (int i = 0; i < holeScores.size(); i++) {
            strokes += holeScores.get(i);
            par += holes.get(i).par();
        }
        return strokes - par;
    }

    private void requireNotComplete() {
        if (isComplete()) {
            throw new IllegalStateException("the round is complete");
        }
    }

    /** Current authoritative state for API projection; null only for a legacy fixture-backed round. */
    public BallState ballState() {
        return ball;
    }

    private ShotContext contextFor(HoleToPlay hole, ShotDecision decision, SeedCoordinate coord) {
        if (ball == null || hole.model().geometry() == null) {
            return new ShotContext(attributes, state, hole.environment(), remaining, hole.model().zoneProfileFor(remaining),
                    decision, coord, lie, hole.model().pinLateral());
        }
        return new ShotContext(attributes, state, hole.environment(), remaining, hole.model().zoneProfileFor(remaining),
                decision, coord, lie, localPinLateral(hole), ball, hole.model().geometry(), hole.model().cupPosition());
    }

    private double localPinLateral(HoleToPlay hole) {
        ShotFrame frame = ShotFrame.towardGreenCentreReference(ball.position(), hole.model().geometry().greenCenter(),
                hole.model().cupPosition());
        return frame.lateralTo(hole.model().cupPosition());
    }

    private static BallState initialBall(HoleToPlay hole) {
        return hole.model().geometry() == null ? null : new BallState(hole.model().geometry().tee(), Surface.TEE_BOX);
    }
}
