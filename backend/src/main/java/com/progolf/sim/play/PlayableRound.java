package com.progolf.sim.play;

import com.progolf.sim.core.Attributes;
import com.progolf.sim.core.SeedCoordinate;
import com.progolf.sim.shot.GolferState;
import com.progolf.sim.shot.ShotContext;
import com.progolf.sim.shot.ShotDecision;
import com.progolf.sim.shot.ShotOutcome;
import com.progolf.sim.shot.ShotResolver;
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
 * stroke-and-distance rule, holed threshold, and shot cap — so a fully-simmed round is identical to the
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
    private final List<Integer> holeScores = new ArrayList<>();

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
    }

    /** Whether all eighteen holes have been played. */
    public boolean isComplete() {
        return holeIndex >= HOLES;
    }

    /** The current hole number (1-based). */
    public int currentHole() {
        return holeIndex + 1;
    }

    /** The situation for the current shot. */
    public ShotSituation situation() {
        requireNotComplete();
        HoleToPlay hole = holes.get(holeIndex);
        return new ShotSituation(holeIndex + 1, hole.par(), shotNumber, strokesThisHole,
                remaining, lie, hole.model().zoneProfileFor(remaining));
    }

    /** Plays the current shot with the human's decision (club / target / risk). */
    public ShotOutcome playShot(ShotDecision decision) {
        requireNotComplete();
        return resolveOne(Objects.requireNonNull(decision, "decision"));
    }

    /** Sims the current shot with the automatic policy. */
    public ShotOutcome simShot() {
        requireNotComplete();
        return resolveOne(simPolicy.decide(remaining));
    }

    /** Sims the rest of the current hole with the automatic policy. */
    public void simHole() {
        requireNotComplete();
        int hole = holeIndex;
        while (!isComplete() && holeIndex == hole) {
            resolveOne(simPolicy.decide(remaining));
        }
    }

    /** Sims the rest of the round with the automatic policy. */
    public void simRound() {
        while (!isComplete()) {
            resolveOne(simPolicy.decide(remaining));
        }
    }

    private ShotOutcome resolveOne(ShotDecision decision) {
        HoleToPlay hole = holes.get(holeIndex);
        double preShotRemaining = remaining;
        SeedCoordinate coord = base.withHole(holeIndex + 1).withShot(shotNumber);
        ShotContext context = new ShotContext(attributes, state, hole.environment(), remaining,
                hole.model().zoneProfileFor(remaining), decision, coord);
        ShotOutcome outcome = ShotResolver.resolveShot(context);

        totalStrokes += outcome.strokes();
        strokesThisHole += outcome.strokes();
        lie = outcome.finalSurface();

        boolean holed;
        if (outcome.hazardEntered()) {
            remaining = preShotRemaining; // stroke-and-distance
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
        holeIndex++;
        if (!isComplete()) {
            remaining = holes.get(holeIndex).model().startDistance();
            shotNumber = 1;
            strokesThisHole = 0;
            lie = Surface.TEE_BOX;
        }
    }

    /** Total strokes taken so far (the round total once complete). */
    public int totalStrokes() {
        return totalStrokes;
    }

    /** The score on each completed hole, in order. */
    public List<Integer> holeScores() {
        return List.copyOf(holeScores);
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
}
